package com.rescatevial.service;

import com.rescatevial.domain.Vehiculo;
import com.rescatevial.repository.VehiculoRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VehiculoServiceTest {
    private final VehiculoRepository repository = mock(VehiculoRepository.class);
    private final UsuarioService usuarios = mock(UsuarioService.class);
    private final EstadoService estados = mock(EstadoService.class);
    private final VehiculoService service = new VehiculoService(repository, usuarios, estados);

    private Vehiculo vehiculo(long id, String placa) {
        Vehiculo v = new Vehiculo();
        v.setId(id);
        v.setPlaca(placa);
        v.setIdUsuario(9L);
        v.setCodigoChasis("CHASIS1");
        return v;
    }

    @Test
    void actualizaPlacaNormalizadaConservandoVehiculo() {
        Vehiculo original = vehiculo(1L, "ABC123");
        when(repository.buscar(1L)).thenReturn(Optional.of(original))
                .thenReturn(Optional.of(vehiculo(1L, "XYZ789")));
        when(repository.listar()).thenReturn(List.of(original));
        service.actualizarPlaca(1L, " xyz789 ");
        verify(repository).modificar(argThat(v -> v.getId().equals(1L)
                && v.getPlaca().equals("XYZ789") && v.getIdUsuario().equals(9L)
                && v.getCodigoChasis().equals("CHASIS1")));
    }

    @Test
    void rechazaPlacaDeOtroVehiculo() {
        when(repository.buscar(1L)).thenReturn(Optional.of(vehiculo(1L, "ABC123")));
        when(repository.listar()).thenReturn(List.of(vehiculo(2L, "XYZ789")));
        assertThrows(IllegalArgumentException.class, () -> service.actualizarPlaca(1L, "xyz789"));
        verify(repository, never()).modificar(any());
    }

    @Test
    void detectaCuandoOracleNoGuardaElCambio() {
        when(repository.buscar(1L)).thenReturn(Optional.of(vehiculo(1L, "ABC123")))
                .thenReturn(Optional.of(vehiculo(1L, "ABC123")));
        when(repository.listar()).thenReturn(List.of());
        assertThrows(IllegalStateException.class, () -> service.actualizarPlaca(1L, "XYZ789"));
    }

    private Vehiculo nuevoValido(String placa) {
        var estado = new com.rescatevial.domain.Estado(); estado.setId(1L); estado.setNombreEstado("ACTIVO");
        when(estados.listar()).thenReturn(List.of(estado));
        var propietario = new com.rescatevial.domain.Usuario(); propietario.setId(9L);
        when(usuarios.getUsuarios(false)).thenReturn(List.of(propietario));
        var marca = new com.rescatevial.domain.OpcionVehiculo(); marca.setId(10L); marca.setEstadoId(1L);
        var modelo = new com.rescatevial.domain.OpcionVehiculo(); modelo.setId(20L); modelo.setEstadoId(1L); modelo.setPadreId(10L);
        var anio = new com.rescatevial.domain.OpcionVehiculo(); anio.setId(30L); anio.setEstadoId(1L); anio.setPadreId(20L);
        when(repository.marcas()).thenReturn(List.of(marca));when(repository.modelos()).thenReturn(List.of(modelo));when(repository.anios()).thenReturn(List.of(anio));
        Vehiculo v = vehiculo(0L,placa); v.setId(null);v.setIdMarca(10L);v.setIdModelo(20L);v.setIdAnio(30L);v.setCodigoChasis(null);
        return v;
    }
    @Test void noPermitePlacaEquivalenteDeOtroPropietarioInclusoInactiva() {
        Vehiculo nuevo=nuevoValido("abc123");
        Vehiculo existente=vehiculo(5L," AbC-123 ");existente.setIdUsuario(99L);existente.setIdEstado(2L);
        when(repository.listar()).thenReturn(List.of(existente));
        assertThrows(IllegalArgumentException.class,()->service.guardar(nuevo));
        verify(repository,never()).insertar(any());
    }
    @Test void normalizaPlacaYConfirmaRegistroNuevoConTodosSusDatos() {
        Vehiculo nuevo=nuevoValido(" abc-123 ");
        Vehiculo creado=nuevoValido("ABC123");creado.setId(7L);
        when(repository.listar()).thenReturn(List.of(),List.of(creado));
        service.guardar(nuevo);
        verify(repository).insertar(argThat(v->"ABC123".equals(v.getPlaca()) && Long.valueOf(9).equals(v.getIdUsuario())));
    }
    @Test void noMuestraExitoSiOracleSoloDevuelveRegistroDeOtroCliente() {
        Vehiculo nuevo=nuevoValido("ABC123");
        Vehiculo ajeno=nuevoValido("ABC123");ajeno.setId(7L);ajeno.setIdUsuario(99L);
        when(repository.listar()).thenReturn(List.of(),List.of(ajeno));
        assertThrows(IllegalStateException.class,()->service.guardar(nuevo));
    }
    @Test void traduceDuplicadoConcurrenteDeOracleEnErrorDeValidacion() {
        Vehiculo nuevo=nuevoValido("ABC123");
        when(repository.listar()).thenReturn(List.of());
        doThrow(new org.springframework.dao.DuplicateKeyException("ORA-00001")).when(repository).insertar(any());
        var error=assertThrows(IllegalArgumentException.class,()->service.guardar(nuevo));
        assertEquals("Ya existe un vehículo con esa placa o chasis.",error.getMessage());
    }
    @Test void noPermiteEditarOtraPlacaEquivalente() {
        Vehiculo editado=nuevoValido("abc123");editado.setId(1L);
        when(repository.listar()).thenReturn(List.of(vehiculo(2L,"ABC-123")));
        assertThrows(IllegalArgumentException.class,()->service.guardar(editado));
        verify(repository,never()).modificar(any());
    }
}
