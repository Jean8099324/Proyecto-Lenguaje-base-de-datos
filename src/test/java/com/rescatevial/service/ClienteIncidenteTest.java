package com.rescatevial.service;
import com.rescatevial.domain.*;
import com.rescatevial.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class ClienteIncidenteTest {
 @Test void creaClienteSinCredencialesConDireccionYContacto() {
  var clientes=mock(ClienteService.class); var repo=mock(UsuarioRepository.class); var datos=mock(DatoContactoService.class);
  when(clientes.tipoClienteId()).thenReturn(5L);when(clientes.estadoActivoId()).thenReturn(1L);
  Usuario creado=new Usuario();creado.setId(20L); when(repo.findByUsername(anyString())).thenReturn(Optional.of(creado));
  ClienteIncidenteForm f=new ClienteIncidenteForm();f.setNombre(" Sofia ");f.setPrimerApellido(" Mora ");
  f.setIdProvincia(1L);f.setIdCanton(2L);f.setIdDistrito(3L);f.setTelefono("88888888");f.setCorreo("sofia@example.com");
  assertEquals(20L,new ClienteIncidenteService(clientes,repo,datos).crear(f).getId());
  verify(repo).insertUsingProcedure(argThat(u->u.getNombre().equals("Sofia")&&u.getTipoUsuario().equals(5L)&&u.getPassword()==null),isNull());
  verify(repo,never()).assignRoleToUser(any(),any());
  verify(datos).guardar(eq("direccion"),argThat(d->d.getIdUsuario().equals(20L)&&d.getIdDistrito().equals(3L)),eq(false));
  verify(datos).guardar(eq("telefono"),argThat(d->d.getValor().equals("88888888")),eq(false));
  verify(datos).guardar(eq("correo"),argThat(d->d.getValor().equals("sofia@example.com")),eq(false));
 }
 @Test void clienteSinPasswordNoPuedeIniciarSesion() {
  var repo=mock(UsuarioRepository.class);Usuario u=new Usuario();
  when(repo.findByUsernameAndActivoTrue("cliente")).thenReturn(Optional.of(u));
  var servicio=new UsuarioDetailsService(repo,mock(jakarta.servlet.http.HttpSession.class));
  assertThrows(org.springframework.security.core.userdetails.UsernameNotFoundException.class,()->servicio.loadUserByUsername("cliente"));
 }
 @Test void detalleAsociaClienteAlVehiculoSinCambiarEjecutivo() {
  var clientes=mock(ClienteService.class);var registro=mock(ClienteIncidenteService.class);
  var datos=mock(DatoContactoService.class);var vehiculos=mock(VehiculoService.class);var incidentes=mock(IncidenteService.class);
  Usuario cliente=new Usuario();cliente.setId(20L);cliente.setEstado(1L);
  when(clientes.buscar(20L)).thenReturn(Optional.of(cliente));when(clientes.estadoActivoId()).thenReturn(1L);
  Incidente i=new Incidente();i.setId(1L);i.setIdVehiculo(2L);i.setIdUsuario(7L);
  when(incidentes.buscar(1L)).thenReturn(Optional.of(i));Vehiculo v=new Vehiculo();v.setId(2L);
  when(vehiculos.buscar(2L)).thenReturn(Optional.of(v));
  var controller=new com.rescatevial.controller.ClienteIncidenteController(clientes,registro,datos,vehiculos,incidentes);
  assertEquals("redirect:/incidentes/detalle/1",controller.asociar(1L,20L,new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap()).getViewName());
  verify(vehiculos).guardar(argThat(actual->actual.getIdUsuario().equals(20L)));
  verify(incidentes,never()).guardar(any());assertEquals(7L,i.getIdUsuario());
 }
 @Test void renderizaSelectorSinPedirCredenciales() {
  var resolver=new org.thymeleaf.templateresolver.ClassLoaderTemplateResolver();
  resolver.setPrefix("templates/");resolver.setSuffix(".html");resolver.setTemplateMode("HTML");
  var engine=new org.thymeleaf.spring6.SpringTemplateEngine();engine.setTemplateResolver(resolver);
  var context=new org.thymeleaf.context.Context();context.setVariable("actual",20L);
  String html=engine.process("fragments/cliente-propietario",context);
   assertTrue(html.contains("data-actual=\"20\""));assertTrue(html.contains("name=\"idUsuario\""));
   assertTrue(html.contains("Cliente dueño del vehículo (opcional)"));
   assertTrue(html.contains("<option value=\"\">Sin propietario registrado</option>"));
   assertFalse(html.contains("name=\"idUsuario\" class=\"seleccionar-cliente\" required"));
  assertFalse(html.contains("type=\"password\""));
 }
}
