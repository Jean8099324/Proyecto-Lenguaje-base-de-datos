package com.rescatevial.service;
import com.rescatevial.controller.IncidenteController;
import com.rescatevial.domain.*;
import com.rescatevial.repository.IncidenteRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import static org.mockito.Mockito.*;
class IncidenteUsuarioTest {
    @Test void noAsociaElUsuarioDeSesionAlCrearIncidente() {
        IncidenteService servicio = mock(IncidenteService.class);
        UsuarioService usuarios = mock(UsuarioService.class);
        IncidenteController controller = new IncidenteController(servicio, mock(EstadoService.class),
                usuarios, mock(ContactoService.class), mock(DatoContactoService.class));
        Incidente i = new Incidente(); i.setIdUsuario(999L);
        i.setNombreReportante("Contacto"); i.setTelefonoReportante("88888888");
        var auth = new UsernamePasswordAuthenticationToken("operador", "", java.util.List.of());
        controller.guardar(i, new BeanPropertyBindingResult(i,"incidenteForm"), new RedirectAttributesModelMap(), auth);
        verify(servicio).guardar(argThat(d -> d.getIdUsuario() == null));
    }
    @Test void guardaClienteSeleccionadoAlEditar() {
        IncidenteRepository repo = mock(IncidenteRepository.class);
        IncidenteService servicio = new IncidenteService(repo, mock(EstadoService.class), mock(VehiculoService.class), mock(ClienteService.class));
        Incidente original = new Incidente(); original.setIdUsuario(7L);
        when(repo.findById(1L)).thenReturn(Optional.of(original));
        Incidente i = new Incidente(); i.setId(1L); i.setIdCliente(8L);
        servicio.guardar(i);
        verify(repo).update(argThat(d -> Long.valueOf(8).equals(d.getIdUsuario())));
    }
    @Test void permiteCrearIncidenteSinUsuario() {
        IncidenteRepository repo = mock(IncidenteRepository.class);
        IncidenteService servicio = new IncidenteService(repo, mock(EstadoService.class), mock(VehiculoService.class), mock(ClienteService.class));
        Incidente incidente = new Incidente();
        incidente.setIdVehiculo(1L);
        servicio.guardar(incidente);
        verify(repo).insert(argThat(d -> d.getIdUsuario() == null));
    }
}
