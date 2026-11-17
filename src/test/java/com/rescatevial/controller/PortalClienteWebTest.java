package com.rescatevial.controller;
import com.rescatevial.domain.*;
import com.rescatevial.service.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.csrf.CsrfToken;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"logging.level.root=ERROR","logging.level.org.springframework=ERROR"})
class PortalClienteWebTest {
    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy security;
    @MockitoBean UsuarioService usuarios;
    @MockitoBean DatoContactoService datos;
    @MockitoBean VehiculoService vehiculos;
    @MockitoBean IncidenteService incidentes;
    @MockitoBean EstadoService estados;
    MockMvc mvc;
    MockHttpSession session(String role) {
        var s=new MockHttpSession();s.setAttribute("SPRING_SECURITY_CONTEXT",new SecurityContextImpl(
                new UsernamePasswordAuthenticationToken("ana","",List.of(new SimpleGrantedAuthority("ROLE_"+role)))));
        s.setAttribute("nombreUsuario","Ana Mora");s.setAttribute("usernameUsuario","ana");return s;
    }
    @BeforeEach void setup() {
        mvc=MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
        Usuario u=new Usuario();u.setId(8L);u.setUsername("ana");u.setNombre("Ana");u.setPrimerApellido("Mora");
        when(usuarios.getUsuarioPorUsername("ana")).thenReturn(Optional.of(u));
    }
    @Test void renderizaPortalConTodosLosDatosYFormularios() throws Exception {
        DatoContacto correo=new DatoContacto();correo.setValor("ana@example.com");correo.setNombreEstado("ACTIVO");
        DatoContacto otro=new DatoContacto();otro.setValor("otro@example.com");otro.setNombreEstado("ACTIVO");
        when(datos.correos(8L)).thenReturn(List.of(correo,otro));
        DatoContacto tel=new DatoContacto();tel.setValor("88888888");tel.setNombreEstado("ACTIVO");when(datos.telefonos(8L)).thenReturn(List.of(tel));
        DatoContacto dir=new DatoContacto();dir.setIdProvincia(1L);dir.setIdCanton(1L);dir.setIdDistrito(1L);dir.setNombreProvincia("San José");dir.setNombreCanton("Central");dir.setNombreDistrito("Carmen");dir.setNombreEstado("ACTIVO");when(datos.direcciones(8L)).thenReturn(List.of(dir));
        Vehiculo v=new Vehiculo();v.setId(4L);v.setPlaca("ABC123");when(vehiculos.listarPorCliente(8L)).thenReturn(List.of(v));
        Incidente i=new Incidente();i.setId(2L);i.setEstadoEtiqueta("REPORTADO");when(incidentes.listarPorCliente(8L)).thenReturn(List.of(i));
        String html=mvc.perform(get("/cliente/inicio").session(session("CLIENT"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        for(String texto:List.of("ABC123","REPORTADO","/logout","_csrf","Nombre del contacto","name=\"nombreContacto\"","name=\"primerApellidoContacto\"","name=\"segundoApellidoContacto\"","son obligatorios","Correo de contacto (opcional)","Otras señas")) assertTrue(html.contains(texto),texto);
        String cuenta=mvc.perform(get("/cliente/cuenta").session(session("CLIENT"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        for(String texto:List.of("ana@example.com","otro@example.com","88888888","Mi información","Mis datos de contacto","Direcciones registradas","/cliente/password","/cliente/perfil","Cambiar mi contraseña")) assertTrue(cuenta.contains(texto),texto);
        assertFalse(html.contains("href=\"/usuarios"));
    }
    @Test void clienteNoAccedeARutasAdministrativas() throws Exception {
        for(String ruta:List.of("/dashboard","/usuarios/listado","/usuarios/roles","/clientes","/personal","/unidades","/despacho","/reparaciones/listado","/repuestos/listado","/facturas/listado","/configuracion","/incidentes/listado","/vehiculos","/incidentes/clientes","/datos-contacto"))
            mvc.perform(get(ruta).session(session("CLIENT"))).andExpect(status().isForbidden()).andExpect(forwardedUrl("/error/403"));
    }
    @Test void guestYAnonimoNoEntran() throws Exception {
        mvc.perform(get("/cliente/inicio").session(session("GUEST"))).andExpect(status().isForbidden());
        mvc.perform(get("/cliente/inicio")).andExpect(status().is3xxRedirection());
    }
    @Test void adminConservaMenuDeRoles() throws Exception {
        mvc.perform(get("/usuarios/roles").session(session("ADMIN"))).andExpect(status().isOk());
    }
    @Test void errorAmigableYDetalleAjenoDevuelven403() throws Exception {
        var s=session("CLIENT");
        String html=mvc.perform(get("/error/403").session(s)).andExpect(status().isForbidden()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(html.contains("Volver a mi portal"));assertFalse(html.contains("Whitelabel"));
        Incidente ajeno=new Incidente();ajeno.setIdUsuario(99L);when(incidentes.buscar(123L)).thenReturn(Optional.of(ajeno));
        mvc.perform(get("/cliente/incidentes/123").session(s)).andExpect(status().isForbidden());
    }
    @Test void postAdministrativoRechazadoRenderiza403SinError405() throws Exception {
        var s=session("CLIENT");
        var inicio=mvc.perform(get("/cliente/inicio").session(s)).andReturn();
        CsrfToken csrf=(CsrfToken)inicio.getRequest().getAttribute(CsrfToken.class.getName());
        mvc.perform(post("/vehiculos/guardar").session(s).param(csrf.getParameterName(),csrf.getToken()))
                .andExpect(status().isForbidden()).andExpect(forwardedUrl("/error/403"));
        // MockMvc no ejecuta los forwards: se reproduce el despacho que hace el contenedor.
        String html=mvc.perform(post("/error/403").session(s)
                .with(request -> { request.setDispatcherType(jakarta.servlet.DispatcherType.FORWARD); return request; })
                .requestAttr(jakarta.servlet.RequestDispatcher.FORWARD_REQUEST_URI,"/vehiculos/guardar")
                .param(csrf.getParameterName(),csrf.getToken()))
                .andExpect(status().isForbidden()).andExpect(view().name("error/403"))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(html.contains("Volver a mi portal"));assertFalse(html.contains("Whitelabel"));
        verify(vehiculos,never()).guardar(any());
    }
    @Test void noAceptaPostSinCsrfYPropietarioSiempreEsSesion() throws Exception {
        var s=session("CLIENT");
        mvc.perform(post("/cliente/datos/telefono").session(s).param("valor","88888888")).andExpect(status().isForbidden());
        var result=mvc.perform(get("/cliente/inicio").session(s)).andExpect(status().isOk()).andReturn();
        CsrfToken token=(CsrfToken)result.getRequest().getAttribute(CsrfToken.class.getName());
        mvc.perform(post("/cliente/datos/telefono").session(s).param(token.getParameterName(),token.getToken())
            .param("valor","88888888").param("idUsuario","99").param("idEstado","2"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/cliente/inicio"));
        var captor=org.mockito.ArgumentCaptor.forClass(DatoContacto.class);verify(datos).guardar(eq("telefono"),captor.capture(),eq(false));
        assertEquals(8L,captor.getValue().getIdUsuario());assertNull(captor.getValue().getIdEstado());
    }

    @Test void detallePropioRenderizaElMenuYLectorDeEstado() throws Exception {
        Incidente propio=new Incidente();propio.setId(3L);propio.setIdUsuario(8L);propio.setDetalles("Falla propia");propio.setEstadoEtiqueta("REPORTADO");
        when(incidentes.buscar(3L)).thenReturn(Optional.of(propio));when(incidentes.listarPorCliente(8L)).thenReturn(List.of(propio));
        String html=mvc.perform(get("/cliente/incidentes/3").session(session("CLIENT"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(html.contains("Falla propia"));assertTrue(html.contains("REPORTADO"));assertTrue(html.contains("sidebar"));
    }

    @Test void adminYClientePuedenAbrirPortalYDetallePropioSinAccederAlAjeno() throws Exception {
        Incidente propio=new Incidente();propio.setId(3L);propio.setIdUsuario(8L);
        propio.setDetalles("Reporte propio");propio.setEstadoEtiqueta("REPORTADO");
        propio.setFechaReporte(java.time.LocalDate.of(2026,11,16));propio.setHoraReporte("08:30:00");
        Incidente ajeno=new Incidente();ajeno.setId(4L);ajeno.setIdUsuario(99L);
        when(incidentes.buscar(3L)).thenReturn(Optional.of(propio));
        when(incidentes.buscar(4L)).thenReturn(Optional.of(ajeno));
        when(incidentes.listarPorCliente(8L)).thenReturn(List.of(propio));
        for(String rol:List.of("ADMIN","CLIENT")) {
            var s=session(rol);
            mvc.perform(get("/cliente/inicio").session(s)).andExpect(status().isOk());
            String html=mvc.perform(get("/cliente/incidentes/3").session(s)).andExpect(status().isOk())
                    .andExpect(view().name("cliente/incidente")).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(html.contains("Reporte propio"));
            mvc.perform(get("/cliente/incidentes/4").session(s)).andExpect(status().isForbidden());
        }
    }

    @Test void formularioCompartidoPermiteAgregarSinSelectorDePropietario() throws Exception {
        String html=mvc.perform(get("/cliente/vehiculos/nuevo").session(session("CLIENT")))
                .andExpect(status().isOk()).andExpect(view().name("vehiculos/formulario"))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(html.contains("/cliente/vehiculos/guardar"));assertTrue(html.contains("data-plate-check-url=\"/cliente/vehiculos/validar-placa\""));
        assertTrue(html.contains("El veh&iacute;culo quedar&aacute; asociado a tu cuenta."));
        assertTrue(html.contains("idMarca"));assertTrue(html.contains("/js/vehiculos.js"));assertTrue(html.contains("_csrf"));
        assertFalse(html.contains("seleccionar-cliente"));assertFalse(html.contains("name=\"idUsuario\""));
        assertFalse(html.contains("/js/cliente-propietario.js"));
    }
    @Test void clientePuedeConsultarSiLaPlacaYaEstaRegistrada() throws Exception {
        when(vehiculos.placaRegistrada("ABC123", null)).thenReturn(true);
        mvc.perform(get("/cliente/vehiculos/validar-placa").session(session("CLIENT")).param("placa", "ABC123"))
                .andExpect(status().isOk()).andExpect(content().json("{\"duplicada\":true}"));
        verify(vehiculos).placaRegistrada("ABC123", null);
    }
    @Test void altaVehiculoIgnoraPropietarioEstadoEIdManipulados() throws Exception {
        var s=session("CLIENT");
        var resultado=mvc.perform(get("/cliente/vehiculos/nuevo").session(s)).andReturn();
        CsrfToken csrf=(CsrfToken)resultado.getRequest().getAttribute(CsrfToken.class.getName());
        mvc.perform(post("/cliente/vehiculos/guardar").session(s).param(csrf.getParameterName(),csrf.getToken())
                .param("placa","ABC123").param("idMarca","1").param("idModelo","2").param("idAnio","3")
                .param("idUsuario","99").param("id","42").param("idEstado","2"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/cliente/inicio#mis-vehiculos"));
        var captor=org.mockito.ArgumentCaptor.forClass(Vehiculo.class);verify(vehiculos).guardar(captor.capture());
        assertEquals(8L,captor.getValue().getIdUsuario());assertNull(captor.getValue().getId());assertNull(captor.getValue().getIdEstado());
    }
    @Test void altaVehiculoInvalidaConservaFormularioYNoGuarda() throws Exception {
        var s=session("CLIENT");
        var resultado=mvc.perform(get("/cliente/vehiculos/nuevo").session(s)).andReturn();
        CsrfToken csrf=(CsrfToken)resultado.getRequest().getAttribute(CsrfToken.class.getName());
        mvc.perform(post("/cliente/vehiculos/guardar").session(s).param(csrf.getParameterName(),csrf.getToken()).param("placa","ABC123"))
                .andExpect(status().isOk()).andExpect(view().name("vehiculos/formulario"))
                .andExpect(model().attributeHasFieldErrors("vehiculoForm","idMarca","idModelo","idAnio"));
        verify(vehiculos,never()).guardar(any());
    }
    @Test void formularioAdminConservaPropietarioYAccionOriginal() throws Exception {
        String html=mvc.perform(get("/vehiculos/nuevo").session(session("ADMIN"))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(html.contains("/vehiculos/guardar"));assertTrue(html.contains("seleccionar-cliente"));assertTrue(html.contains("/js/cliente-propietario.js"));
    }

    @Test void enviarReporteUsaLaAccionRenderizadaYRegistraAlClienteAutenticado() throws Exception {
        var s=session("CLIENT");
        Vehiculo v=new Vehiculo();v.setId(4L);v.setIdUsuario(8L);v.setIdEstado(1L);v.setPlaca("ABC123");
        when(vehiculos.listarPorCliente(8L)).thenReturn(List.of(v));when(vehiculos.buscar(4L)).thenReturn(Optional.of(v));when(vehiculos.estadoActivo()).thenReturn(1L);
        OpcionIncidente tipo=new OpcionIncidente();tipo.setId(2L);tipo.setEtiqueta("Falla mecánica");when(incidentes.tipos()).thenReturn(List.of(tipo));
        OpcionIncidente prioridad=new OpcionIncidente();prioridad.setId(3L);prioridad.setEtiqueta("MEDIA");when(incidentes.prioridades()).thenReturn(List.of(prioridad));
        Estado estado=new Estado();estado.setId(4L);estado.setNombreEstado("REPORTADO");when(estados.listar()).thenReturn(List.of(estado));
        OpcionUbicacion p=new OpcionUbicacion();p.setId(1L);p.setNombre("San José");
        OpcionUbicacion c=new OpcionUbicacion();c.setId(2L);c.setIdPadre(1L);c.setNombre("Central");
        OpcionUbicacion d=new OpcionUbicacion();d.setId(3L);d.setIdPadre(2L);d.setNombre("Carmen");
        when(datos.provincias()).thenReturn(List.of(p));when(datos.cantones()).thenReturn(List.of(c));when(datos.distritos()).thenReturn(List.of(d));
        var inicio=mvc.perform(get("/cliente/inicio").session(s)).andExpect(status().isOk()).andReturn();
        String html=inicio.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        var forms=java.util.regex.Pattern.compile("<form\\b([^>]*)>(.*?)</form>",java.util.regex.Pattern.DOTALL).matcher(html);
        String attributes=null;
        while(forms.find()) if(forms.group(2).contains("Enviar reporte")) attributes=forms.group(1);
        assertNotNull(attributes,"El botón debe pertenecer a un formulario");
        var action=java.util.regex.Pattern.compile("action=\"([^\"]+)\"").matcher(attributes);
        assertTrue(action.find(),"El formulario debe tener una acción explícita");
        assertEquals("/cliente/incidentes",action.group(1));assertTrue(attributes.contains("method=\"post\""));
        CsrfToken csrf=(CsrfToken)inicio.getRequest().getAttribute(CsrfToken.class.getName());
        mvc.perform(post(action.group(1)).session(s).param(csrf.getParameterName(),csrf.getToken())
                .param("idVehiculo","4").param("idTipoIncidente","2").param("idProvincia","1").param("idCanton","2").param("idDistrito","3")
                .param("detalles","Falla mecánica").param("otrasSenas","Frente al parque").param("nombreContacto","Pedro").param("primerApellidoContacto","Soto")
                .param("telefono","88888888").param("idUsuario","99"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/cliente/inicio"))
                .andExpect(flash().attribute("mensajeExito","Incidente reportado correctamente."));
        var captor=org.mockito.ArgumentCaptor.forClass(Incidente.class);verify(incidentes).guardar(captor.capture());
        assertEquals(8L,captor.getValue().getIdCliente());assertEquals("Pedro",captor.getValue().getNombreReportante());
    }
}
