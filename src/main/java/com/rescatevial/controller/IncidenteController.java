package com.rescatevial.controller;

import com.rescatevial.domain.Incidente;
import com.rescatevial.domain.DatoContacto;
import com.rescatevial.domain.VehiculoIncidenteForm;
import com.rescatevial.service.ContactoService;
import com.rescatevial.service.DatoContactoService;
import com.rescatevial.service.EstadoService;
import com.rescatevial.service.IncidenteService;
import com.rescatevial.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/incidentes")
public class IncidenteController {
    private static final long ESTADO_REPORTADO_ID = 3L;
    private final IncidenteService incidenteService;
    private final EstadoService estadoService;
    private final UsuarioService usuarioService;
    private final ContactoService contactoService;
    private final DatoContactoService datoContactoService;

    public IncidenteController(IncidenteService incidenteService, EstadoService estadoService,
                               UsuarioService usuarioService, ContactoService contactoService,
                               DatoContactoService datoContactoService) {
        this.incidenteService = incidenteService;
        this.estadoService = estadoService;
        this.usuarioService = usuarioService;
        this.contactoService = contactoService;
        this.datoContactoService = datoContactoService;
    }

    @GetMapping("/listado")
    public String listado(Model model) {
        var incidentes = incidenteService.listar();
        model.addAttribute("incidentes", incidentes);
        model.addAttribute("totalIncidentes", incidentes.size());
        catalogos(model, null);
        if (!model.containsAttribute("incidenteForm")) {
            var ahora = java.time.ZonedDateTime.now(java.time.ZoneId.of("America/Costa_Rica"));
            var formulario = new Incidente();
            formulario.setFechaReporte(ahora.toLocalDate());
            formulario.setHoraReporte(ahora.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
            formulario.setIdEstado(ESTADO_REPORTADO_ID);
            model.addAttribute("incidenteForm", formulario);
        }
        return "incidentes/listado";
    }

    @GetMapping("/modificar/{id}")
    public String modificar(@PathVariable Long id,
                            @RequestParam(required = false) Long clienteId,
                            Model model, RedirectAttributes redirect) {
        Incidente incidente = model.containsAttribute("incidenteForm")
                ? (Incidente) model.getAttribute("incidenteForm")
                : incidenteService.buscar(id).orElse(null);
        if (incidente == null) {
            redirect.addFlashAttribute("mensajeError", "No se encontró el incidente indicado.");
            return "redirect:/incidentes/listado";
        }
        if (!model.containsAttribute("incidenteForm") && clienteId != null) {
            incidente.setIdCliente(clienteId);
        }
        if (!model.containsAttribute("incidenteForm")) {
            incidenteService.vehiculoDetalle(incidente.getIdVehiculo()).ifPresent(vehiculo -> {
                incidente.setPlacaVehiculo(vehiculo.getPlaca());
            });
            if (incidente.getIdCliente() != null) {
                usuarioService.getUsuarioPorId(incidente.getIdCliente()).ifPresent(cliente -> {
                    incidente.setUsernameCliente(cliente.getUsername());
                    incidente.setNombreCliente(cliente.getNombre());
                    incidente.setPrimerApellidoCliente(cliente.getPrimerApellido());
                    incidente.setSegundoApellidoCliente(cliente.getSegundoApellido());
                });
                datoContactoService.direcciones(incidente.getIdCliente()).stream()
                        .filter(direccion -> "ACTIVO".equalsIgnoreCase(direccion.getNombreEstado()))
                        .findFirst().ifPresent(direccion -> {
                            incidente.setIdProvinciaCliente(direccion.getIdProvincia());
                            incidente.setIdCantonCliente(direccion.getIdCanton());
                            incidente.setIdDistritoCliente(direccion.getIdDistrito());
                            incidente.setIdProvinciaClienteOriginal(direccion.getIdProvincia());
                            incidente.setIdCantonClienteOriginal(direccion.getIdCanton());
                            incidente.setIdDistritoClienteOriginal(direccion.getIdDistrito());
                            incidente.setOtrasSenasCliente(direccion.getOtrasSenas());
                        });
            }
            incidenteService.contactos(id).stream().findFirst().ifPresent(contacto -> {
                incidente.setIdContactoIncidente(contacto.getId());
                incidente.setNombreContactoIncidente(contacto.getNombre());
                incidente.setPrimerApellidoContactoIncidente(contacto.getPrimerApellido());
                incidente.setSegundoApellidoContactoIncidente(contacto.getSegundoApellido());
                incidente.setTelefonoContactoIncidente(contacto.getTelefono());
            });
        }
        model.addAttribute("incidenteForm", incidente);
        model.addAttribute("vehiculoForm", model.containsAttribute("vehiculoForm")
                ? model.getAttribute("vehiculoForm")
                : incidenteService.vehiculoParaEdicion(id).orElse(null));
        Long clienteSeleccionadoId = incidente.getIdCliente();
        model.addAttribute("correosCliente", clienteSeleccionadoId == null ? java.util.List.<DatoContacto>of()
                : datoContactoService.correos(clienteSeleccionadoId).stream()
                        .filter(dato -> "ACTIVO".equalsIgnoreCase(dato.getNombreEstado()))
                        .toList());
        model.addAttribute("telefonosCliente", clienteSeleccionadoId == null ? java.util.List.<DatoContacto>of()
                : datoContactoService.telefonos(clienteSeleccionadoId).stream()
                        .filter(dato -> "ACTIVO".equalsIgnoreCase(dato.getNombreEstado()))
                        .toList());
        model.addAttribute("clientes", usuarioService.getUsuarios(true).stream()
                .filter(cliente -> "CLIENTE".equalsIgnoreCase(cliente.getTipoUsuarioNombre()))
                .toList());
        catalogos(model, id);
        return "incidentes/modifica";
    }

    @GetMapping("/detalle/{id}")
    public String detalle(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        Incidente incidente = incidenteService.buscarDetalle(id).orElse(null);
        if (incidente == null) {
            redirect.addFlashAttribute("mensajeError", "No se encontró el incidente indicado.");
            return "redirect:/incidentes/listado";
        }
        model.addAttribute("incidente", incidente);
        model.addAttribute("contactos", incidenteService.contactos(id));
        var vehiculo = incidenteService.vehiculoDetalle(incidente.getIdVehiculo()).orElse(null);
        model.addAttribute("vehiculo", vehiculo);
        var clienteId = incidente.getIdCliente() != null ? incidente.getIdCliente() : incidente.getIdUsuario();
        model.addAttribute("clienteIncidente", clienteId == null ? null
                : usuarioService.getUsuarioPorId(clienteId).orElse(null));
        var correosCliente = clienteId == null ? java.util.List.<DatoContacto>of()
                : datoContactoService.correos(clienteId).stream()
                        .filter(dato -> "ACTIVO".equalsIgnoreCase(dato.getNombreEstado()))
                        .toList();
        var telefonosCliente = clienteId == null ? java.util.List.<DatoContacto>of()
                : datoContactoService.telefonos(clienteId).stream()
                        .filter(dato -> "ACTIVO".equalsIgnoreCase(dato.getNombreEstado()))
                        .toList();
        model.addAttribute("correosCliente", correosCliente);
        model.addAttribute("telefonosCliente", telefonosCliente);
        model.addAttribute("historialServicios", incidenteService.historialServicios(id));
        model.addAttribute("facturacion", incidenteService.facturacion(id));
        return "incidentes/detalle";
    }

    public String guardar(@Valid @ModelAttribute("incidenteForm") Incidente incidente,
                          BindingResult result, RedirectAttributes redirect,
                          org.springframework.security.core.Authentication authentication) {
        return guardar(incidente, result, redirect, authentication, null, null);
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("incidenteForm") Incidente incidente,
                          BindingResult result, RedirectAttributes redirect,
                          org.springframework.security.core.Authentication authentication,
                          @RequestParam(required = false) java.util.List<String> correosClienteNuevos,
                          @RequestParam(required = false) java.util.List<String> telefonosClienteNuevos) {
        boolean nuevo = incidente.getId() == null;
        boolean contactoIngresado = java.util.stream.Stream.of(incidente.getNombreContactoIncidente(),
                incidente.getPrimerApellidoContactoIncidente(), incidente.getSegundoApellidoContactoIncidente(),
                incidente.getTelefonoContactoIncidente()).anyMatch(v -> v != null && !v.isBlank());
        if (nuevo) incidente.setIdEstado(ESTADO_REPORTADO_ID);
        if (nuevo && (incidente.getNombreReportante() == null || incidente.getNombreReportante().isBlank())) {
            result.rejectValue("nombreReportante", "required", "Indique quién reporta el incidente.");
        }
        if (nuevo && (incidente.getTelefonoReportante() == null || incidente.getTelefonoReportante().isBlank())
                && (incidente.getCorreoReportante() == null || incidente.getCorreoReportante().isBlank())) {
            result.rejectValue("telefonoReportante", "required", "Indique al menos un teléfono o correo de contacto.");
        }
        if (!nuevo) {
            if (incidente.getIdCliente() != null) {
                if (incidente.getNombreCliente() == null || incidente.getNombreCliente().isBlank())
                    result.rejectValue("nombreCliente", "required", "Indique el nombre del cliente.");
                if (incidente.getPrimerApellidoCliente() == null || incidente.getPrimerApellidoCliente().isBlank())
                    result.rejectValue("primerApellidoCliente", "required", "Indique el primer apellido del cliente.");
            }
            if (incidente.getIdContactoIncidente() != null || contactoIngresado) {
                if (incidente.getNombreContactoIncidente() == null || incidente.getNombreContactoIncidente().isBlank())
                    result.rejectValue("nombreContactoIncidente", "required", "Indique el nombre del contacto.");
                if (incidente.getPrimerApellidoContactoIncidente() == null || incidente.getPrimerApellidoContactoIncidente().isBlank())
                    result.rejectValue("primerApellidoContactoIncidente", "required", "Indique el primer apellido del contacto.");
            }
        }
        if (!nuevo && incidente.getIdContactoIncidente() == null && contactoIngresado
                && (incidente.getTelefonoContactoIncidente() == null || incidente.getTelefonoContactoIncidente().isBlank()))
            result.rejectValue("telefonoContactoIncidente", "required", "Indique el teléfono del nuevo contacto.");
        if (result.hasErrors()) {
            redirect.addFlashAttribute("mensajeError", "Complete los campos requeridos del incidente.");
            redirect.addFlashAttribute("incidenteForm", incidente);
            redirect.addFlashAttribute("modalAbierto", true);
            return incidente.getId() == null ? "redirect:/incidentes/listado" : "redirect:/incidentes/modificar/" + incidente.getId();
        }
        try {
            incidente.setIdUsuario(incidente.getIdCliente());
            if (!nuevo) {
                if (incidente.getIdContactoIncidente() != null
                        && contactoService.buscar(incidente.getIdContactoIncidente()).isEmpty())
                    throw new IllegalArgumentException("El contacto asociado ya no existe.");
            }
            incidenteService.guardar(incidente);
            if (!nuevo && incidente.getIdCliente() != null) {
                usuarioService.actualizarNombreDesdeIncidente(incidente.getIdCliente(),
                        incidente.getNombreCliente(), incidente.getPrimerApellidoCliente(),
                        incidente.getSegundoApellidoCliente());
                if (incidente.getIdProvinciaCliente() != null
                        && incidente.getIdCantonCliente() != null
                        && incidente.getIdDistritoCliente() != null) {
                    DatoContacto direccion = new DatoContacto();
                    direccion.setIdProvincia(incidente.getIdProvinciaCliente());
                    direccion.setIdCanton(incidente.getIdCantonCliente());
                    direccion.setIdDistrito(incidente.getIdDistritoCliente());
                    direccion.setOtrasSenas(incidente.getOtrasSenasCliente());
                    if (incidente.getIdProvinciaClienteOriginal() != null
                            && incidente.getIdCantonClienteOriginal() != null
                            && incidente.getIdDistritoClienteOriginal() != null) {
                        datoContactoService.actualizarDireccionDesdeIncidente(incidente.getIdCliente(),
                                incidente.getIdProvinciaClienteOriginal(), incidente.getIdCantonClienteOriginal(),
                                incidente.getIdDistritoClienteOriginal(), direccion);
                    } else {
                        datoContactoService.crearDireccionDesdeIncidente(incidente.getIdCliente(), direccion);
                    }
                }
                if (incidente.getIdContactoIncidente() != null) {
                    contactoService.actualizarDesdeIncidente(incidente.getIdContactoIncidente(),
                            incidente.getNombreContactoIncidente(), incidente.getPrimerApellidoContactoIncidente(),
                            incidente.getSegundoApellidoContactoIncidente(), incidente.getTelefonoContactoIncidente());
                } else if (contactoIngresado) {
                    incidenteService.crearContactoDesdeIncidente(incidente);
                }
                guardarDatosContactoNuevos(incidente.getIdCliente(), "correo", correosClienteNuevos);
                guardarDatosContactoNuevos(incidente.getIdCliente(), "telefono", telefonosClienteNuevos);
            }
            redirect.addFlashAttribute("mensajeExito", nuevo
                    ? "Incidente registrado correctamente." : "Incidente actualizado correctamente.");
        } catch (Exception ex) {
            redirect.addFlashAttribute("mensajeError", "No se pudo guardar el incidente: " + ex.getMessage());
            return prepararFormularioConError(incidente, ex.getMessage(), redirect);
        }
        return "redirect:/incidentes/listado";
    }

    private void guardarDatosContactoNuevos(Long clienteId, String tipo, java.util.List<String> valores) {
        if (clienteId == null || valores == null) return;
        for (String valor : valores) {
            if (valor != null && !valor.isBlank()) {
                DatoContacto dato = new DatoContacto();
                dato.setIdUsuario(clienteId);
                dato.setValor(valor);
                datoContactoService.guardar(tipo, dato, false);
            }
        }
    }

    private String prepararFormularioConError(Incidente incidente, String mensaje, RedirectAttributes redirect) {
        redirect.addFlashAttribute("mensajeError", "No se pudo guardar el incidente: " + mensaje);
        redirect.addFlashAttribute("incidenteForm", incidente);
        if (incidente.getId() == null) {
            redirect.addFlashAttribute("modalAbierto", true);
            return "redirect:/incidentes/listado";
        }
        return "redirect:/incidentes/modificar/" + incidente.getId();
    }

    @PostMapping("/desactivar/{id}")
    public String desactivar(@PathVariable Long id, RedirectAttributes redirect) {
        incidenteService.desactivar(id);
        redirect.addFlashAttribute("mensajeExito", "Incidente desactivado correctamente.");
        return "redirect:/incidentes/listado";
    }

    @PostMapping("/vehiculo/actualizar")
    public String actualizarVehiculo(@RequestParam Long incidenteId,
                                     @Valid @ModelAttribute("vehiculoForm") com.rescatevial.domain.Vehiculo vehiculo,
                                     BindingResult result, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("mensajeError", "Complete los datos requeridos del vehículo.");
            redirect.addFlashAttribute("vehiculoForm", vehiculo);
            return "redirect:/incidentes/modificar/" + incidenteId;
        }
        try {
            incidenteService.actualizarVehiculoDesdeIncidente(incidenteId, vehiculo);
            redirect.addFlashAttribute("mensajeExito", "Datos del vehículo actualizados correctamente.");
        } catch (Exception ex) {
            redirect.addFlashAttribute("mensajeError", "No se pudo actualizar el vehículo: " + ex.getMessage());
            redirect.addFlashAttribute("vehiculoForm", vehiculo);
        }
        return "redirect:/incidentes/modificar/" + incidenteId;
    }

    @PostMapping("/vehiculo/guardar")
    @ResponseBody
    public ResponseEntity<java.util.Map<String, Object>> guardarVehiculo(
            @Valid @ModelAttribute VehiculoIncidenteForm vehicle, BindingResult result,
            org.springframework.security.core.Authentication authentication) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "Complete todos los datos requeridos del vehículo."));
        }
        try {
            if (authentication == null || !authentication.isAuthenticated()
                    || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)
                throw new IllegalStateException("Debe iniciar sesión para registrar un vehículo.");
            var saved = incidenteService.registrarVehiculo(vehicle);
            return ResponseEntity.ok(java.util.Map.of("id", saved.getId(), "placa", saved.getEtiqueta()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(java.util.Map.of("error", "No se pudo registrar el vehículo: " + ex.getMessage()));
        }
    }

    private void catalogos(Model model, Long incidentId) {
        model.addAttribute("vehiculos", incidentId == null
                ? incidenteService.vehiculos() : incidenteService.vehiculos(incidentId));
        model.addAttribute("tiposIncidente", incidenteService.tipos());
        model.addAttribute("prioridades", incidenteService.prioridades());
        model.addAttribute("provincias", incidenteService.provincias());
        model.addAttribute("cantones", incidenteService.cantones());
        model.addAttribute("distritos", incidenteService.distritos());
        model.addAttribute("marcasVehiculo", incidenteService.marcasVehiculo());
        model.addAttribute("modelosVehiculo", incidenteService.modelosVehiculo());
        model.addAttribute("aniosVehiculo", incidenteService.aniosVehiculo());
        var estados = estadoService.listar();
        model.addAttribute("estadosVehiculo", estados.stream()
                .filter(estado -> Long.valueOf(1).equals(estado.getId())
                        || Long.valueOf(2).equals(estado.getId()))
                .toList());
        model.addAttribute("estados", estados);
        model.addAttribute("estadosIncidente", estados.stream()
                .filter(estado -> estado.getId() != null
                        && estado.getId() >= 3 && estado.getId() <= 8)
                .toList());
        model.addAttribute("estadosFiltro", estados);
    }
}
