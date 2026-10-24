package com.rescatevial.service;

import com.rescatevial.domain.Usuario;
import com.rescatevial.repository.UsuarioRepository;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();
    private final UsuarioRepository repository;

    public UsuarioService(UsuarioRepository repository) {
        this.repository = repository;
    }

    public List<Usuario> listar() { return repository.findAll(); }
    public List<Map<String, Object>> listarTipos() { return repository.findTiposUsuario(); }
    public List<Map<String, Object>> listarEstados() { return repository.findEstados(); }

    @Transactional
    public void registrar(Usuario usuario) {
        usuario.setId(repository.nextUserId());
        usuario.setFechaCreacion(LocalDateTime.now());
        repository.insertUsingProcedure(usuario, PASSWORD_ENCODER.encode(usuario.getPassword()));
    }
}
