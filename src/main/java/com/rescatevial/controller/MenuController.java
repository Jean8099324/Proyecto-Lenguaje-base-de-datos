package com.rescatevial.controller;

import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expone únicamente las capacidades del usuario autenticado para completar la navegación. */
@RestController
public class MenuController {

    @GetMapping("/menu/estado")
    public Map<String, Boolean> estado(Authentication authentication) {
        boolean administrador = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        return Map.of("esAdministrador", administrador);
    }
}
