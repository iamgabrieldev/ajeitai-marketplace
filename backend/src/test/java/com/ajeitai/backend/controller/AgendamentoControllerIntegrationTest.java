package com.ajeitai.backend.controller;

import com.ajeitai.backend.domain.agendamento.Disponibilidade;
import com.ajeitai.backend.domain.agendamento.FormaPagamento;
import com.ajeitai.backend.domain.cliente.Cliente;
import com.ajeitai.backend.domain.endereco.Endereco;
import com.ajeitai.backend.domain.prestador.Prestador;
import com.ajeitai.backend.repository.ClienteRepository;
import com.ajeitai.backend.repository.DisponibilidadeRepository;
import com.ajeitai.backend.repository.PrestadorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@WebAppConfiguration
@ActiveProfiles("test")
class AgendamentoControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PrestadorRepository prestadorRepository;

    @Autowired
    private DisponibilidadeRepository disponibilidadeRepository;

    private Prestador prestador;
    private LocalDateTime dataHoraBase;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        disponibilidadeRepository.deleteAll();
        prestadorRepository.deleteAll();
        clienteRepository.deleteAll();

        Cliente cliente = Cliente.builder()
                .keycloakId("cliente-1")
                .nome("Cliente")
                .email("cliente@teste.com")
                .telefone("11999999999")
                .cpf("12345678909")
                .ativo(true)
                .endereco(new Endereco("Rua A", "Centro", "12345678", "10", null, "São Paulo", "SP", -23.5, -46.6))
                .build();
        clienteRepository.save(cliente);

        prestador = Prestador.builder()
                .keycloakId("prestador-1")
                .nomeFantasia("Casa Limpa")
                .telefone("11999999999")
                .email("prestador@teste.com")
                .ativo(true)
                .endereco(new Endereco("Rua B", "Centro", "12345678", "20", null, "São Paulo", "SP", -23.5, -46.6))
                .build();
        prestadorRepository.save(prestador);

        dataHoraBase = LocalDateTime.now()
                .plusDays(1)
                .withHour(10)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        Disponibilidade disponibilidade = Disponibilidade.builder()
                .prestador(prestador)
                .diaSemana(dataHoraBase.getDayOfWeek().getValue())
                .horaInicio(LocalTime.of(8, 0))
                .horaFim(LocalTime.of(18, 0))
                .build();
        disponibilidadeRepository.save(disponibilidade);
    }

    @Test
    void criarAgendamento_retornaPendente() throws Exception {
        LocalDateTime dataHora = dataHoraBase;
        String payload = """
                {
                  "prestadorId": %d,
                  "dataHora": "%s",
                  "formaPagamento": "%s",
                  "observacao": "Teste"
                }
                """.formatted(prestador.getId(), dataHora, FormaPagamento.ONLINE.name());

        mockMvc.perform(post("/api/agendamentos")
                        .with(jwt().jwt(jwt -> jwt.subject("cliente-1").claim("email", "cliente@teste.com"))
                                .authorities(new SimpleGrantedAuthority("ROLE_cliente")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }
}
