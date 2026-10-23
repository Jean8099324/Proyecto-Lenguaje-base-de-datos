package com.rescatevial.service;

import com.rescatevial.domain.Cliente;
import com.rescatevial.repository.ClienteRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteService {
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();
    private final ClienteRepository repository;

    public ClienteService(ClienteRepository repository) {
        this.repository = repository;
    }

    public List<Cliente> listar() { return repository.findAll(); }
    public long contarActivos() { return repository.countByState(1); }
    public long contarInactivos() { return repository.countByState(2); }
    public long contarVehiculos() { return repository.countVehicles(); }
    public Cliente buscar(long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("No se encontró el cliente."));
    }

    @Transactional
    public void registrar(Cliente form) {
        if (repository.usernameExists(form.getUsername().trim())) {
            throw new IllegalArgumentException("Ese nombre de usuario ya está registrado.");
        }
        try {
            repository.save(repository.nextId(), form, PASSWORD_ENCODER.encode(form.getPassword()));
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("El usuario, correo o teléfono ya existe.", ex);
        }
    }

    @Transactional
    public void cambiarEstado(long id, boolean activo) {
        repository.updateState(id, activo ? 1 : 2);
    }
}
