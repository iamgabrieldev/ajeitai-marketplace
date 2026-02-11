package com.ajeitai.backend.controller;

import com.ajeitai.backend.domain.endereco.Endereco;
import com.ajeitai.backend.domain.prestador.CategoriaAtuacao;
import com.ajeitai.backend.domain.prestador.Prestador;
import com.ajeitai.backend.repository.AgendamentoRepository;
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
class PrestadorControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PrestadorRepository prestadorRepository;

    @Autowired
    private DisponibilidadeRepository disponibilidadeRepository;

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        agendamentoRepository.deleteAll();
        disponibilidadeRepository.deleteAll();
        prestadorRepository.deleteAll();
    }

    @Test
    void vincularPrestador_retornaPrestador() throws Exception {
        String payload = """
                {
                  "nomeFantasia": "Casa Limpa",
                  "email": "prestador@email.com",
                  "telefone": "11999999999",
                  "cnpj": "11222333000181",
                  "categoria": "LIMPEZA",
                  "valorServico": 120,
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

        mockMvc.perform(post("/api/prestadores/vincular")
                        .with(jwt().jwt(jwt -> jwt.subject("prestador-1").claim("email", "kc@email.com"))
                                .authorities(new SimpleGrantedAuthority("ROLE_prestador")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeFantasia").value("Casa Limpa"))
                .andExpect(jsonPath("$.email").value("prestador@email.com"));
    }

    @Test
    void buscarMe_retornaPrestador() throws Exception {
        Prestador prestador = Prestador.builder()
                .keycloakId("prestador-1")
                .nomeFantasia("Casa Limpa")
                .categoria(CategoriaAtuacao.LIMPEZA)
                .telefone("11999999999")
                .email("prestador@email.com")
                .ativo(true)
                .endereco(new Endereco("Rua A", "Centro", "12345678", "10", null, "São Paulo", "SP", -23.5, -46.6))
                .build();
        prestadorRepository.save(prestador);

        mockMvc.perform(get("/api/prestadores/me")
                        .with(jwt().jwt(jwt -> jwt.subject("prestador-1").claim("email", "prestador@email.com"))
                                .authorities(new SimpleGrantedAuthority("ROLE_prestador"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeFantasia").value("Casa Limpa"));
    }

    @Test
    void atualizarMe_atualizaDados() throws Exception {
        Prestador prestador = Prestador.builder()
                .keycloakId("prestador-1")
                .nomeFantasia("Casa Limpa")
                .categoria(CategoriaAtuacao.LIMPEZA)
                .telefone("11999999999")
                .email("prestador@email.com")
                .ativo(true)
                .endereco(new Endereco("Rua A", "Centro", "12345678", "10", null, "São Paulo", "SP", -23.5, -46.6))
                .build();
        prestadorRepository.save(prestador);

        String payload = """
                {
                  "nomeFantasia": "Casa Limpa Premium",
                  "telefone": "11888888888",
                  "categoria": "LIMPEZA",
                  "valorServico": 150,
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

        mockMvc.perform(put("/api/prestadores/me")
                        .with(jwt().jwt(jwt -> jwt.subject("prestador-1").claim("email", "prestador@email.com"))
                                .authorities(new SimpleGrantedAuthority("ROLE_prestador")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeFantasia").value("Casa Limpa Premium"))
                .andExpect(jsonPath("$.telefone").value("11888888888"));
    }
}
