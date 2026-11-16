package com.rescatevial.service;
import com.rescatevial.controller.*;
import com.rescatevial.domain.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class VehiculoSesionTest {
    private final UsuarioService usuarios = mock(UsuarioService.class);
    private final VehiculoService vehiculos = mock(VehiculoService.class);
    private final IncidenteService incidentes = mock(IncidenteService.class);
    private final UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("operador", "", List.of());
    private void sesion() {
        Usuario u = new Usuario(); u.setId(7L);
        when(usuarios.getUsuarioPorUsername("operador")).thenReturn(Optional.of(u));
    }
    @Test void registroDesdeIncidenteUsaClienteSeleccionado() {
        sesion();
        IncidenteController controller = new IncidenteController(incidentes, mock(EstadoService.class), usuarios,
                mock(ContactoService.class), mock(DatoContactoService.class));
        VehiculoIncidenteForm v = new VehiculoIncidenteForm(); v.setIdUsuario(999L);
        OpcionIncidente saved = new OpcionIncidente(); saved.setId(1L); saved.setEtiqueta("ABC123");
        when(incidentes.registrarVehiculo(any())).thenReturn(saved);
        var result = controller.guardarVehiculo(v, new BeanPropertyBindingResult(v, "vehicle"), auth);
        assertEquals(200, result.getStatusCode().value());
        verify(incidentes).registrarVehiculo(argThat(d -> Long.valueOf(999).equals(d.getIdUsuario())));
    }
    @Test void registroEnVehiculosUsaClienteSeleccionado() {
        sesion();
        Vehiculo v = new Vehiculo(); v.setIdUsuario(999L);
        new VehiculoController(vehiculos, usuarios).guardar(v, new BeanPropertyBindingResult(v, "vehiculoForm"),
                new ExtendedModelMap(), new RedirectAttributesModelMap(), auth);
        verify(vehiculos).guardar(argThat(d -> Long.valueOf(999).equals(d.getIdUsuario())));
    }
    @Test void editarPermiteCorregirPropietario() {
        Vehiculo original = new Vehiculo(); original.setIdUsuario(3L);
        when(vehiculos.buscar(1L)).thenReturn(Optional.of(original));
        Vehiculo v = new Vehiculo(); v.setId(1L); v.setIdUsuario(999L);
        new VehiculoController(vehiculos, usuarios).guardar(v, new BeanPropertyBindingResult(v, "vehiculoForm"),
                new ExtendedModelMap(), new RedirectAttributesModelMap(), auth);
        verify(vehiculos).guardar(argThat(d -> Long.valueOf(999).equals(d.getIdUsuario())));
    }
    @Test void noRegistraSinPropietario() {
        Vehiculo v = new Vehiculo();
        new VehiculoController(vehiculos, usuarios).guardar(v, new BeanPropertyBindingResult(v, "vehiculoForm"),
                new ExtendedModelMap(), new RedirectAttributesModelMap(), null);
        verify(vehiculos, never()).guardar(any());
    }
}
