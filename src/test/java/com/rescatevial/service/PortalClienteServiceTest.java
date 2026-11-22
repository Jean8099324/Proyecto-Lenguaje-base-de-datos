package com.rescatevial.service;
import com.rescatevial.domain.*;
import com.rescatevial.repository.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PortalClienteServiceTest {
    UsuarioService usuarios=mock(UsuarioService.class);
    VehiculoService vehiculos=mock(VehiculoService.class);
    IncidenteService incidentes=mock(IncidenteService.class);
    DatoContactoService datos=mock(DatoContactoService.class);
    EstadoService estados=mock(EstadoService.class);
    PortalClienteService portal=new PortalClienteService(usuarios,vehiculos,incidentes,datos,estados);
    UsernamePasswordAuthenticationToken auth=new UsernamePasswordAuthenticationToken("ana","",List.of(new SimpleGrantedAuthority("ROLE_CLIENT")));
    @BeforeEach void setup() {
        Usuario u=new Usuario(); u.setId(8L);u.setUsername("ana");u.setNombre("Ana");u.setPrimerApellido("Mora");
        when(usuarios.getUsuarioPorUsername("ana")).thenReturn(Optional.of(u));
    }
    @Test void rechazaGuest() {
        var guest=new UsernamePasswordAuthenticationToken("ana","",List.of(new SimpleGrantedAuthority("ROLE_GUEST")));
        assertThrows(AccessDeniedException.class,()->portal.actual(guest));
    }
    @Test void adminEnPortalSigueUsandoSuIdentidadYValidaPropiedad() {
        var admin=new UsernamePasswordAuthenticationToken("ana","",List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        assertEquals(8L,portal.actual(admin).getId());
        Incidente propio=new Incidente();propio.setIdUsuario(8L);
        Incidente ajeno=new Incidente();ajeno.setIdUsuario(99L);
        when(incidentes.buscar(3L)).thenReturn(Optional.of(propio));
        when(incidentes.buscar(4L)).thenReturn(Optional.of(ajeno));
        assertSame(propio,portal.incidentePropio(admin,3L));
        assertThrows(AccessDeniedException.class,()->portal.incidentePropio(admin,4L));
    }
    @Test void rechazaVehiculoAjenoAntesDeCrearIncidente() {
        Vehiculo v=new Vehiculo();v.setId(90L);v.setIdUsuario(99L);
        when(vehiculos.buscar(90L)).thenReturn(Optional.of(v));
        assertThrows(AccessDeniedException.class,()->portal.reportar(auth,90L,1L,1L,1L,1L,"","Auxilio","Pedro","Soto","","88888888",""));
        verify(incidentes,never()).guardar(any());
    }
    @Test void rechazaIncidenteAjenoAunqueVehiculoSeaPropio() {
        Incidente i=new Incidente();i.setId(90L);i.setIdUsuario(99L);
        when(incidentes.buscar(90L)).thenReturn(Optional.of(i));
        assertThrows(AccessDeniedException.class,()->portal.incidentePropio(auth,90L));
    }
    @Test void reemplazaIdentidadEnContactoYEstado() {
        DatoContacto d=new DatoContacto();d.setIdUsuario(99L);d.setValor("88888888");
        portal.guardarDato(auth,"telefono",d,true);
        assertEquals(8L,d.getIdUsuario());verify(datos).guardar("telefono",d,true);
        d.setIdUsuario(99L);portal.estadoDato(auth,"telefono",d,false);
        assertEquals(8L,d.getIdUsuario());verify(datos).cambiarEstado("telefono",d,false);
    }
    @Test void reporteUsaClienteFechaEstadoYVehiculoValidados() {
        Vehiculo v=new Vehiculo();v.setId(90L);v.setIdUsuario(8L);v.setIdEstado(1L);
        when(vehiculos.buscar(90L)).thenReturn(Optional.of(v));when(vehiculos.estadoActivo()).thenReturn(1L);
        OpcionIncidente tipo=new OpcionIncidente();tipo.setId(2L);
        OpcionIncidente prioridad=new OpcionIncidente();prioridad.setId(3L);prioridad.setEtiqueta("MEDIA");
        when(incidentes.tipos()).thenReturn(List.of(tipo));when(incidentes.prioridades()).thenReturn(List.of(prioridad));
        Estado e=new Estado();e.setId(4L);e.setNombreEstado("REPORTADO");when(estados.listar()).thenReturn(List.of(e));
        OpcionUbicacion p=new OpcionUbicacion();p.setId(1L);
        OpcionUbicacion c=new OpcionUbicacion();c.setId(2L);c.setIdPadre(1L);
        OpcionUbicacion d=new OpcionUbicacion();d.setId(3L);d.setIdPadre(2L);
        when(datos.provincias()).thenReturn(List.of(p));when(datos.cantones()).thenReturn(List.of(c));when(datos.distritos()).thenReturn(List.of(d));
        portal.reportar(auth,90L,2L,1L,2L,3L,"Frente al parque","Falla mecánica"," Pedro ","Soto","Rojas","88888888","ana@example.com");
        var captor=org.mockito.ArgumentCaptor.forClass(Incidente.class);verify(incidentes).guardar(captor.capture());
        assertEquals(8L,captor.getValue().getIdCliente());assertEquals(4L,captor.getValue().getIdEstado());
        assertEquals("Pedro",captor.getValue().getNombreReportante());assertEquals("Soto",captor.getValue().getPrimerApellidoReportante());assertEquals("Rojas",captor.getValue().getSegundoApellidoReportante());
        assertEquals(3L,captor.getValue().getIdPrioridad());assertNull(captor.getValue().getId());
    }
    @Test void passwordActualIncorrectaNoEscribe() {
        UsuarioRepository repo=mock(UsuarioRepository.class);PasswordEncoder encoder=mock(PasswordEncoder.class);
        Usuario u=new Usuario();u.setPassword("hash");when(repo.findByUsername("ana")).thenReturn(Optional.of(u));
        var service=new UsuarioService(repo,encoder,mock(DatoContactoRepository.class),estados);
        assertThrows(IllegalArgumentException.class,()->service.cambiarPasswordPropia("ana","incorrecta","nueva1234","nueva1234"));
        verify(repo,never()).updateUsingProcedure(any(),any());
    }

    @Test void passwordSeCifraYSeConfirmaConElProcedimiento() {
        UsuarioRepository repo=mock(UsuarioRepository.class);PasswordEncoder encoder=mock(PasswordEncoder.class);
        Usuario original=new Usuario();original.setPassword("hashAnterior");
        Usuario actualizado=new Usuario();actualizado.setPassword("hashNuevo");
        when(repo.findByUsername("ana")).thenAnswer(new org.mockito.stubbing.Answer<Optional<Usuario>>() {
            private final java.util.Iterator<Optional<Usuario>> respuestas =
                    List.of(Optional.of(original), Optional.of(actualizado)).iterator();
            @Override public Optional<Usuario> answer(org.mockito.invocation.InvocationOnMock invocation) {
                return respuestas.next();
            }
        });
        when(encoder.matches("actual123", "hashAnterior")).thenReturn(true);
        when(encoder.encode("nueva1234")).thenReturn("hashNuevo");
        when(encoder.matches("nueva1234", "hashNuevo")).thenReturn(true);
        var service=new UsuarioService(repo,encoder,mock(DatoContactoRepository.class),estados);
        service.cambiarPasswordPropia("ana","actual123","nueva1234","nueva1234");
        verify(repo).updateUsingProcedure(original,"hashNuevo");
    }
    @Test void listasNuncaDevuelvenRecursosAjenos() {
        IncidenteRepository repo=mock(IncidenteRepository.class);
        Incidente propio=new Incidente();propio.setIdUsuario(8L);
        Incidente ajeno=new Incidente();ajeno.setIdUsuario(99L);
        when(repo.findAll()).thenReturn(List.of(propio,ajeno));
        var service=new IncidenteService(repo,estados,vehiculos,mock(ClienteService.class));
        assertEquals(List.of(propio),service.listarPorCliente(8L));
        VehiculoRepository vr=mock(VehiculoRepository.class);
        Vehiculo mio=new Vehiculo();mio.setIdUsuario(8L);
        Vehiculo otro=new Vehiculo();otro.setIdUsuario(99L);
        when(vr.listar()).thenReturn(List.of(mio,otro));
        var vs=new VehiculoService(vr,usuarios,estados);
        assertEquals(List.of(mio),vs.listarPorCliente(8L));
    }
    @Test void loginAdminConservaDashboardInclusoConClient() throws Exception {
        var admin=new UsernamePasswordAuthenticationToken("admin","",List.of(new SimpleGrantedAuthority("ROLE_ADMIN"),new SimpleGrantedAuthority("ROLE_CLIENT")));
        var response=new org.springframework.mock.web.MockHttpServletResponse();
        new com.rescatevial.SecurityConfig().authenticationSuccessHandler().onAuthenticationSuccess(new org.springframework.mock.web.MockHttpServletRequest(),response,admin);
        assertEquals("/dashboard",response.getRedirectedUrl());
    }

    @Test void agregarVehiculoSiempreCreaUnoNuevoParaElUsuarioAutenticado() {
        Vehiculo manipulado=new Vehiculo();
        manipulado.setId(42L); manipulado.setIdUsuario(99L); manipulado.setIdEstado(2L);
        manipulado.setPlaca("ABC123"); manipulado.setIdMarca(1L); manipulado.setIdModelo(2L); manipulado.setIdAnio(3L);
        portal.agregarVehiculo(auth,manipulado);
        var captor=org.mockito.ArgumentCaptor.forClass(Vehiculo.class);verify(vehiculos).guardar(captor.capture());
        Vehiculo nuevo=captor.getValue();
        assertNull(nuevo.getId());assertNull(nuevo.getIdEstado());assertEquals(8L,nuevo.getIdUsuario());
        assertEquals("ABC123",nuevo.getPlaca());assertEquals(2L,nuevo.getIdModelo());
    }

    @Test void reporteRechazaContactoVacioYNombreDemasiadoLargo() {
        for (String nombre : Arrays.asList(null, "", "   ", "a".repeat(101))) {
            assertThrows(IllegalArgumentException.class, () -> portal.reportar(auth,90L,2L,1L,2L,3L,"","Auxilio",nombre,"","","88888888",""));
        }
        verify(incidentes,never()).guardar(any());
    }
}
