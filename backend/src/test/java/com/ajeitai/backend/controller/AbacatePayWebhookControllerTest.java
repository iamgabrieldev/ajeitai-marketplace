package com.ajeitai.backend.controller;

import com.ajeitai.backend.domain.agendamento.Agendamento;
import com.ajeitai.backend.domain.agendamento.FormaPagamento;
import com.ajeitai.backend.domain.agendamento.StatusAgendamento;
import com.ajeitai.backend.domain.cliente.Cliente;
import com.ajeitai.backend.domain.endereco.Endereco;
import com.ajeitai.backend.domain.pagamento.Pagamento;
import com.ajeitai.backend.domain.pagamento.StatusPagamento;
import com.ajeitai.backend.domain.prestador.Prestador;
import com.ajeitai.backend.repository.AgendamentoRepository;
import com.ajeitai.backend.repository.ClienteRepository;
import com.ajeitai.backend.repository.PagamentoRepository;
import com.ajeitai.backend.repository.PrestadorRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@WebAppConfiguration
@ActiveProfiles("test")
class AbacatePayWebhookControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PrestadorRepository prestadorRepository;

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private PagamentoRepository pagamentoRepository;

    private Agendamento agendamento;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        pagamentoRepository.deleteAll();
        agendamentoRepository.deleteAll();
        prestadorRepository.deleteAll();
        clienteRepository.deleteAll();

        Cliente cliente = Cliente.builder()
                .keycloakId("cliente-webhook")
                .nome("Cliente")
                .email("c@test.com")
                .telefone("11999999999")
                .cpf("12345678909")
                .ativo(true)
                .endereco(new Endereco("Rua A", "B", "12345678", "1", null, "SP", "SP", -23.5, -46.6))
                .build();
        clienteRepository.save(cliente);

        Prestador prestador = Prestador.builder()
                .keycloakId("prestador-webhook")
                .nomeFantasia("Prestador")
                .telefone("11999999999")
                .email("p@test.com")
                .ativo(true)
                .valorServico(BigDecimal.valueOf(100))
                .endereco(new Endereco("Rua B", "B", "12345678", "2", null, "SP", "SP", -23.5, -46.6))
                .build();
        prestadorRepository.save(prestador);

        agendamento = Agendamento.builder()
                .cliente(cliente)
                .prestador(prestador)
                .dataHora(LocalDateTime.now().plusDays(1))
                .status(StatusAgendamento.ACEITO)
                .formaPagamento(FormaPagamento.ONLINE)
                .valorServico(BigDecimal.valueOf(100))
                .build();
        agendamento = agendamentoRepository.save(agendamento);

        Pagamento pagamento = Pagamento.builder()
                .agendamento(agendamento)
                .status(StatusPagamento.PENDENTE)
                .billingId("bill_test")
                .build();
        pagamentoRepository.save(pagamento);
    }

    @Test
    void billingPaid_comExternalIdAgendamento_confirmaPagamentoEAprendamento() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "id", "evt_1",
                "event", "billing.paid",
                "data", Map.of(
                        "billing", Map.of(
                                "id", "bill_123",
                                "status", "PAID",
                                "products", List.of(Map.of("externalId", "ag-" + agendamento.getId()))
                        )
                )
        ));

        mockMvc.perform(post("/api/webhooks/abacatepay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        Pagamento pag = pagamentoRepository.findByAgendamentoId(agendamento.getId()).orElseThrow();
        assertThat(pag.getStatus()).isEqualTo(StatusPagamento.CONFIRMADO);

        Agendamento ag = agendamentoRepository.findById(agendamento.getId()).orElseThrow();
        assertThat(ag.getStatus()).isEqualTo(StatusAgendamento.CONFIRMADO);
    }
}
