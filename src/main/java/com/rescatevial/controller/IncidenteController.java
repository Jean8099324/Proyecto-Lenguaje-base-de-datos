package com.rescatevial.controller;

import com.rescatevial.domain.Incidente;
import com.rescatevial.domain.VehiculoIncidenteForm;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/incidentes")
public class IncidenteController {
    private final IncidenteService incidenteService;
    private final EstadoService estadoService;
    private final UsuarioService usuarioService;

    public IncidenteController(IncidenteService incidenteService, EstadoService estadoService,
                               UsuarioService usuarioService) {
        this.incidenteService = incidenteService;
        this.estadoService = estadoService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/listado")
    public String listado(Model model) {
        var incidentes = incidenteService.listar();
        model.addAttribute("incidentes", incidentes);
        model.addAttribute("totalIncidentes", incidentes.size());
        catalogos(model);
        if (!model.containsAttribute("incidenteForm")) model.addAttribute("incidenteForm", new Incidente());
        return "incidentes/listado";
    }

    @GetMapping("/modificar/{id}")
    public String modificar(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        Incidente incidente = incidenteService.buscar(id).orElse(null);
        if (incidente == null) {
            redirect.addFlashAttribute("mensajeError", "No se encontró el incidente indicado.");
            return "redirect:/incidentes/listado";
        }
        model.addAttribute("incidenteForm", incidente);
        catalogos(model);
        return "incidentes/modifica";
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("incidenteForm") Incidente incidente,
                          BindingResult result, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("mensajeError", "Complete los campos requeridos del incidente.");
            redirect.addFlashAttribute("incidenteForm", incidente);
            redirect.addFlashAttribute("modalAbierto", true);
            return incidente.getId() == null ? "redirect:/incidentes/listado" : "redirect:/incidentes/modificar/" + incidente.getId();
        }
        try {
            incidenteService.guardar(incidente);
            redirect.addFlashAttribute("mensajeExito", incidente.getId() == null
                    ? "Incidente registrado correctamente." : "Incidente actualizado correctamente.");
        } catch (Exception ex) {
            redirect.addFlashAttribute("mensajeError", "No se pudo guardar el incidente: " + ex.getMessage());
            redirect.addFlashAttribute("incidenteForm", incidente);
            redirect.addFlashAttribute("modalAbierto", incidente.getId() == null);
            if (incidente.getId() != null) return "redirect:/incidentes/modificar/" + incidente.getId();
        }
        return "redirect:/incidentes/listado";
    }

    @PostMapping("/desactivar/{id}")
    public String desactivar(@PathVariable Long id, RedirectAttributes redirect) {
        incidenteService.desactivar(id);
        redirect.addFlashAttribute("mensajeExito", "Incidente desactivado correctamente.");
        return "redirect:/incidentes/listado";
    }

    @PostMapping("/vehiculo/guardar")
    @ResponseBody
    public ResponseEntity<java.util.Map<String, Object>> guardarVehiculo(
            @Valid @ModelAttribute VehiculoIncidenteForm vehicle, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "Complete todos los datos requeridos del vehículo."));
        }
        try {
            var saved = incidenteService.registrarVehiculo(vehicle);
            return ResponseEntity.ok(java.util.Map.of("id", saved.getId(), "placa", saved.getEtiqueta()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(java.util.Map.of("error", "No se pudo registrar el vehículo: " + ex.getMessage()));
        }
    }

    private void catalogos(Model model) {
        model.addAttribute("vehiculos", incidenteService.vehiculos());
        model.addAttribute("tiposIncidente", incidenteService.tipos());
        model.addAttribute("prioridades", incidenteService.prioridades());
        model.addAttribute("ubicaciones", incidenteService.ubicaciones());
        model.addAttribute("marcasVehiculo", incidenteService.marcasVehiculo());
        model.addAttribute("modelosVehiculo", incidenteService.modelosVehiculo());
        model.addAttribute("aniosVehiculo", incidenteService.aniosVehiculo());
        model.addAttribute("usuariosActivos", usuarioService.getUsuarios(true));
        var estados = estadoService.listar();
        model.addAttribute("estadosVehiculo", estados.stream()
                .filter(estado -> Long.valueOf(1).equals(estado.getId())
                        || Long.valueOf(2).equals(estado.getId()))
                .toList());
        model.addAttribute("estados", estados);
        model.addAttribute("estadosFiltro", estados);
    }
}
