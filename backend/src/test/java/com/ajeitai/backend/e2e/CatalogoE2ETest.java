package com.ajeitai.backend.e2e;

import com.ajeitai.backend.domain.endereco.Endereco;
import com.ajeitai.backend.domain.prestador.CategoriaAtuacao;
import com.ajeitai.backend.domain.prestador.Prestador;
import com.ajeitai.backend.repository.PrestadorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = RANDOM_PORT)
class CatalogoE2ETest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    private final RestTemplate restTemplate = new RestTemplate();

    @LocalServerPort
    private int port;

    @Autowired
    private PrestadorRepository prestadorRepository;

    @BeforeEach
    void setup() {
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
    void catalogoRetornaPrestadores() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/catalogo/prestadores",
                String.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("Casa Limpa");
    }
}
