package com.ajeitai.backend.controller;

import com.ajeitai.backend.domain.cliente.Cliente;
import com.ajeitai.backend.domain.endereco.Endereco;
import com.ajeitai.backend.repository.AgendamentoRepository;
import com.ajeitai.backend.repository.ClienteRepository;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@WebAppConfiguration
@ActiveProfiles("test")
class ClienteControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        agendamentoRepository.deleteAll();
        clienteRepository.deleteAll();
    }

    @Test
    void vincularCliente_retornaCliente() throws Exception {
        String payload = """
                {
                  "nome": "Joao",
                  "email": "joao@email.com",
                  "telefone": "11999999999",
                  "cpf": "12345678909",
                  "endereco": {
                    "logradouro": "Rua A",
                    "bairro": "Centro",
                    "cep": "12345678",
                    "cidade": "São Paulo",
                    "uf": "SP",
                    "numero": "10"
                  }
                }
                """;

        mockMvc.perform(post("/api/clientes/vincular")
                        .with(jwt().jwt(jwt -> jwt.subject("cliente-1").claim("email", "kc@email.com"))
                                .authorities(new SimpleGrantedAuthority("ROLE_cliente")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Joao"))
                .andExpect(jsonPath("$.email").value("kc@email.com"));
    }

    @Test
    void buscarMe_retornaCliente() throws Exception {
        Cliente cliente = Cliente.builder()
                .keycloakId("cliente-1")
                .nome("Joao")
                .email("joao@email.com")
                .telefone("11999999999")
                .cpf("12345678909")
                .ativo(true)
                .endereco(new Endereco("Rua A", "Centro", "12345678", "10", null, "São Paulo", "SP", -23.5, -46.6))
                .build();
        clienteRepository.save(cliente);

        mockMvc.perform(get("/api/clientes/me")
                        .with(jwt().jwt(jwt -> jwt.subject("cliente-1").claim("email", "joao@email.com"))
                                .authorities(new SimpleGrantedAuthority("ROLE_cliente"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Joao"));
    }

    @Test
    void atualizarMe_atualizaDados() throws Exception {
        Cliente cliente = Cliente.builder()
                .keycloakId("cliente-1")
                .nome("Joao")
                .email("joao@email.com")
                .telefone("11999999999")
                .cpf("12345678909")
                .ativo(true)
                .endereco(new Endereco("Rua A", "Centro", "12345678", "10", null, "São Paulo", "SP", -23.5, -46.6))
                .build();
        clienteRepository.save(cliente);

        String payload = """
                {
                  "nome": "Joao Silva",
                  "telefone": "11888888888",
                  "endereco": {
                    "logradouro": "Rua B",
                    "bairro": "Centro",
                    "cep": "12345678",
                    "cidade": "São Paulo",
                    "uf": "SP",
                    "numero": "20"
                  }
                }
                """;

        mockMvc.perform(put("/api/clientes/me")
                        .with(jwt().jwt(jwt -> jwt.subject("cliente-1").claim("email", "joao@email.com"))
                                .authorities(new SimpleGrantedAuthority("ROLE_cliente")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Joao Silva"))
                .andExpect(jsonPath("$.telefone").value("11888888888"));
    }
}
