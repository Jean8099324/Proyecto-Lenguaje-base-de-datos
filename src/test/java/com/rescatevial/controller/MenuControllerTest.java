package com.rescatevial.controller;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.junit.jupiter.api.Assertions.*;

class MenuControllerTest {
    private final MenuController controller = new MenuController();

    @Test
    void informaAdministracionSoloAlRolAdmin() {
        var admin = new UsernamePasswordAuthenticationToken("admin", "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        var operador = new UsernamePasswordAuthenticationToken("operador", "", List.of(new SimpleGrantedAuthority("ROLE_CLIENT")));
        assertTrue(controller.estado(admin).get("esAdministrador"));
        assertFalse(controller.estado(operador).get("esAdministrador"));
    }
}
