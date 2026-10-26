package com.rescatevial.controller;

import com.rescatevial.domain.TipoUsuario;
import com.rescatevial.service.TipoUsuarioService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tipos-usuario")
public class TipoUsuarioController {
    private final TipoUsuarioService service;

    public TipoUsuarioController(TipoUsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public List<TipoUsuario> listar() {
        return service.listarActivos();
    }
}
