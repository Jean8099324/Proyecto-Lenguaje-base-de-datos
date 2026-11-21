package com.rescatevial.controller;

import com.rescatevial.domain.Usuario;
import com.rescatevial.service.EstadoService;
import com.rescatevial.service.CorreoService;
import com.rescatevial.service.TipoUsuarioService;
import com.rescatevial.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {
    private final UsuarioService usuarioService;
    private final TipoUsuarioService tipoUsuarioService;
    private final EstadoService estadoService;
    private final CorreoService correoService;

    public UsuarioController(UsuarioService usuarioService, TipoUsuarioService tipoUsuarioService,
                             EstadoService estadoService, CorreoService correoService) {
        this.usuarioService = usuarioService;
        this.tipoUsuarioService = tipoUsuarioService;
        this.estadoService = estadoService;
        this.correoService = correoService;
    }

    @GetMapping("/listado")
    public String inicio(@RequestParam(defaultValue = "1") int pagina, Model model) {
        int tamanioPagina = 10;
        var resultado = usuarioService.getUsuariosPaginados(Math.max(1, pagina), tamanioPagina);
        long totalUsuarios = resultado.total();
        int totalPaginas = (int) Math.max(1, (totalUsuarios + tamanioPagina - 1) / tamanioPagina);
        int paginaActual = Math.min(Math.max(1, pagina), totalPaginas);
        if (paginaActual != Math.max(1, pagina)) {
            resultado = usuarioService.getUsuariosPaginados(paginaActual, tamanioPagina);
        }
        model.addAttribute("usuarios", resultado.usuarios());
        model.addAttribute("totalUsuarios", totalUsuarios);
        model.addAttribute("paginaActual", paginaActual);
        model.addAttribute("totalPaginas", totalPaginas);
        model.addAttribute("inicioRegistro", totalUsuarios == 0 ? 0 : (paginaActual - 1) * tamanioPagina + 1);
        model.addAttribute("finRegistro", Math.min((long) paginaActual * tamanioPagina, totalUsuarios));
        model.addAttribute("tiposUsuario", tipoUsuarioService.listarActivos());
        model.addAttribute("estados", estadoService.listar());
        model.addAttribute("roles", usuarioService.getRolesNombres());
        if (!model.containsAttribute("usuarioForm")) model.addAttribute("usuarioForm", new Usuario());
        return "usuarios/listado";
    }

    @GetMapping("/modificar/{id}")
    public String modificar(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        if (!model.containsAttribute("usuarioForm")) {
            Usuario usuario = usuarioService.getUsuarioPorId(id).orElse(null);
            if (usuario == null) {
                redirectAttributes.addFlashAttribute("mensajeError", "No se encontró el usuario indicado.");
                return "redirect:/usuarios/listado";
            }
            // No mostrar ni enviar al formulario el hash almacenado como si fuera una contraseña nueva.
            usuario.setPassword(null);
            usuario.setRolesNombresSeleccionados(usuario.getRoles().stream()
                    .map(rol -> rol.getNombre()).collect(java.util.stream.Collectors.toSet()));
            model.addAttribute("usuarioForm", usuario);
        }
        model.addAttribute("tiposUsuario", tipoUsuarioService.listarActivos());
        model.addAttribute("estados", estadoService.listar());
        model.addAttribute("roles", usuarioService.getRolesNombres());
        return "usuarios/modifica";
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("usuarioForm") Usuario usuario,
                          BindingResult bindingResult,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("mensajeError", "Revise los campos requeridos y sus formatos.");
            // No conservar la contraseña ingresada dentro de los atributos flash de la redirección.
            usuario.setPassword(null);
            redirectAttributes.addFlashAttribute("usuarioForm", usuario);

            if (usuario.getId() == null) {
                redirectAttributes.addFlashAttribute("modalAbierto", true);
                return "redirect:/usuarios/listado";
            }
            return "redirect:/usuarios/modificar/" + usuario.getId();
        }

        try {
            boolean nuevo = usuario.getId() == null;
            String passwordTemporal = usuario.getPassword();
            String destinatario = usuario.getCorreo();
            if (nuevo && (destinatario == null || destinatario.isBlank())) {
                throw new IllegalArgumentException("El correo es obligatorio para enviar las credenciales.");
            }
            usuarioService.save(usuario);
            if (nuevo) {
                correoService.enviarCredenciales(destinatario, usuario.getUsername(), passwordTemporal);
                redirectAttributes.addFlashAttribute("mensajeExito",
                        "Usuario registrado y credenciales enviadas al correo indicado.");
            } else {
                redirectAttributes.addFlashAttribute("mensajeExito", "Usuario actualizado correctamente.");
            }
            return "redirect:/usuarios/listado";
        } catch (DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", "No se pudo guardar: revisa el username, tipo y estado.");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("mensajeError", "No se pudo guardar el usuario: " + ex.getMessage());
        }

        if (usuario.getId() != null) {
            return "redirect:/usuarios/modificar/" + usuario.getId();
        }
        redirectAttributes.addFlashAttribute("modalAbierto", true);
        // Si falló el alta, conserva los demás datos del formulario, pero descarta la contraseña.
        usuario.setPassword(null);
        redirectAttributes.addFlashAttribute("usuarioForm", usuario);
        return "redirect:/usuarios/listado";
    }

}
