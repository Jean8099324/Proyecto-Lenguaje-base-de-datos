package com.rescatevial.controller;

import com.rescatevial.domain.Vehiculo;
import com.rescatevial.service.VehiculoService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Map;

@Controller
@RequestMapping("/vehiculos")
public class VehiculoController {
    private final VehiculoService servicio;

    public VehiculoController(VehiculoService servicio, com.rescatevial.service.UsuarioService usuarios) {
        this.servicio = servicio;
    }

    @GetMapping
    public String listado(Model model) {
        var vehiculos = servicio.listar();
        model.addAttribute("vehiculos", vehiculos);
        model.addAttribute("totalVehiculos", vehiculos.size());
        return "vehiculos/listado";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setIdEstado(servicio.estadoActivo());
        model.addAttribute("vehiculoForm", vehiculo);
        opciones(model, true);
        return "vehiculos/formulario";
    }

    @GetMapping("/validar-placa")
    @ResponseBody
    public Map<String, Boolean> validarPlaca(@RequestParam String placa,
                                             @RequestParam(required = false) Long id) {
        return Map.of("duplicada", servicio.placaRegistrada(placa, id));
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        var vehiculo = servicio.buscar(id);
        if (vehiculo.isEmpty()) {
            redirect.addFlashAttribute("mensajeError", "Vehículo no encontrado.");
            return "redirect:/vehiculos";
        }
        model.addAttribute("vehiculoForm", vehiculo.get());
        opciones(model, false);
        return "vehiculos/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("vehiculoForm") Vehiculo vehiculo,
                          BindingResult errores, Model model, RedirectAttributes redirect,
                          org.springframework.security.core.Authentication authentication) {
        boolean nuevo = vehiculo.getId() == null;
        if (errores.hasErrors()) {
            model.addAttribute("mensajeError", "Revisa los campos obligatorios.");
            opciones(model, nuevo);
            return "vehiculos/formulario";
        }
        try {
            servicio.guardar(vehiculo);
            redirect.addFlashAttribute("mensajeExito", nuevo ?
                    "Vehículo registrado correctamente." : "Vehículo actualizado correctamente.");
            return "redirect:/vehiculos";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("mensajeError", e.getMessage());
        } catch (Exception e) {
            model.addAttribute("mensajeError", "No se pudo guardar el vehículo. Revisa los datos.");
        }
        opciones(model, nuevo);
        return "vehiculos/formulario";
    }

    @PostMapping("/{id}/desactivar")
    public String desactivar(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            servicio.desactivar(id);
            redirect.addFlashAttribute("mensajeExito", "Vehículo desactivado correctamente.");
        } catch (Exception e) {
            redirect.addFlashAttribute("mensajeError", "No se pudo desactivar el vehículo.");
        }
        return "redirect:/vehiculos";
    }

    @PostMapping("/{id}/reactivar")
    public String reactivar(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            servicio.reactivar(id);
            redirect.addFlashAttribute("mensajeExito", "Vehículo reactivado correctamente.");
        } catch (Exception e) {
            redirect.addFlashAttribute("mensajeError", "No se pudo reactivar el vehículo.");
        }
        return "redirect:/vehiculos";
    }

    private void opciones(Model model, boolean nuevo) {
        model.addAttribute("esNuevo", nuevo);
        model.addAttribute("marcas", servicio.marcas());
        model.addAttribute("modelos", servicio.modelos());
        model.addAttribute("anios", servicio.anios());
        model.addAttribute("estadoActivo", servicio.estadoActivo());
    }
}
