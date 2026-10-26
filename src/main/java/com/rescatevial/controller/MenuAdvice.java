package com.rescatevial.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Comparte con las vistas si el usuario actual puede administrar el sistema. */
@ControllerAdvice
public class MenuAdvice {

    @ModelAttribute("esAdministrador")
    public boolean esAdministrador(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
