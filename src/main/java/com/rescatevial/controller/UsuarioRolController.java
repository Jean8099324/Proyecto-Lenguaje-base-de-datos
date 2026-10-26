package com.rescatevial.controller;

import com.rescatevial.domain.Usuario;
import com.rescatevial.service.UsuarioService;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Pantalla de mantenimiento de roles por usuario, siguiendo el flujo de Powerlete. */
@Controller
@RequestMapping("/usuarios/roles")
public class UsuarioRolController {
    private final UsuarioService usuarioService;

    public UsuarioRolController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String inicio(Model model) {
        model.addAttribute("usuario", null);
        model.addAttribute("usernameBuscado", "");
        model.addAttribute("rolesAsignados", List.of());
        model.addAttribute("rolesDisponibles", List.of());
        model.addAttribute("usernames", usuarioService.getUsernames());
        return "usuarios/roles";
    }

    @GetMapping("/buscar")
    public String buscar(@RequestParam String username, Model model) {
        model.addAttribute("usernames", usuarioService.getUsernames());
        model.addAttribute("usernameBuscado", username);
        var usuario = usuarioService.getUsuarioPorUsername(username).orElse(null);
        model.addAttribute("usuario", usuario);
        if (usuario != null) {
            var asignados = usuario.getRoles().stream().toList();
            var nombresAsignados = asignados.stream().map(rol -> rol.getNombre()).toList();
            var disponibles = usuarioService.getRolesNombres().stream()
                    .filter(nombre -> nombresAsignados.stream().noneMatch(asignado -> asignado.equalsIgnoreCase(nombre)))
                    .toList();
            model.addAttribute("rolesAsignados", asignados);
            model.addAttribute("rolesDisponibles", disponibles);
        } else {
            model.addAttribute("rolesAsignados", List.of());
            model.addAttribute("rolesDisponibles", List.of());
            model.addAttribute("mensajeError", "No se encontró el usuario " + username + ".");
        }
        return "usuarios/roles";
    }

    @PostMapping("/asignar")
    public String asignar(@RequestParam String username, @RequestParam String nombreRol,
                          RedirectAttributes redirectAttributes) {
        try {
            usuarioService.asignarRolPorUsername(username, nombreRol);
            redirectAttributes.addFlashAttribute("mensajeExito", "Rol asignado correctamente.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("mensajeError", "No se pudo asignar el rol: " + ex.getMessage());
        }
        redirectAttributes.addAttribute("username", username);
        return "redirect:/usuarios/roles/buscar";
    }

    @PostMapping("/quitar")
    public String quitar(@RequestParam String username, @RequestParam Long idRol,
                         RedirectAttributes redirectAttributes) {
        try {
            usuarioService.eliminarRol(username, idRol);
            redirectAttributes.addFlashAttribute("mensajeExito", "Rol eliminado correctamente.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("mensajeError", "No se pudo quitar el rol: " + ex.getMessage());
        }
        redirectAttributes.addAttribute("username", username);
        return "redirect:/usuarios/roles/buscar";
    }
}
