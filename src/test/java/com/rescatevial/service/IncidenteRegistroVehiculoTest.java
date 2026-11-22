package com.rescatevial.service;

import com.rescatevial.domain.OpcionIncidente;
import com.rescatevial.domain.VehiculoIncidenteForm;
import com.rescatevial.repository.IncidenteRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import jakarta.validation.Validation;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IncidenteRegistroVehiculoTest {
    @Test
    void formularioAceptaIdUsuarioNulo() {
        var form = new VehiculoIncidenteForm();
        form.setPlaca("902754");
        form.setIdMarca(1L);
        form.setIdModelo(2L);
        form.setIdAnio(3L);
        form.setIdEstado(1L);

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(form).isEmpty());
        }
    }

    @Test
    void registraVehiculoSinClientePropietario() {
        var repository = mock(IncidenteRepository.class);
        var clientes = mock(ClienteService.class);
        var service = new IncidenteService(repository, mock(EstadoService.class),
                mock(VehiculoService.class), clientes);
        var form = new VehiculoIncidenteForm();
        form.setPlaca("902754");
        form.setIdMarca(1L);
        form.setIdModelo(2L);
        form.setIdAnio(3L);
        form.setIdEstado(1L);
        form.setIdUsuario(null);
        var saved = new OpcionIncidente();
        saved.setId(7L);
        saved.setEtiqueta("902754");
        when(repository.findVehicleByPlate("902754"))
                .thenAnswer(new org.mockito.stubbing.Answer<Optional<OpcionIncidente>>() {
                    private final java.util.Iterator<Optional<OpcionIncidente>> respuestas =
                            List.of(Optional.<OpcionIncidente>empty(), Optional.of(saved)).iterator();
                    @Override public Optional<OpcionIncidente> answer(
                            org.mockito.invocation.InvocationOnMock invocation) {
                        return respuestas.next();
                    }
                });

        assertSame(saved, service.registrarVehiculo(form));
        verify(repository).insertVehicle(form);
    }
}
