package com.rescatevial.controller;

import com.rescatevial.domain.Usuario;
import com.rescatevial.service.UsuarioService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {
    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public String listado(Model model) {
        completarModelo(model);
        if (!model.containsAttribute("usuarioForm")) model.addAttribute("usuarioForm", new Usuario());
        return "usuarios/listado";
    }

    @PostMapping
    public String registrar(@Valid Usuario usuarioForm, BindingResult bindingResult,
                            RedirectAttributes redirectAttributes, Model model) {
        if (bindingResult.hasErrors()) {
            completarModelo(model);
            model.addAttribute("modalAbierto", true);
            return "usuarios/listado";
        }
        try {
            service.registrar(usuarioForm);
            redirectAttributes.addFlashAttribute("mensajeExito", "Usuario registrado correctamente.");
        } catch (DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", "No se pudo registrar: revisa el ID, usuario, tipo y estado.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("mensajeError", "El procedimiento no pudo registrar el usuario: " + ex.getMessage());
        }
        return "redirect:/usuarios";
    }

    private void completarModelo(Model model) {
        List<Usuario> usuarios = service.listar();
        model.addAttribute("usuarios", usuarios);
        model.addAttribute("totalUsuarios", usuarios.size());
        model.addAttribute("tiposUsuario", service.listarTipos());
        model.addAttribute("estados", service.listarEstados());
    }
}
