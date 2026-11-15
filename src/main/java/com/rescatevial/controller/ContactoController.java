package com.rescatevial.controller;

import com.rescatevial.domain.Contacto;
import com.rescatevial.service.ContactoService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/contactos")
public class ContactoController {
    private final ContactoService servicio;

    public ContactoController(ContactoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public String listado(Model model) {
        var datos = servicio.listar();
        model.addAttribute("contactos", datos);
        model.addAttribute("totalContactos", datos.size());
        return "contactos/listado";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        Contacto contacto = new Contacto();
        contacto.setIdEstado(servicio.estadoActivo());
        model.addAttribute("contactoForm", contacto);
        model.addAttribute("esNuevo", true);
        return "contactos/formulario";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes mensajes) {
        var registro = servicio.buscar(id);
        if (registro.isEmpty()) {
            mensajes.addFlashAttribute("mensajeError", "Contacto no encontrado.");
            return "redirect:/contactos";
        }
        model.addAttribute("contactoForm", registro.get());
        model.addAttribute("esNuevo", false);
        return "contactos/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("contactoForm") Contacto contacto,
                          BindingResult errores, Model model, RedirectAttributes mensajes) {
        boolean esNuevo = contacto.getId() == null;
        if (errores.hasErrors()) {
            model.addAttribute("esNuevo", esNuevo);
            model.addAttribute("mensajeError", "Revisa los campos del formulario.");
            return "contactos/formulario";
        }
        try {
            servicio.guardar(contacto);
            mensajes.addFlashAttribute("mensajeExito", esNuevo
                    ? "Contacto registrado correctamente." : "Contacto actualizado correctamente.");
            return "redirect:/contactos";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("mensajeError", e.getMessage());
        } catch (Exception e) {
            model.addAttribute("mensajeError", "No se pudo guardar el contacto. Revisa sus datos.");
        }
        model.addAttribute("esNuevo", esNuevo);
        return "contactos/formulario";
    }

    @PostMapping("/{id}/desactivar")
    public String desactivar(@PathVariable Long id, RedirectAttributes mensajes) {
        try {
            servicio.desactivar(id);
            mensajes.addFlashAttribute("mensajeExito", "Contacto desactivado correctamente.");
        } catch (Exception e) {
            mensajes.addFlashAttribute("mensajeError", "No se pudo desactivar el contacto.");
        }
        return "redirect:/contactos";
    }

    @PostMapping("/{id}/reactivar")
    public String reactivar(@PathVariable Long id, RedirectAttributes mensajes) {
        try {
            servicio.reactivar(id);
            mensajes.addFlashAttribute("mensajeExito", "Contacto reactivado correctamente.");
        } catch (Exception e) {
            mensajes.addFlashAttribute("mensajeError", "No se pudo reactivar el contacto.");
        }
        return "redirect:/contactos";
    }

    @GetMapping("/{id}/incidentes")
    public String relaciones(@PathVariable Long id, Model model, RedirectAttributes mensajes) {
        var contacto = servicio.buscar(id);
        if (contacto.isEmpty()) {
            mensajes.addFlashAttribute("mensajeError", "Contacto no encontrado.");
            return "redirect:/contactos";
        }
        var incidentes = servicio.incidentes();
        Map<Long, String> nombres = incidentes.stream()
                .collect(Collectors.toMap(i -> i.getId(), i -> i.getDetalle(), (a, b) -> a));
        model.addAttribute("contacto", contacto.get());
        model.addAttribute("incidentes", incidentes);
        model.addAttribute("relaciones", servicio.relaciones(id));
        model.addAttribute("nombresIncidentes", nombres);
        return "contactos/relaciones";
    }

    @PostMapping("/{id}/incidentes/asociar")
    public String asociar(@PathVariable Long id, @RequestParam Long idIncidente,
                          RedirectAttributes mensajes) {
        try {
            servicio.asociarIncidente(id, idIncidente);
            mensajes.addFlashAttribute("mensajeExito", "Contacto asociado al incidente correctamente.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            mensajes.addFlashAttribute("mensajeError", e.getMessage());
        } catch (Exception e) {
            mensajes.addFlashAttribute("mensajeError", "No se pudo asociar el incidente.");
        }
        return "redirect:/contactos/" + id + "/incidentes";
    }

    @PostMapping("/{id}/incidentes/{idIncidente}/quitar")
    public String quitar(@PathVariable Long id, @PathVariable Long idIncidente,
                         RedirectAttributes mensajes) {
        try {
            servicio.quitarIncidente(id, idIncidente);
            mensajes.addFlashAttribute("mensajeExito", "Contacto retirado del incidente correctamente.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            mensajes.addFlashAttribute("mensajeError", e.getMessage());
        } catch (Exception e) {
            mensajes.addFlashAttribute("mensajeError", "No se pudo retirar el contacto del incidente.");
        }
        return "redirect:/contactos/" + id + "/incidentes";
    }
}
