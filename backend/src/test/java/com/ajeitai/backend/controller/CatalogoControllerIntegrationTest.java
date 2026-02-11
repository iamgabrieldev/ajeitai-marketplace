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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@WebAppConfiguration
@ActiveProfiles("test")
class CatalogoControllerIntegrationTest {

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
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        agendamentoRepository.deleteAll();
        disponibilidadeRepository.deleteAll();
        prestadorRepository.deleteAll();
        Prestador prestador = Prestador.builder()
                .keycloakId("prestador-1")
                .nomeFantasia("Casa Limpa")
                .categoria(CategoriaAtuacao.LIMPEZA)
                .telefone("11999999999")
                .email("prestador@teste.com")
                .ativo(true)
                .endereco(new Endereco("Rua A", "Centro", "12345678", "10", null, "São Paulo", "SP", -23.5, -46.6))
                .build();
        prestadorRepository.save(prestador);
    }

    @Test
    void listarPrestadores_retornaCatalogo() throws Exception {
        mockMvc.perform(get("/api/catalogo/prestadores")
                        .param("cidade", "São Paulo")
                        .param("uf", "SP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nomeFantasia").value("Casa Limpa"))
                .andExpect(jsonPath("$[0].categoria").value("LIMPEZA"));
    }
}
