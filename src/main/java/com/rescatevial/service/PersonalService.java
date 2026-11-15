package com.rescatevial.service;

import com.rescatevial.domain.TipoUsuario;
import com.rescatevial.domain.Usuario;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** Gestiona personal sin crear tablas adicionales ni ejecutar SQL desde Java. */
@Service
public class PersonalService {
    private final UsuarioService usuarioService;
    private final TipoUsuarioService tipoUsuarioService;
    private final EstadoService estadoService;

    public PersonalService(UsuarioService usuarioService,
                           TipoUsuarioService tipoUsuarioService,
                           EstadoService estadoService) {
        this.usuarioService = usuarioService;
        this.tipoUsuarioService = tipoUsuarioService;
        this.estadoService = estadoService;
    }

    public List<Usuario> listar() {
        return usuarioService.getUsuarios(false).stream()
                .filter(this::esPersonal).toList();
    }

    public Optional<Usuario> buscar(Long id) {
        return usuarioService.getUsuarioPorId(id).filter(this::esPersonal);
    }

    public List<TipoUsuario> tiposDePersonal() {
        return tipoUsuarioService.listarActivos().stream()
                .filter(tipo -> tipo.getNombreTipoUsuario() != null)
                .filter(tipo -> !"CLIENTE".equalsIgnoreCase(tipo.getNombreTipoUsuario().trim()))
                .toList();
    }

    public Long estadoActivoId() {
        return estadoService.listar().stream()
                .filter(estado -> "ACTIVO".equalsIgnoreCase(estado.getNombreEstado()))
                .map(estado -> estado.getId())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No existe el estado ACTIVO."));
    }

    public void guardar(Usuario formulario) {
        if (formulario.getTipoUsuario() == null || tiposDePersonal().stream()
                .noneMatch(t -> formulario.getTipoUsuario().equals(t.getId()))) {
            throw new IllegalArgumentException("Seleccione un tipo de personal válido.");
        }
        if (formulario.getId() == null) {
            formulario.setEstado(estadoActivoId());
            // Tipo de usuario y rol de seguridad son conceptos distintos.
            // El alta recibe rol GUEST; los accesos se asignan en Roles y permisos.
            formulario.setRolesNombresSeleccionados(Set.of("GUEST"));
        } else {
            Usuario actual = buscar(formulario.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Personal no encontrado."));
            formulario.setEstado(actual.getEstado());
            formulario.setRolesNombresSeleccionados(actual.getRoles().stream()
                    .map(rol -> rol.getNombre()).collect(Collectors.toSet()));
        }
        usuarioService.save(formulario);
    }

    public void desactivar(Long id, String usernameActual) {
        Usuario persona = buscar(id)
                .orElseThrow(() -> new IllegalArgumentException("Personal no encontrado."));
        if (persona.getUsername().equalsIgnoreCase(usernameActual)) {
            throw new IllegalArgumentException("No puedes desactivar tu propia cuenta.");
        }
        usuarioService.delete(id);
    }

    public void reactivar(Long id) {
        Usuario persona = buscar(id)
                .orElseThrow(() -> new IllegalArgumentException("Personal no encontrado."));
        persona.setEstado(estadoActivoId());
        persona.setPassword(null);
        persona.setRolesNombresSeleccionados(persona.getRoles().stream()
                .map(rol -> rol.getNombre()).collect(Collectors.toSet()));
        usuarioService.save(persona);
    }

    private boolean esPersonal(Usuario usuario) {
        return usuario.getTipoUsuarioNombre() != null
                && !"CLIENTE".equalsIgnoreCase(usuario.getTipoUsuarioNombre().trim());
    }
}
