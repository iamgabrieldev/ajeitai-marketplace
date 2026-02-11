package com.ajeitai.backend.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    @GetMapping("/perfil")
    public Map<String, Object> getPerfil(@AuthenticationPrincipal Jwt jwt) {
        var usuarioId = jwt.getSubject();
        var email = jwt.getClaimAsString("email");
        var roles = jwt.getClaimAsMap("realm_access");

        return Map.of(
                "id", usuarioId,
                "email", email,
                "token_id", jwt.getId(),
                "claims_completas", jwt.getClaims(),
                "roles", roles
        );
    }


}
