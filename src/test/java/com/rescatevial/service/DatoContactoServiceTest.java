package com.rescatevial.service;

import com.rescatevial.domain.*;
import com.rescatevial.repository.DatoContactoRepository;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DatoContactoServiceTest {
    private final DatoContactoRepository repo = mock(DatoContactoRepository.class);
    private final UsuarioService usuarios = mock(UsuarioService.class);
    private final EstadoService estados = mock(EstadoService.class);
    private final DatoContactoService service = new DatoContactoService(repo, usuarios, estados);

    private void preparar() {
        when(usuarios.getUsuarioPorId(9L)).thenReturn(Optional.of(new Usuario()));
        Estado activo = new Estado(); activo.setId(1L); activo.setNombreEstado("ACTIVO");
        when(estados.listar()).thenReturn(List.of(activo));
        when(repo.provincias()).thenReturn(List.of(opcion(1L, null)));
        when(repo.cantones()).thenReturn(List.of(opcion(2L, 1L)));
        when(repo.distritos()).thenReturn(List.of(opcion(3L, 2L)));
    }
    private OpcionUbicacion opcion(Long id, Long padre) {
        OpcionUbicacion o = new OpcionUbicacion();
        o.setId(id); o.setIdPadre(padre); o.setIdEstado(1L); return o;
    }
    private DatoContacto direccion(Long estado, String senas) {
        DatoContacto d = new DatoContacto();
        d.setIdUsuario(9L); d.setIdProvincia(1L); d.setIdCanton(2L);
        d.setIdDistrito(3L); d.setIdEstado(estado); d.setOtrasSenas(senas); return d;
    }
    @Test
    void aceptaSenasVaciasDevueltasComoNull() {
        preparar();
        when(repo.direcciones(9L)).thenReturn(List.of())
                .thenReturn(List.of(direccion(1L, null)));
        DatoContacto nueva = direccion(null, "   ");
        assertDoesNotThrow(() -> service.crearDireccionDesdeIncidente(9L, nueva));
        assertNull(nueva.getOtrasSenas());
        verify(repo).insertarDireccion(nueva);
    }
    @Test
    void reactivaDireccionExistenteSinDuplicarClave() {
        preparar();
        when(repo.direcciones(9L)).thenReturn(List.of(direccion(2L, "Antes")))
                .thenReturn(List.of(direccion(1L, "Ahora")));
        DatoContacto nueva = direccion(null, "Ahora");
        service.crearDireccionDesdeIncidente(9L, nueva);
        verify(repo).modificarDireccion(nueva);
        verify(repo, never()).insertarDireccion(any());
    }
    @Test
    void sigueDetectandoInsercionFallida() {
        preparar();
        when(repo.direcciones(9L)).thenReturn(List.of());
        assertThrows(IllegalStateException.class,
                () -> service.crearDireccionDesdeIncidente(9L, direccion(null, "")));
    }
}
