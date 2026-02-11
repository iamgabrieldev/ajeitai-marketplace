package com.ajeitai.backend.service;

import com.ajeitai.backend.domain.agendamento.Agendamento;
import com.ajeitai.backend.domain.agendamento.Disponibilidade;
import com.ajeitai.backend.domain.agendamento.DadosAgendamento;
import com.ajeitai.backend.domain.agendamento.DadosLocalizacao;
import com.ajeitai.backend.domain.agendamento.FormaPagamento;
import com.ajeitai.backend.domain.agendamento.StatusAgendamento;
import com.ajeitai.backend.domain.cliente.Cliente;
import com.ajeitai.backend.domain.endereco.Endereco;
import com.ajeitai.backend.domain.prestador.Prestador;
import com.ajeitai.backend.domain.pagamento.Pagamento;
import com.ajeitai.backend.repository.AgendamentoRepository;
import com.ajeitai.backend.repository.DisponibilidadeRepository;
import com.ajeitai.backend.repository.PrestadorRepository;
import com.ajeitai.backend.service.ArmazenamentoMidiaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AgendamentoService {

    private final ClienteService clienteService;
    private final PrestadorRepository prestadorRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final DisponibilidadeRepository disponibilidadeRepository;
    private final PagamentoService pagamentoService;
    private final NotificacaoPushService notificacaoPushService;
    private final MensageriaService mensageriaService;
    private final ArmazenamentoMidiaService armazenamentoMidiaService;

    @Transactional
    public Agendamento criar(String clienteKeycloakId, DadosAgendamento dados) {
        Cliente cliente = clienteService.buscarPorKeycloakId(clienteKeycloakId);
        Prestador prestador = prestadorRepository.findById(dados.prestadorId())
                .orElseThrow(() -> new IllegalArgumentException("Prestador não encontrado."));

        if (dados.dataHora().isBefore(LocalDateTime.now().plusMinutes(30))) {
            throw new IllegalArgumentException("O agendamento deve ser criado com pelo menos 30 minutos de antecedência.");
        }

        // Validação de cidade: cliente e prestador na mesma cidade
        if (cliente.getEndereco() == null || prestador.getEndereco() == null) {
            throw new IllegalArgumentException("Cliente e prestador devem possuir endereço cadastrado.");
        }
        String cidadeCliente = cliente.getEndereco().getCidade();
        String cidadePrestador = prestador.getEndereco().getCidade();
        if (cidadeCliente == null || !cidadeCliente.equalsIgnoreCase(cidadePrestador)) {
            throw new IllegalArgumentException("O prestador não atende na cidade do cliente. Cidade do cliente: " + cidadeCliente);
        }

        // Validação de horário: dataHora deve cair em um slot de Disponibilidade do prestador
        int diaSemana = dados.dataHora().getDayOfWeek().getValue(); // 1 = Segunda, 7 = Domingo
        List<Disponibilidade> slots = disponibilidadeRepository.findByPrestadorIdAndDiaSemanaOrderByHoraInicioAsc(
                prestador.getId(), diaSemana);
        boolean dentroDeAlgumSlot = slots.stream().anyMatch(d ->
                !dados.dataHora().toLocalTime().isBefore(d.getHoraInicio()) &&
                        dados.dataHora().toLocalTime().isBefore(d.getHoraFim()));
        if (slots.isEmpty() || !dentroDeAlgumSlot) {
            throw new IllegalArgumentException("O horário escolhido não está dentro da disponibilidade do prestador.");
        }

        // Conflito: não pode haver outro agendamento ACEITO ou PENDENTE no mesmo horário para o mesmo prestador
        boolean conflito = agendamentoRepository.existsByPrestadorIdAndDataHoraBetweenAndStatusIn(
                prestador.getId(),
                dados.dataHora(),
                dados.dataHora(),
                List.of(StatusAgendamento.PENDENTE, StatusAgendamento.ACEITO, StatusAgendamento.CONFIRMADO)
        );
        if (conflito) {
            throw new IllegalArgumentException("Já existe um agendamento para este horário com o prestador.");
        }

        Endereco enderecoServico = cliente.getEndereco() != null
                ? new Endereco(cliente.getEndereco().getLogradouro(), cliente.getEndereco().getBairro(),
                cliente.getEndereco().getCep(), cliente.getEndereco().getNumero(),
                cliente.getEndereco().getComplemento(), cliente.getEndereco().getCidade(),
                cliente.getEndereco().getUf(), cliente.getEndereco().getLatitude(),
                cliente.getEndereco().getLongitude())
                : null;

        Agendamento agendamento = Agendamento.builder()
                .cliente(cliente)
                .prestador(prestador)
                .dataHora(dados.dataHora())
                .status(StatusAgendamento.PENDENTE)
                .formaPagamento(dados.formaPagamento())
                .valorServico(prestador.getValorServico())
                .observacao(dados.observacao())
                .endereco(enderecoServico)
                .build();
        Agendamento salvo = agendamentoRepository.save(agendamento);
        notificacaoPushService.notificarNovoAgendamento(salvo);
        return salvo;
    }

    public List<Agendamento> listarPorCliente(String clienteKeycloakId) {
        Cliente cliente = clienteService.buscarPorKeycloakId(clienteKeycloakId);
        return agendamentoRepository.findByClienteIdOrderByDataHoraDesc(cliente.getId());
    }

    public List<Agendamento> listarPorCliente(String clienteKeycloakId, Optional<StatusAgendamento> status) {
        Cliente cliente = clienteService.buscarPorKeycloakId(clienteKeycloakId);
        if (status.isPresent()) {
            return agendamentoRepository.findByClienteIdAndStatusOrderByDataHoraDesc(cliente.getId(), status.get());
        }
        return agendamentoRepository.findByClienteIdOrderByDataHoraDesc(cliente.getId());
    }

    public Agendamento buscarPorId(Long id) {
        return agendamentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado."));
    }

    public Agendamento buscarPorIdDoCliente(Long id, String clienteKeycloakId) {
        Agendamento agendamento = buscarPorId(id);
        validarCliente(agendamento, clienteKeycloakId);
        return agendamento;
    }

    @Transactional
    public Agendamento aceitar(Long agendamentoId, String prestadorKeycloakId) {
        Agendamento agendamento = buscarPorId(agendamentoId);
        Prestador prestador = prestadorRepository.findByKeycloakId(prestadorKeycloakId)
                .orElseThrow(() -> new IllegalArgumentException("Prestador não encontrado."));
        if (!agendamento.getPrestador().getId().equals(prestador.getId())) {
            throw new IllegalArgumentException("Somente o prestador do agendamento pode aceitá-lo.");
        }
        if (agendamento.getStatus() != StatusAgendamento.PENDENTE) {
            throw new IllegalArgumentException("Somente agendamentos pendentes podem ser aceitos.");
        }
        agendamento.aceitar();
        Pagamento pagamento = pagamentoService.criarPagamento(agendamento);
        if (pagamento.getLinkPagamento() != null) {
            mensageriaService.enviarLinkPagamento(agendamento.getCliente(), pagamento.getLinkPagamento());
        }
        if (agendamento.getFormaPagamento() == FormaPagamento.DINHEIRO) {
            agendamento.confirmar();
        }
        return agendamentoRepository.save(agendamento);
    }

    @Transactional
    public Agendamento recusar(Long agendamentoId, String prestadorKeycloakId) {
        Agendamento agendamento = buscarPorId(agendamentoId);
        Prestador prestador = prestadorRepository.findByKeycloakId(prestadorKeycloakId)
                .orElseThrow(() -> new IllegalArgumentException("Prestador não encontrado."));
        if (!agendamento.getPrestador().getId().equals(prestador.getId())) {
            throw new IllegalArgumentException("Somente o prestador do agendamento pode recusá-lo.");
        }
        if (agendamento.getStatus() != StatusAgendamento.PENDENTE) {
            throw new IllegalArgumentException("Somente agendamentos pendentes podem ser recusados.");
        }
        agendamento.recusar();
        return agendamentoRepository.save(agendamento);
    }

    @Transactional
    public Agendamento cancelar(Long agendamentoId, String clienteKeycloakId) {
        Agendamento agendamento = buscarPorId(agendamentoId);
        Cliente cliente = clienteService.buscarPorKeycloakId(clienteKeycloakId);
        if (!agendamento.getCliente().getId().equals(cliente.getId())) {
            throw new IllegalArgumentException("Somente o cliente do agendamento pode cancelar.");
        }
        if (agendamento.getStatus() == StatusAgendamento.REALIZADO) {
            throw new IllegalArgumentException("Não é possível cancelar um agendamento realizado.");
        }
        agendamento.cancelar();
        return agendamentoRepository.save(agendamento);
    }

    @Transactional
    public Agendamento confirmarPagamento(Long agendamentoId, String clienteKeycloakId) {
        Agendamento agendamento = buscarPorId(agendamentoId);
        validarCliente(agendamento, clienteKeycloakId);
        pagamentoService.confirmarPagamento(agendamentoId);
        agendamento.confirmar();
        return agendamentoRepository.save(agendamento);
    }

    /**
     * Confirma pagamento pelo id do agendamento (usado pelo webhook AbacatePay).
     * Só altera estado se o agendamento estiver ACEITO e o pagamento ainda pendente.
     */
    @Transactional
    public void confirmarPagamentoPorIdAgendamento(Long agendamentoId) {
        Agendamento agendamento = buscarPorId(agendamentoId);
        if (agendamento.getStatus() != StatusAgendamento.ACEITO) {
            return;
        }
        pagamentoService.confirmarPagamento(agendamentoId);
        agendamento.confirmar();
        agendamentoRepository.save(agendamento);
    }

    @Transactional
    public Agendamento registrarCheckin(Long agendamentoId, String prestadorKeycloakId, DadosLocalizacao localizacao) {
        Agendamento agendamento = buscarPorId(agendamentoId);
        validarPrestador(agendamento, prestadorKeycloakId);
        if (agendamento.getStatus() != StatusAgendamento.CONFIRMADO) {
            throw new IllegalArgumentException("Somente agendamentos confirmados podem receber check-in.");
        }
        if (agendamento.getCheckinEm() != null) {
            throw new IllegalArgumentException("Check-in já foi realizado.");
        }
        agendamento.setCheckinEm(LocalDateTime.now());
        agendamento.setCheckinLatitude(localizacao.latitude());
        agendamento.setCheckinLongitude(localizacao.longitude());
        return agendamentoRepository.save(agendamento);
    }

    @Transactional
    public Agendamento registrarCheckout(Long agendamentoId, String prestadorKeycloakId, DadosLocalizacao localizacao) {
        return registrarCheckoutComFoto(agendamentoId, prestadorKeycloakId, localizacao, null);
    }

    @Transactional
    public Agendamento registrarCheckoutComFoto(Long agendamentoId, String prestadorKeycloakId, DadosLocalizacao localizacao, MultipartFile fotoTrabalho) {
        Agendamento agendamento = buscarPorId(agendamentoId);
        validarPrestador(agendamento, prestadorKeycloakId);
        if (agendamento.getCheckinEm() == null) {
            throw new IllegalArgumentException("É necessário realizar o check-in antes do checkout.");
        }
        if (agendamento.getCheckoutEm() != null) {
            throw new IllegalArgumentException("Checkout já foi realizado.");
        }
        if (fotoTrabalho != null && !fotoTrabalho.isEmpty()) {
            try {
                String caminho = armazenamentoMidiaService.salvar("agendamentos", fotoTrabalho);
                agendamento.setFotoTrabalhoUrl(caminho);
            } catch (IOException e) {
                throw new IllegalArgumentException("Erro ao salvar foto do trabalho: " + e.getMessage());
            }
        }
        agendamento.setCheckoutEm(LocalDateTime.now());
        agendamento.setCheckoutLatitude(localizacao.latitude());
        agendamento.setCheckoutLongitude(localizacao.longitude());
        agendamento.marcarRealizado();
        return agendamentoRepository.save(agendamento);
    }

    private void validarPrestador(Agendamento agendamento, String prestadorKeycloakId) {
        Prestador prestador = prestadorRepository.findByKeycloakId(prestadorKeycloakId)
                .orElseThrow(() -> new IllegalArgumentException("Prestador não encontrado."));
        if (!agendamento.getPrestador().getId().equals(prestador.getId())) {
            throw new IllegalArgumentException("Somente o prestador do agendamento pode executar esta ação.");
        }
    }

    private void validarCliente(Agendamento agendamento, String clienteKeycloakId) {
        Cliente cliente = clienteService.buscarPorKeycloakId(clienteKeycloakId);
        if (!agendamento.getCliente().getId().equals(cliente.getId())) {
            throw new IllegalArgumentException("Somente o cliente do agendamento pode executar esta ação.");
        }
    }
}
