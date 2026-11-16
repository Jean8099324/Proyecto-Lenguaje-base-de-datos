package com.rescatevial.service;
import com.rescatevial.domain.*;
import com.rescatevial.repository.IncidenteRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class IncidenteContactoTest {
    private final IncidenteRepository repo = mock(IncidenteRepository.class);
    private final IncidenteService service = new IncidenteService(repo, mock(EstadoService.class), mock(VehiculoService.class), mock(ClienteService.class));
    @Test void creaContactoParaIncidenteSinAsociacion() {
        when(repo.findContactsByIncident(1L)).thenReturn(List.of());
        Incidente i = new Incidente(); i.setId(1L); i.setNombreContactoIncidente("Sofia");
        i.setPrimerApellidoContactoIncidente("Prueba"); i.setTelefonoContactoIncidente("88888888");
        service.crearContactoDesdeIncidente(i);
        verify(repo).insertReporter(argThat(d -> d.getId().equals(1L)
                && "Sofia".equals(d.getNombreReportante())
                && "Prueba".equals(d.getPrimerApellidoReportante())
                && "88888888".equals(d.getTelefonoReportante())));
    }
    @Test void impideDuplicarContactoAlReenviarFormularioDesactualizado() {
        when(repo.findContactsByIncident(1L)).thenReturn(List.of(new ContactoIncidente()));
        Incidente i = new Incidente(); i.setId(1L);
        assertThrows(IllegalArgumentException.class, () -> service.crearContactoDesdeIncidente(i));
        verify(repo, never()).insertReporter(any());
    }
}
