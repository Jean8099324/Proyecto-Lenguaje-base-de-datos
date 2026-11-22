package com.rescatevial.service;

import com.rescatevial.domain.Usuario;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** Gestiona clientes mediante los procedimientos de usuarios ya existentes en Oracle. */
@Service
public class ClienteService {
    private final UsuarioService usuarioService;
    private final TipoUsuarioService tipoUsuarioService;
    private final EstadoService estadoService;

    public ClienteService(UsuarioService usuarioService, TipoUsuarioService tipoUsuarioService,
                          EstadoService estadoService) {
        this.usuarioService = usuarioService;
        this.tipoUsuarioService = tipoUsuarioService;
        this.estadoService = estadoService;
    }

    public List<Usuario> listar() {
        return usuarioService.getUsuarios(false).stream()
                .filter(this::esCliente)
                .toList();
    }

    public com.rescatevial.repository.UsuarioRepository.PaginaUsuarios listarPaginado(int pagina, int tamanio) {
        return usuarioService.getClientesPaginados(pagina, tamanio);
    }

    public Optional<Usuario> buscar(Long id) {
        return usuarioService.getUsuarioPorId(id).filter(this::esCliente);
    }

    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioService.getUsuarioPorUsername(username);
    }

    public Long tipoClienteId() {
        return tipoUsuarioService.listarActivos().stream()
                .filter(tipo -> "CLIENTE".equalsIgnoreCase(tipo.getNombreTipoUsuario()))
                .map(tipo -> tipo.getId())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No existe el tipo de usuario CLIENTE activo."));
    }

    public Long estadoActivoId() {
        return estadoService.listar().stream()
                .filter(estado -> "ACTIVO".equalsIgnoreCase(estado.getNombreEstado()))
                .map(estado -> estado.getId())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No existe el estado ACTIVO."));
    }

    public void guardar(Usuario formulario) {
        formulario.setTipoUsuario(tipoClienteId()); // No confiar en el tipo enviado desde el navegador.
        if (formulario.getId() == null) {
            formulario.setEstado(estadoActivoId());
            formulario.setRolesNombresSeleccionados(java.util.Set.of("CLIENT"));
        } else {
            Usuario existente = buscar(formulario.getId())
                    .orElseThrow(() -> new IllegalArgumentException("No existe ese cliente."));
            // El formulario de clientes no administra roles ni estados.
            formulario.setEstado(existente.getEstado());
            formulario.setRolesNombresSeleccionados(existente.getRoles().stream()
                    .map(rol -> rol.getNombre()).collect(Collectors.toSet()));
        }
        usuarioService.save(formulario);
        if (formulario.getId() == null) {
            formulario.setId(usuarioService.getUsuarioPorUsername(formulario.getUsername())
                    .orElseThrow(() -> new IllegalStateException("No se pudo recuperar el cliente creado."))
                    .getId());
        }
    }

    public void desactivar(Long id) {
        buscar(id).orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado."));
        usuarioService.delete(id); // Procedimiento FIDE_ELIMINAR_USUARIO_SP (borrado logico).
    }

    public void reactivar(Long id) {
        Usuario cliente = buscar(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado."));
        cliente.setEstado(estadoActivoId());
        cliente.setPassword(null); // Conservar la contraseña almacenada.
        cliente.setRolesNombresSeleccionados(cliente.getRoles().stream()
                .map(rol -> rol.getNombre()).collect(Collectors.toSet()));
        usuarioService.save(cliente); // Procedimiento FIDE_ACTUALIZAR_USUARIO_SP.
    }

    private boolean esCliente(Usuario usuario) {
        return "CLIENTE".equalsIgnoreCase(usuario.getTipoUsuarioNombre());
    }
}
