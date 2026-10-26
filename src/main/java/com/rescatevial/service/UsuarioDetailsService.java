package com.rescatevial.service;

import com.rescatevial.domain.Usuario;
import com.rescatevial.repository.UsuarioRepository;
import java.util.Locale;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service("userDetailsService")
public class UsuarioDetailsService implements UserDetailsService {
    private final UsuarioRepository usuarioRepository;
    private final HttpSession session;

    // Se usa la sesión como en proyecto_final_tienda para compartir datos del usuario con las vistas.
    public UsuarioDetailsService(UsuarioRepository usuarioRepository, HttpSession session) {
        this.usuarioRepository = usuarioRepository;
        this.session = session;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository
                .findByUsernameAndActivoTrue(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado o inactivo."));

        var roles = usuario.getRoles().stream()
                .map(rol -> new SimpleGrantedAuthority("ROLE_" + rol.getNombre().trim().toUpperCase(Locale.ROOT)))
                .collect(java.util.stream.Collectors.toSet());

        // Se prepara el nombre que usarán el saludo y el perfil del dashboard.
        String nombreCompleto = java.util.stream.Stream.of(
                        usuario.getNombre(), usuario.getPrimerApellido(), usuario.getSegundoApellido())
                .filter(nombre -> nombre != null && !nombre.isBlank())
                .collect(java.util.stream.Collectors.joining(" "));

        // Se calculan las iniciales para el avatar mostrado en la barra superior.
        String iniciales = java.util.stream.Stream.of(usuario.getNombre(), usuario.getPrimerApellido())
                .filter(nombre -> nombre != null && !nombre.isBlank())
                .map(nombre -> nombre.substring(0, 1).toUpperCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.joining());
        // Se limpian valores previos y se guardan los datos de la sesión autenticada.
        session.removeAttribute("nombreUsuario");
        session.removeAttribute("usernameUsuario");
        session.removeAttribute("inicialesUsuario");
        session.setAttribute("nombreUsuario", nombreCompleto);
        session.setAttribute("usernameUsuario", usuario.getUsername());
        session.setAttribute("inicialesUsuario", iniciales);

        return new UsuarioPrincipal(usuario.getUsername(), usuario.getPassword(), roles, nombreCompleto);
    }
}
