package com.rescatevial.controller;

import com.rescatevial.domain.Usuario;
import com.rescatevial.service.PersonalService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/personal")
public class PersonalController {
    private final PersonalService personalService;

    public PersonalController(PersonalService personalService) {
        this.personalService = personalService;
    }

    @GetMapping
    public String listado(Model model) {
        var personas = personalService.listar();
        model.addAttribute("personas", personas);
        model.addAttribute("totalPersonal", personas.size());
        return "personal/listado";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        Usuario formulario = new Usuario();
        formulario.setEstado(personalService.estadoActivoId());
        model.addAttribute("personalForm", formulario);
        model.addAttribute("esNuevo", true);
        model.addAttribute("tiposPersonal", personalService.tiposDePersonal());
        return "personal/formulario";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes mensajes) {
        var persona = personalService.buscar(id);
        if (persona.isEmpty()) {
            mensajes.addFlashAttribute("mensajeError", "No se encontró la persona indicada.");
            return "redirect:/personal";
        }
        Usuario formulario = persona.get();
        formulario.setPassword(null); // Nunca enviar el hash al navegador.
        model.addAttribute("personalForm", formulario);
        model.addAttribute("esNuevo", false);
        model.addAttribute("tiposPersonal", personalService.tiposDePersonal());
        return "personal/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("personalForm") Usuario persona,
                          BindingResult errores, Model model, RedirectAttributes mensajes) {
        boolean nuevo = persona.getId() == null;
        if (nuevo && (persona.getPassword() == null || persona.getPassword().isBlank())) {
            errores.rejectValue("password", "obligatoria", "La contraseña es obligatoria.");
        }
        if (errores.hasErrors()) {
            return mostrarErrores(persona, nuevo, model, "Revisa los campos del formulario.");
        }
        try {
            personalService.guardar(persona);
            mensajes.addFlashAttribute("mensajeExito", nuevo
                    ? "Personal registrado correctamente." : "Datos del personal actualizados.");
            return "redirect:/personal";
        } catch (Exception e) {
            return mostrarErrores(persona, nuevo, model,
                    "No se pudo guardar. Revisa el username, tipo de personal y demás datos.");
        }
    }

    private String mostrarErrores(Usuario persona, boolean nuevo, Model model, String mensaje) {
        persona.setPassword(null);
        model.addAttribute("esNuevo", nuevo);
        model.addAttribute("tiposPersonal", personalService.tiposDePersonal());
        model.addAttribute("mensajeError", mensaje);
        return "personal/formulario";
    }

    @PostMapping("/{id}/desactivar")
    public String desactivar(@PathVariable Long id, Authentication autenticacion,
                             RedirectAttributes mensajes) {
        try {
            personalService.desactivar(id, autenticacion.getName());
            mensajes.addFlashAttribute("mensajeExito", "Personal desactivado correctamente.");
        } catch (Exception e) {
            mensajes.addFlashAttribute("mensajeError", "No se pudo desactivar. Comprueba que no sea tu propia cuenta.");
        }
        return "redirect:/personal";
    }

    @PostMapping("/{id}/reactivar")
    public String reactivar(@PathVariable Long id, RedirectAttributes mensajes) {
        try {
            personalService.reactivar(id);
            mensajes.addFlashAttribute("mensajeExito", "Personal reactivado correctamente.");
        } catch (Exception e) {
            mensajes.addFlashAttribute("mensajeError", "No se pudo reactivar la persona.");
        }
        return "redirect:/personal";
    }
}
