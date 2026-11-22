package com.rescatevial.service;

import com.rescatevial.domain.Usuario;
import com.rescatevial.domain.Rol;
import com.rescatevial.repository.UsuarioRepository;
import com.rescatevial.repository.DatoContactoRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final DatoContactoRepository datoContactoRepository;
    private final EstadoService estadoService;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                          DatoContactoRepository datoContactoRepository, EstadoService estadoService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.datoContactoRepository = datoContactoRepository;
        this.estadoService = estadoService;
    }

    @Transactional(readOnly = true)
    public List<Usuario> getUsuarios(boolean activo) {
        if (activo) {
            return usuarioRepository.findByActivoTrue();
        }
        return usuarioRepository.findAll();
    }

    @Transactional(readOnly = true)
    public UsuarioRepository.PaginaUsuarios getUsuariosPaginados(int numeroPagina, int tamanioPagina) {
        if (numeroPagina < 1 || tamanioPagina < 1) {
            throw new IllegalArgumentException("La página y su tamaño deben ser mayores que cero.");
        }
        return usuarioRepository.findAllPaginado(numeroPagina, tamanioPagina);
    }

    @Transactional(readOnly = true)
    public UsuarioRepository.PaginaUsuarios getClientesPaginados(int numeroPagina, int tamanioPagina) {
        if (numeroPagina < 1 || tamanioPagina < 1) {
            throw new IllegalArgumentException("La página y su tamaño deben ser mayores que cero.");
        }
        return usuarioRepository.findClientesPaginado(numeroPagina, tamanioPagina);
    }

    @Transactional(readOnly = true)
    public List<String> getUsernames() {
        return usuarioRepository.findAllUsernames();
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> getUsuarioPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> getUsuarioPorUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    @Transactional
    public void save(Usuario usuario) {
        boolean cliente = usuarioRepository.esPerfilCliente(usuario.getTipoUsuario());
        if (cliente) usuario.setRolesNombresSeleccionados(java.util.Set.of("CLIENT"));
        if (usuario.getId() == null) {// Si el usuario es nuevo
            if (usuario.getPassword() == null || usuario.getPassword().isBlank()) {
                throw new IllegalArgumentException("La contraseña es obligatoria para un usuario nuevo.");
            }
            // Oracle genera el ID; la aplicación solo envía la contraseña cifrada al procedimiento.
            usuarioRepository.insertUsingProcedure(usuario, passwordEncoder.encode(usuario.getPassword()));
            Usuario creado = usuarioRepository.findByUsername(usuario.getUsername())
                    .orElseThrow(() -> new IllegalStateException("No se pudo recuperar el usuario creado."));
            if (usuario.getCorreo() != null && !usuario.getCorreo().isBlank()) {
                var correo = new com.rescatevial.domain.DatoContacto();
                correo.setIdUsuario(creado.getId());
                correo.setValor(usuario.getCorreo().trim());
                correo.setIdEstado(estadoService.listar().stream()
                        .filter(estado -> "ACTIVO".equalsIgnoreCase(estado.getNombreEstado()))
                        .map(estado -> estado.getId()).findFirst()
                        .orElseThrow(() -> new IllegalStateException("No existe el estado ACTIVO en Oracle.")));
                datoContactoRepository.insertarCorreo(correo);
            }
            // Los clientes reciben exclusivamente CLIENT, según su perfil.
            var rolesSeleccionados = rolesSeleccionados(usuario.getRolesNombresSeleccionados());
            if (rolesSeleccionados.isEmpty()) rolesSeleccionados = java.util.Set.of("GUEST");
            if (cliente) {
                actualizarRoles(creado, java.util.Set.of("CLIENT"));
                return;
            }
            for (String rol : rolesSeleccionados) {
                asignarRolPorUsername(usuario.getUsername(), rol);
            }
            return;
        }

        Usuario usuarioExistente = usuarioRepository.findById(usuario.getId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + usuario.getId()));
        if (usuario.getPassword() == null || usuario.getPassword().isBlank()) {
            // Un campo vacío significa conservar el hash actual.
            usuario.setPassword(usuarioExistente.getPassword());
        } else {
            // Solo se reemplaza el hash cuando se ingresó una contraseña nueva.
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        }
        usuario.setRoles(usuarioExistente.getRoles());
        usuarioRepository.updateUsingProcedure(usuario, usuario.getPassword());
        actualizarRoles(usuarioExistente, rolesSeleccionados(usuario.getRolesNombresSeleccionados()));
    }

    @Transactional
    public void actualizarNombreDesdeIncidente(Long id, String nombre, String primerApellido, String segundoApellido) {
        Usuario existente = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro el cliente asociado al vehiculo."));
        existente.setNombre(nombre == null ? "" : nombre.trim());
        existente.setPrimerApellido(primerApellido == null ? "" : primerApellido.trim());
        existente.setSegundoApellido(segundoApellido == null || segundoApellido.isBlank() ? null : segundoApellido.trim());
        usuarioRepository.updateUsingProcedure(existente, existente.getPassword());
    }

    @Transactional
    public void cambiarPasswordPropia(String username, String actual, String nueva, String confirmacion) {
        Usuario usuario = getUsuarioPorUsername(username).orElseThrow();
        if (actual == null || !passwordEncoder.matches(actual, usuario.getPassword()))
            throw new IllegalArgumentException("La contraseña actual no es correcta.");
        if (nueva == null || nueva.length() < 8 || nueva.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("La nueva contraseña debe tener al menos 8 caracteres y un máximo de 72 bytes.");
        if (!nueva.equals(confirmacion)) throw new IllegalArgumentException("Las contraseñas no coinciden.");
        usuarioRepository.updateUsingProcedure(usuario, passwordEncoder.encode(nueva));
        Usuario actualizado = getUsuarioPorUsername(username).orElseThrow();
        if (!passwordEncoder.matches(nueva, actualizado.getPassword()))
            throw new IllegalStateException("Oracle no confirmó el cambio de contraseña.");
    }

    private static java.util.Set<String> rolesSeleccionados(java.util.Set<String> nombres) {
        if (nombres == null) return java.util.Set.of();
        return nombres.stream().filter(nombre -> nombre != null).map(nombre -> nombre.trim())
                .filter(nombre -> !nombre.isBlank())
                .map(nombre -> nombre.toUpperCase(java.util.Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());
    }

    private void actualizarRoles(Usuario usuario, java.util.Set<String> nombresSeleccionados) {
        var idsSeleccionados = nombresSeleccionados.stream()
                .map(nombre -> usuarioRepository.findRoleIdByName(nombre)
                        .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + nombre)))
                .collect(java.util.stream.Collectors.toSet());
        for (Rol rol : usuario.getRoles()) {
            if (!idsSeleccionados.contains(rol.getId())) {
                usuarioRepository.removeRoleFromUser(usuario.getId(), rol.getId());
            }
        }
        var idsActuales = usuario.getRoles().stream().map(rol -> rol.getId()).collect(java.util.stream.Collectors.toSet());
        for (Long idRol : idsSeleccionados) {
            if (!idsActuales.contains(idRol)) {
                usuarioRepository.assignRoleToUser(usuario.getId(), idRol);
            }
        }
    }

    @Transactional
    public Usuario asignarRolPorUsername(String username, String nombreRol) {
        Usuario usuario = getUsuarioPorUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));
        if (usuarioRepository.esPerfilCliente(usuario.getTipoUsuario())) {
            if (!"CLIENT".equalsIgnoreCase(nombreRol.trim())) {
                throw new IllegalArgumentException("El perfil CLIENTE solo puede tener el rol CLIENT.");
            }
            actualizarRoles(usuario, java.util.Set.of("CLIENT"));
            return getUsuarioPorUsername(username).orElseThrow();
        }
        Long idRol = usuarioRepository.findRoleIdByName(nombreRol)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + nombreRol));
        boolean yaAsignado = usuarioRepository.findRolesByUserId(usuario.getId()).stream()
                .anyMatch(rol -> rol.getId().equals(idRol));
        if (!yaAsignado) {
            usuarioRepository.assignRoleToUser(usuario.getId(), idRol);
        }
        return getUsuarioPorUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));
    }

    @Transactional
    public void delete(Long idUsuario) {
        if (!usuarioRepository.existsById(idUsuario)) {
            throw new IllegalArgumentException("Usuario con ID " + idUsuario + " no existe.");
        }
        usuarioRepository.deleteById(idUsuario);
    }

    @Transactional(readOnly = true)
    public List<String> getRolesNombres() {
        return usuarioRepository.findAllRoles().stream().map(rol -> rol.getNombre()).toList();
    }

    @Transactional
    public Usuario eliminarRol(String username, Long idRol) {
        Usuario usuario = getUsuarioPorUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));
        if (usuarioRepository.esPerfilCliente(usuario.getTipoUsuario())
                && usuarioRepository.findRoleIdByName("CLIENT").filter(idRol::equals).isPresent()) {
            throw new IllegalArgumentException("El perfil CLIENTE debe conservar el rol CLIENT.");
        }
        usuarioRepository.removeRoleFromUser(usuario.getId(), idRol);
        return getUsuarioPorUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));
    }

}
