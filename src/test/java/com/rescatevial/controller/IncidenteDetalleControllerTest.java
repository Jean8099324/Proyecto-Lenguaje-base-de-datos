package com.rescatevial.controller;

import com.rescatevial.domain.DatoContacto;
import com.rescatevial.domain.Incidente;
import com.rescatevial.domain.Usuario;
import com.rescatevial.domain.VehiculoDetalleIncidente;
import com.rescatevial.service.ContactoService;
import com.rescatevial.service.DatoContactoService;
import com.rescatevial.service.EstadoService;
import com.rescatevial.service.IncidenteService;
import com.rescatevial.service.UsuarioService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IncidenteDetalleControllerTest {

    @Test
    void usaElPropietarioDelVehiculoCuandoElIncidenteNoTieneCliente() {
        IncidenteService incidentes = mock(IncidenteService.class);
        DatoContactoService datos = mock(DatoContactoService.class);
        UsuarioService usuarios = mock(UsuarioService.class);
        Incidente incidente = new Incidente();
        incidente.setId(2L);
        incidente.setIdVehiculo(8L);
        VehiculoDetalleIncidente vehiculo = new VehiculoDetalleIncidente();
        vehiculo.setIdCliente(20L);
        when(incidentes.buscarDetalle(2L)).thenReturn(Optional.of(incidente));
        when(incidentes.vehiculoDetalle(8L)).thenReturn(Optional.of(vehiculo));
        when(usuarios.getUsuarioPorId(20L)).thenReturn(Optional.of(new Usuario()));
        when(datos.correos(20L)).thenReturn(List.of(dato("cliente@ejemplo.com", "ACTIVO")));
        when(datos.telefonos(20L)).thenReturn(List.of(dato("88888888", "ACTIVO")));
        IncidenteController controller = new IncidenteController(incidentes, mock(EstadoService.class),
                usuarios, mock(ContactoService.class), datos);
        ExtendedModelMap modelo = new ExtendedModelMap();

        assertEquals("incidentes/detalle", controller.detalle(2L, modelo, new RedirectAttributesModelMap()));
        assertNotNull(modelo.get("clienteIncidente"));
        assertEquals(List.of("cliente@ejemplo.com"), contactos(modelo, "correosCliente").stream()
                .map(DatoContacto::getValor).toList());
        assertEquals(List.of("88888888"), contactos(modelo, "telefonosCliente").stream()
                .map(DatoContacto::getValor).toList());
    }

    @Test
    void cargaSoloTelefonosYCorreosActivosDelCliente() {
        IncidenteService incidentes = mock(IncidenteService.class);
        DatoContactoService datos = mock(DatoContactoService.class);
        Incidente incidente = new Incidente();
        incidente.setId(1L);
        incidente.setIdVehiculo(8L);
        incidente.setIdUsuario(20L);
        VehiculoDetalleIncidente vehiculo = new VehiculoDetalleIncidente();
        vehiculo.setIdCliente(20L);
        when(incidentes.buscarDetalle(1L)).thenReturn(Optional.of(incidente));
        when(incidentes.vehiculoDetalle(8L)).thenReturn(Optional.of(vehiculo));
        when(datos.correos(20L)).thenReturn(List.of(dato("uno@ejemplo.com", "ACTIVO"), dato("viejo@ejemplo.com", "INACTIVO")));
        when(datos.telefonos(20L)).thenReturn(List.of(dato("88888888", "ACTIVO"), dato("77777777", "ACTIVO")));
        IncidenteController controller = new IncidenteController(incidentes, mock(EstadoService.class),
                mock(UsuarioService.class), mock(ContactoService.class), datos);
        ExtendedModelMap modelo = new ExtendedModelMap();

        assertEquals("incidentes/detalle", controller.detalle(1L, modelo, new RedirectAttributesModelMap()));
        List<DatoContacto> correos = contactos(modelo, "correosCliente");
        List<DatoContacto> telefonos = contactos(modelo, "telefonosCliente");
        assertEquals(List.of("uno@ejemplo.com"), correos.stream().map(contacto -> contacto.getValor()).toList());
        assertEquals(List.of("88888888", "77777777"),
                telefonos.stream().map(contacto -> contacto.getValor()).toList());
    }

    private static List<DatoContacto> contactos(ExtendedModelMap modelo, String atributo) {
        Object valor = modelo.get(atributo);
        assertInstanceOf(List.class, valor);
        return ((List<?>) valor).stream()
                .map(elemento -> assertInstanceOf(DatoContacto.class, elemento))
                .toList();
    }

    private static DatoContacto dato(String valor, String estado) {
        DatoContacto dato = new DatoContacto();
        dato.setValor(valor);
        dato.setNombreEstado(estado);
        return dato;
    }
}
