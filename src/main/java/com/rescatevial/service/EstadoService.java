package com.rescatevial.service;

import com.rescatevial.domain.Estado;
import com.rescatevial.repository.EstadoRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EstadoService {
    private final EstadoRepository repository;

    public EstadoService(EstadoRepository repository) {
        this.repository = repository;
    }

    public List<Estado> listar() {
        return repository.findAll();
    }
}
