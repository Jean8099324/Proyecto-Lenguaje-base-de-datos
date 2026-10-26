package com.rescatevial.service;

import com.rescatevial.domain.TipoUsuario;
import com.rescatevial.repository.TipoUsuarioRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TipoUsuarioService {
    private final TipoUsuarioRepository repository;

    public TipoUsuarioService(TipoUsuarioRepository repository) {
        this.repository = repository;
    }

    public List<TipoUsuario> listarActivos() {
        return repository.findAllActivos();
    }
}
