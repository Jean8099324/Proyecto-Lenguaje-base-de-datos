package com.rescatevial.service;

import com.rescatevial.domain.Estado;
import com.rescatevial.domain.Unidad;
import com.rescatevial.repository.UnidadRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UnidadServiceTest {
    private final UnidadRepository repo = mock(UnidadRepository.class);
    private final EstadoService estados = mock(EstadoService.class);
    private final TipoUnidadService tipos = mock(TipoUnidadService.class);
    private final UnidadService service = new UnidadService(repo, estados, tipos);

    private Unidad preparar(String nombreEstado) {
        var estado = new Estado();
        estado.setId(2L);
        estado.setNombreEstado(nombreEstado);
        var unidad = new Unidad();
        unidad.setId(1L);
        unidad.setIdEstado(2L);
        when(estados.listar()).thenReturn(List.of(estado));
        when(tipos.listar()).thenReturn(List.of());
        when(repo.listar()).thenReturn(List.of(unidad));
        return unidad;
    }

    @Test void muestraElIncidenteMasRecienteDeLaUnidad() {
        preparar("ASIGNADA");
        when(repo.historiales()).thenReturn(List.of(
                Map.of("ID_UNIDAD", 1L, "ID_HISTORIAL", 2L, "ID_INCIDENTE", 20L),
                Map.of("ID_UNIDAD", 1L, "ID_HISTORIAL", 1L, "ID_INCIDENTE", 10L),
                Map.of("ID_UNIDAD", 9L, "ID_HISTORIAL", 3L, "ID_INCIDENTE", 30L)));
        assertEquals(20L, service.listar().getFirst().getIncidenteAsignadoId());
    }

    @Test void noMuestraAsignacionesHistoricasDeUnaUnidadDisponible() {
        preparar("DISPONIBLE");
        assertNull(service.listar().getFirst().getIncidenteAsignadoId());
        verify(repo, never()).historiales();
    }

    @Test void noRecuperaUnaAsignacionAntiguaSiLaUltimaYaRegreso() {
        preparar("ASIGNADA");
        when(repo.historiales()).thenReturn(List.of(
                Map.of("ID_UNIDAD", 1L, "ID_HISTORIAL", 1L, "ID_INCIDENTE", 10L),
                Map.of("ID_UNIDAD", 1L, "ID_HISTORIAL", 2L, "ID_INCIDENTE", 20L,
                        "FECHA_REGRESO_UNIDAD", java.sql.Date.valueOf("2026-11-21"))));
        assertNull(service.listar().getFirst().getIncidenteAsignadoId());
    }

    @Test void admiteUnidadAsignadaSinHistorial() {
        preparar("ASIGNADA");
        when(repo.historiales()).thenReturn(List.of());
        assertNull(service.listar().getFirst().getIncidenteAsignadoId());
    }
}
