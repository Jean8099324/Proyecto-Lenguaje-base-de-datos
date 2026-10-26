package com.rescatevial.controller;

import com.rescatevial.domain.Estado;
import com.rescatevial.service.EstadoService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/estados")
public class EstadoController {
    private final EstadoService service;

    public EstadoController(EstadoService service) {
        this.service = service;
    }

    @GetMapping
    public List<Estado> listar() {
        return service.listar();
    }
}
