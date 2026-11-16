package com.rescatevial.controller;
import com.rescatevial.domain.*;
import com.rescatevial.service.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.BindingResult;
@RestController
@RequestMapping("/incidentes/clientes")
public class ClienteIncidenteController {
 private final ClienteService clientes;
 private final ClienteIncidenteService registro;
 private final DatoContactoService datos;
 private final VehiculoService vehiculos;
 private final IncidenteService incidentes;
 public ClienteIncidenteController(ClienteService clientes,ClienteIncidenteService registro,DatoContactoService datos,VehiculoService vehiculos,IncidenteService incidentes) {
  this.clientes=clientes; this.registro=registro; this.datos=datos; this.vehiculos=vehiculos; this.incidentes=incidentes;
 }
 private Map<String,Object> opcion(Usuario u) {
  return Map.of("id",u.getId(),"nombre",java.util.stream.Stream.of(u.getNombre(),u.getPrimerApellido(),u.getSegundoApellido())
   .filter(v->v!=null&&!v.isBlank()).collect(java.util.stream.Collectors.joining(" ")));
 }
 @GetMapping public Map<String,Object> listar() {
  Long activo=clientes.estadoActivoId();
  return Map.of("clientes",clientes.listar().stream().filter(u->activo.equals(u.getEstado())).map(this::opcion).toList(),
   "provincias",datos.provincias(),"cantones",datos.cantones(),"distritos",datos.distritos());
 }
 @GetMapping("/{id}")
 public ResponseEntity<?> datos(@PathVariable Long id) {
  var cliente=clientes.buscar(id).orElse(null);
  if (cliente == null) return ResponseEntity.notFound().build();
  var direccion=datos.direcciones(id).stream()
   .filter(d -> "ACTIVO".equalsIgnoreCase(d.getNombreEstado())).findFirst().orElse(null);
  Map<String,Object> respuesta=new LinkedHashMap<>();
  respuesta.put("id",cliente.getId());
  respuesta.put("username",cliente.usernameParaMostrar());
  respuesta.put("nombre",cliente.getNombre());
  respuesta.put("primerApellido",cliente.getPrimerApellido());
  respuesta.put("segundoApellido",cliente.getSegundoApellido());
  respuesta.put("direccion",direccion == null ? null : Map.of(
   "provincia",direccion.getIdProvincia(),"canton",direccion.getIdCanton(),
   "distrito",direccion.getIdDistrito(),"otrasSenas",
   direccion.getOtrasSenas() == null ? "" : direccion.getOtrasSenas()));
  respuesta.put("correos",datos.correos(id).stream()
   .filter(d -> "ACTIVO".equalsIgnoreCase(d.getNombreEstado())).map(DatoContacto::getValor).toList());
  respuesta.put("telefonos",datos.telefonos(id).stream()
   .filter(d -> "ACTIVO".equalsIgnoreCase(d.getNombreEstado())).map(DatoContacto::getValor).toList());
  return ResponseEntity.ok(respuesta);
 }
 @PostMapping public ResponseEntity<?> crear(@Valid @ModelAttribute ClienteIncidenteForm f,BindingResult errores) {
  if(errores.hasErrors()) return ResponseEntity.badRequest().body(Map.of("error","Revise nombre, apellidos, dirección y formatos de teléfono o correo."));
  try { return ResponseEntity.ok(opcion(registro.crear(f))); }
  catch(Exception ex) { return ResponseEntity.badRequest().body(Map.of("error","No se pudo registrar el cliente: "+ex.getMessage())); }
 }
 @PostMapping("/asociar/{idIncidente}")
 public org.springframework.web.servlet.ModelAndView asociar(@PathVariable Long idIncidente,@RequestParam Long idUsuario,
         org.springframework.web.servlet.mvc.support.RedirectAttributes redirect) {
  try {
   clientes.buscar(idUsuario).filter(u->clientes.estadoActivoId().equals(u.getEstado()))
       .orElseThrow(()->new IllegalArgumentException("Seleccione un cliente activo."));
   Incidente incidente=incidentes.buscar(idIncidente).orElseThrow(()->new IllegalArgumentException("El incidente no existe."));
   Vehiculo vehiculo=vehiculos.buscar(incidente.getIdVehiculo()).orElseThrow();
   vehiculo.setIdUsuario(idUsuario); vehiculos.guardar(vehiculo);
   redirect.addFlashAttribute("mensajeExito","Cliente propietario actualizado correctamente.");
  } catch(Exception ex) { redirect.addFlashAttribute("mensajeError","No se pudo asociar el cliente: "+ex.getMessage()); }
  return new org.springframework.web.servlet.ModelAndView("redirect:/incidentes/detalle/"+idIncidente);
 }
}
