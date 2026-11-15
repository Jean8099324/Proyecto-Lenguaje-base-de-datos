package com.rescatevial.controller;

import com.rescatevial.domain.DatoContacto;
import com.rescatevial.service.DatoContactoService;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/datos-contacto")
public class DatoContactoController {
    private static final Set<String> TIPOS = Set.of("telefono", "correo", "direccion");
    private final DatoContactoService servicio;
    public DatoContactoController(DatoContactoService servicio) { this.servicio = servicio; }

    private static void validarTipo(String tipo) {
        if (!TIPOS.contains(tipo)) throw new IllegalArgumentException("Tipo desconocido.");
    }
    @GetMapping
    public String listado(@RequestParam(required = false) Long usuario, Model model) {
        model.addAttribute("usuarios", servicio.usuarios());
        model.addAttribute("usuarioSeleccionado", usuario);
        if (usuario != null) {
            try {
                model.addAttribute("nombreUsuarioSeleccionado", servicio.usuario(usuario).getNombre());
                model.addAttribute("telefonos", servicio.telefonos(usuario));
                model.addAttribute("correos", servicio.correos(usuario));
                model.addAttribute("direcciones", servicio.direcciones(usuario));
            } catch (IllegalArgumentException ex) {
                model.addAttribute("mensajeError", ex.getMessage());
            }
        }
        return "datos-contacto/listado";
    }
    private void datosFormulario(Model model, String tipo, DatoContacto d, boolean nuevo) {
        model.addAttribute("tipo", tipo);
        model.addAttribute("datoForm", d);
        model.addAttribute("esNuevo", nuevo);
        model.addAttribute("nombreUsuario", servicio.usuario(d.getIdUsuario()).getNombre());
        if ("direccion".equals(tipo)) {
            model.addAttribute("provincias", servicio.provincias());
            model.addAttribute("cantones", servicio.cantones());
            model.addAttribute("distritos", servicio.distritos());
        }
    }
    @GetMapping("/{tipo}/nuevo")
    public String nuevo(@PathVariable String tipo, @RequestParam Long usuario, Model model) {
        validarTipo(tipo);
        var d = new DatoContacto(); d.setIdUsuario(usuario);
        datosFormulario(model, tipo, d, true);
        return "datos-contacto/formulario";
    }
    @GetMapping("/{tipo}/editar")
    public String editar(@PathVariable String tipo, @RequestParam Long usuario,
                         @RequestParam(required = false) String valor,
                         @RequestParam(required = false) Long provincia,
                         @RequestParam(required = false) Long canton,
                         @RequestParam(required = false) Long distrito, Model model,
                         RedirectAttributes mensajes) {
        validarTipo(tipo);
        try {
            var d = servicio.buscar(tipo, usuario, valor, provincia, canton, distrito);
            d.setValorAnterior(d.getValor());
            datosFormulario(model, tipo, d, false);
            return "datos-contacto/formulario";
        } catch (IllegalArgumentException ex) {
            mensajes.addFlashAttribute("mensajeError", ex.getMessage());
            return "redirect:/datos-contacto?usuario=" + usuario;
        }
    }
    @PostMapping("/{tipo}/guardar")
    public String guardar(@PathVariable String tipo, @ModelAttribute("datoForm") DatoContacto dato,
                          @RequestParam(defaultValue = "false") boolean editar,
                          Model model, RedirectAttributes mensajes) {
        validarTipo(tipo);
        try {
            servicio.guardar(tipo, dato, editar);
            mensajes.addFlashAttribute("mensajeExito", "Datos guardados correctamente.");
            return "redirect:/datos-contacto?usuario=" + dato.getIdUsuario();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("mensajeError", ex.getMessage());
        } catch (Exception ex) {
            model.addAttribute("mensajeError", "No se pudo guardar. Comprueba los datos e intenta nuevamente.");
        }
        datosFormulario(model, tipo, dato, !editar);
        return "datos-contacto/formulario";
    }
    @PostMapping("/{tipo}/estado")
    public String estado(@PathVariable String tipo, @ModelAttribute DatoContacto dato,
                         @RequestParam boolean reactivar, RedirectAttributes mensajes) {
        validarTipo(tipo);
        try {
            servicio.cambiarEstado(tipo, dato, reactivar);
            mensajes.addFlashAttribute("mensajeExito", reactivar ? "Registro reactivado." : "Registro desactivado.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            mensajes.addFlashAttribute("mensajeError", ex.getMessage());
        } catch (Exception ex) {
            mensajes.addFlashAttribute("mensajeError", "No se pudo modificar el estado.");
        }
        return "redirect:/datos-contacto?usuario=" + dato.getIdUsuario();
    }
}
