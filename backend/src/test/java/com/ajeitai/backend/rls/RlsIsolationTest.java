package com.ajeitai.backend.rls;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

/**
 * Teste de isolamento via RLS diretamente no PostgreSQL.
 *
 * Cenário:
 * - 2 clientes (ids 1 e 2)
 * - 2 prestadores (ids 1 e 2)
 * - 3 agendamentos:
 *   - ag1: cliente 1, prestador 1
 *   - ag2: cliente 2, prestador 1
 *   - ag3: cliente 1, prestador 2
 *
 * Verificamos que, ao setar app.current_cliente_id ou app.current_prestador_id,
 * apenas as linhas \"donas\" são visíveis, conforme políticas de RLS.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = NONE)
class RlsIsolationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setupData() {
        // Limpa dados e reinicia sequences
        jdbcTemplate.execute("TRUNCATE TABLE pagamentos, avaliacoes, portfolio_itens, notificacao_tokens, agendamentos, disponibilidades, prestadores, clientes RESTART IDENTITY CASCADE");

        // Clientes 1 e 2
        jdbcTemplate.update("INSERT INTO clientes(keycloak_id, nome, email, ativo) VALUES ('cliente-1','Cliente 1','c1@teste.com',true)");
        jdbcTemplate.update("INSERT INTO clientes(keycloak_id, nome, email, ativo) VALUES ('cliente-2','Cliente 2','c2@teste.com',true)");

        // Prestadores 1 e 2
        jdbcTemplate.update("INSERT INTO prestadores(keycloak_id, nome_fantasia, email, ativo) VALUES ('prestador-1','Prestador 1','p1@teste.com',true)");
        jdbcTemplate.update("INSERT INTO prestadores(keycloak_id, nome_fantasia, email, ativo) VALUES ('prestador-2','Prestador 2','p2@teste.com',true)");

        // Agendamentos:
        // ag1: cliente 1, prestador 1
        jdbcTemplate.update("INSERT INTO agendamentos(cliente_id, prestador_id, data_hora, status) VALUES (1, 1, now(), 'PENDENTE')");
        // ag2: cliente 2, prestador 1
        jdbcTemplate.update("INSERT INTO agendamentos(cliente_id, prestador_id, data_hora, status) VALUES (2, 1, now(), 'PENDENTE')");
        // ag3: cliente 1, prestador 2
        jdbcTemplate.update("INSERT INTO agendamentos(cliente_id, prestador_id, data_hora, status) VALUES (1, 2, now(), 'PENDENTE')");
    }

    @Test
    void cliente1EnxergaSomenteSeusAgendamentos() {
        int count = jdbcTemplate.execute(con -> {
            try (var stmt = con.createStatement()) {
                stmt.execute("RESET ALL");
                stmt.execute("SET app.current_cliente_id = '1'");
                var rs = stmt.executeQuery("SELECT COUNT(*) FROM agendamentos");
                rs.next();
                return rs.getInt(1);
            }
        });

        // cliente 1 participa de ag1 e ag3
        assertThat(count).isEqualTo(2);
    }

    @Test
    void cliente2EnxergaSomenteSeusAgendamentos() {
        int count = jdbcTemplate.execute(con -> {
            try (var stmt = con.createStatement()) {
                stmt.execute("RESET ALL");
                stmt.execute("SET app.current_cliente_id = '2'");
                var rs = stmt.executeQuery("SELECT COUNT(*) FROM agendamentos");
                rs.next();
                return rs.getInt(1);
            }
        });

        // cliente 2 participa apenas de ag2
        assertThat(count).isEqualTo(1);
    }

    @Test
    void prestador1EnxergaSomenteSeusAgendamentos() {
        int count = jdbcTemplate.execute(con -> {
            try (var stmt = con.createStatement()) {
                stmt.execute("RESET ALL");
                stmt.execute("SET app.current_prestador_id = '1'");
                var rs = stmt.executeQuery("SELECT COUNT(*) FROM agendamentos");
                rs.next();
                return rs.getInt(1);
            }
        });

        // prestador 1 participa de ag1 e ag2
        assertThat(count).isEqualTo(2);
    }

    @Test
    void acessoSemContextoDeUsuarioNaoVêNada() {
        int count = jdbcTemplate.execute(con -> {
            try (var stmt = con.createStatement()) {
                stmt.execute("RESET ALL");
                var rs = stmt.executeQuery("SELECT COUNT(*) FROM agendamentos");
                rs.next();
                return rs.getInt(1);
            }
        });

        assertThat(count).isEqualTo(0);
    }
}

