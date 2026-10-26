package com.rescatevial.service;

import com.rescatevial.domain.Usuario;
import com.rescatevial.domain.Rol;
import com.rescatevial.repository.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<Usuario> getUsuarios(boolean activo) {
        if (activo) {
            return usuarioRepository.findByActivoTrue();
        }
        return usuarioRepository.findAll();
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
        if (usuario.getId() == null) {// Si el usuario es nuevo
            if (usuario.getPassword() == null || usuario.getPassword().isBlank()) {
                throw new IllegalArgumentException("La contraseña es obligatoria para un usuario nuevo.");
            }
            // Oracle genera el ID; la aplicación solo envía la contraseña cifrada al procedimiento.
            usuarioRepository.insertUsingProcedure(usuario, passwordEncoder.encode(usuario.getPassword()));
            // Permite asignar varios roles desde el formulario; CLIENT es el rol inicial predeterminado.
            var rolesSeleccionados = rolesSeleccionados(usuario.getRolesNombresSeleccionados());
            if (rolesSeleccionados.isEmpty()) rolesSeleccionados = java.util.Set.of("CLIENT");
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

    private static java.util.Set<String> rolesSeleccionados(java.util.Set<String> nombres) {
        if (nombres == null) return java.util.Set.of();
        return nombres.stream().filter(java.util.Objects::nonNull).map(String::trim)
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
        var idsActuales = usuario.getRoles().stream().map(Rol::getId).collect(java.util.stream.Collectors.toSet());
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
        return usuarioRepository.findAllRoles().stream().map(Rol::getNombre).toList();
    }

    @Transactional
    public Usuario eliminarRol(String username, Long idRol) {
        Usuario usuario = getUsuarioPorUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));
        usuarioRepository.removeRoleFromUser(usuario.getId(), idRol);
        return getUsuarioPorUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));
    }

}
