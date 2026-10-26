package com.rescatevial.service;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

/** UserDetails con el nombre completo disponible para las vistas. */
public class UsuarioPrincipal extends User {
    private final String nombreCompleto;

    public UsuarioPrincipal(String username, String password,
                            Collection<? extends GrantedAuthority> authorities,
                            String nombreCompleto) {
        super(username, password, authorities);
        this.nombreCompleto = nombreCompleto;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }
}
