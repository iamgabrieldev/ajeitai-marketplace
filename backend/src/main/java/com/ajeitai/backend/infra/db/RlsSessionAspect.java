package com.ajeitai.backend.infra.db;

import com.ajeitai.backend.infra.security.UserContext;
import com.ajeitai.backend.infra.security.UserContextHolder;
import com.ajeitai.backend.infra.security.UserType;
import com.ajeitai.backend.service.ClienteService;
import com.ajeitai.backend.service.PrestadorService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * Configura variáveis de sessão usadas pelas políticas de RLS no PostgreSQL
 * (app.current_cliente_id, app.current_prestador_id, app.current_role)
 * no início de métodos transacionais da camada de serviço.
 *
 * Ativo apenas no profile "prod", onde o banco é PostgreSQL e as migrations
 * de RLS estão habilitadas via Flyway.
 */
@Aspect
@Component
@Profile("prod")
@RequiredArgsConstructor
public class RlsSessionAspect {

    private final EntityManager entityManager;
    private final ClienteService clienteService;
    private final PrestadorService prestadorService;

    @Around("within(com.ajeitai.backend.service..*) && @annotation(transactional)")
    public Object applyRlsContext(ProceedingJoinPoint pjp, Transactional transactional) throws Throwable {
        UserContext context = UserContextHolder.get();
        if (context != null) {
            applySessionVariables(context);
        }
        return pjp.proceed();
    }

    private void applySessionVariables(UserContext context) {
        Session session = entityManager.unwrap(Session.class);
        session.doWork(connection -> {
            try (var stmt = connection.createStatement()) {
                // Limpa sempre os valores anteriores na conexão (por segurança)
                stmt.execute("RESET LOCAL app.current_cliente_id");
                stmt.execute("RESET LOCAL app.current_prestador_id");
                stmt.execute("RESET LOCAL app.current_role");

                if (context.isAdmin()) {
                    stmt.execute("SET LOCAL app.current_role = 'ADMIN'");
                    return;
                }

                if (context.isCliente()) {
                    Long clienteId = context.getClienteId();
                    if (clienteId == null) {
                        clienteId = clienteService.buscarPorKeycloakId(context.getKeycloakId()).getId();
                        context.setClienteId(clienteId);
                    }
                    stmt.execute("SET LOCAL app.current_role = 'CLIENTE'");
                    stmt.execute("SET LOCAL app.current_cliente_id = '" + clienteId + "'");
                    return;
                }

                if (context.isPrestador()) {
                    Long prestadorId = context.getPrestadorId();
                    if (prestadorId == null) {
                        prestadorId = prestadorService.buscarPorKeycloakId(context.getKeycloakId()).getId();
                        context.setPrestadorId(prestadorId);
                    }
                    stmt.execute("SET LOCAL app.current_role = 'PRESTADOR'");
                    stmt.execute("SET LOCAL app.current_prestador_id = '" + prestadorId + "'");
                }
            }
        });
    }
}

