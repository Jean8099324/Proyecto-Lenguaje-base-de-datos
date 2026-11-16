package com.rescatevial.service;

import com.rescatevial.domain.Usuario;
import com.rescatevial.domain.Rol;
import com.rescatevial.repository.UsuarioRepository;
import com.rescatevial.repository.DatoContactoRepository;
import com.rescatevial.SecurityConfig;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClienteRolesTest {
    private final UsuarioRepository repository = mock(UsuarioRepository.class);
    private Usuario cliente() {
        Usuario usuario = new Usuario();
        usuario.setId(8L);
        usuario.setTipoUsuario(5L);
        usuario.setNombre("Ana");
        usuario.setUsername("ana");
        usuario.setPassword("hash");
        Rol admin = new Rol(); admin.setId(1L); admin.setNombre("ADMIN");
        usuario.setRoles(Set.of(admin));
        when(repository.esPerfilCliente(5L)).thenReturn(true);
        when(repository.findByUsername("ana")).thenReturn(Optional.of(usuario));
        when(repository.findByUsernameAndActivoTrue("ana")).thenReturn(Optional.of(usuario));
        return usuario;
    }
    private UsuarioService service() {
        return new UsuarioService(repository, mock(PasswordEncoder.class), mock(DatoContactoRepository.class), mock(EstadoService.class));
    }
    @Test void clienteConRolAntiguoAdminSoloObtieneClientYEntraAlPortal() throws Exception {
        cliente();
        var details = new UsuarioDetailsService(repository, mock(HttpSession.class)).loadUserByUsername("ana");
        assertEquals(Set.of("ROLE_CLIENT"), details.getAuthorities().stream().map(a -> a.getAuthority()).collect(java.util.stream.Collectors.toSet()));
        var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(details, "", details.getAuthorities());
        var response = new MockHttpServletResponse();
        new SecurityConfig().authenticationSuccessHandler().onAuthenticationSuccess(new MockHttpServletRequest(), response, auth);
        assertEquals("/cliente/inicio", response.getRedirectedUrl());
    }
    @Test void noPermiteAsignarAdminACliente() {
        cliente();
        assertThrows(IllegalArgumentException.class, () -> service().asignarRolPorUsername("ana", "ADMIN"));
        verify(repository, never()).assignRoleToUser(anyLong(), anyLong());
    }
    @Test void noPermiteQuitarClientACliente() {
        cliente();
        when(repository.findRoleIdByName("CLIENT")).thenReturn(Optional.of(3L));
        assertThrows(IllegalArgumentException.class, () -> service().eliminarRol("ana", 3L));
        verify(repository, never()).removeRoleFromUser(anyLong(), anyLong());
    }
    @Test void guardarClienteSustituyeRolesSuperioresPorClient() {
        Usuario usuario = cliente();
        when(repository.findById(8L)).thenReturn(Optional.of(usuario));
        when(repository.findRoleIdByName("CLIENT")).thenReturn(Optional.of(3L));
        usuario.setPassword("");
        usuario.setRolesNombresSeleccionados(Set.of("ADMIN", "GUEST"));
        service().save(usuario);
        verify(repository).removeRoleFromUser(8L, 1L);
        verify(repository).assignRoleToUser(8L, 3L);
        assertEquals(Set.of("CLIENT"), usuario.getRolesNombresSeleccionados());
    }
    @Test void administradorConservaSusPermisos() {
        cliente();
        when(repository.esPerfilCliente(5L)).thenReturn(false);
        var details = new UsuarioDetailsService(repository, mock(HttpSession.class)).loadUserByUsername("ana");
        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }
}
