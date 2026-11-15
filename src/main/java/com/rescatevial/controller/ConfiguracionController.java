package com.rescatevial.controller;

import com.rescatevial.domain.ConfiguracionRegistro;
import com.rescatevial.domain.ConfiguracionTipo;
import com.rescatevial.service.ConfiguracionService;
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
@RequestMapping("/configuracion")
public class ConfiguracionController {
    private final ConfiguracionService servicio;
    public ConfiguracionController(ConfiguracionService servicio) { this.servicio = servicio; }

    private void menu(Model model, ConfiguracionTipo tipo) {
        model.addAttribute("catalogos", ConfiguracionTipo.todos());
        model.addAttribute("tipo", tipo);
    }

    @GetMapping
    public String inicio() { return "redirect:/configuracion/provincias"; }

    @GetMapping("/{catalogo}")
    public String listado(@PathVariable String catalogo, Model model) {
        var tipo = ConfiguracionTipo.desdeRuta(catalogo);
        menu(model, tipo);
        var datos = servicio.listar(tipo);
        model.addAttribute("registros", datos);
        model.addAttribute("total", datos.size());
        return "configuracion/listado";
    }

    private String form(Model model, ConfiguracionTipo tipo, ConfiguracionRegistro registro, boolean nuevo) {
        menu(model, tipo);
        model.addAttribute("registro", registro);
        model.addAttribute("nuevo", nuevo);
        model.addAttribute("opcionesPadre", servicio.opcionesPadre(tipo));
        return "configuracion/formulario";
    }

    @GetMapping("/{catalogo}/nuevo")
    public String nuevo(@PathVariable String catalogo, Model model) {
        var tipo = ConfiguracionTipo.desdeRuta(catalogo);
        ConfiguracionRegistro registro = new ConfiguracionRegistro();
        registro.setIdEstado(1L);
        return form(model, tipo, registro, true);
    }

    @GetMapping("/{catalogo}/editar/{id}")
    public String editar(@PathVariable String catalogo, @PathVariable Long id, Model model) {
        var tipo = ConfiguracionTipo.desdeRuta(catalogo);
        return form(model, tipo, servicio.buscar(tipo, id), false);
    }

    @PostMapping("/{catalogo}/guardar")
    public String guardar(@PathVariable String catalogo,
                           @ModelAttribute("registro") ConfiguracionRegistro registro,
                           @RequestParam boolean nuevo,
                           Model model, RedirectAttributes mensajes) {
        var tipo = ConfiguracionTipo.desdeRuta(catalogo);
        try {
            servicio.guardar(tipo, registro, nuevo);
            mensajes.addFlashAttribute("mensajeExito", nuevo ? "Registro creado correctamente." : "Registro actualizado correctamente.");
            return "redirect:/configuracion/" + catalogo;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("mensajeError", ex.getMessage());
        } catch (Exception ex) {
            model.addAttribute("mensajeError", "No fue posible guardar. Comprueba los datos y el paquete Oracle.");
        }
        return form(model, tipo, registro, nuevo);
    }

    @PostMapping("/{catalogo}/{id}/estado")
    public String cambiarEstado(@PathVariable String catalogo, @PathVariable Long id,
                                 @RequestParam boolean reactivar, RedirectAttributes mensajes) {
        var tipo = ConfiguracionTipo.desdeRuta(catalogo);
        try {
            servicio.cambiarEstado(tipo, id, reactivar);
            mensajes.addFlashAttribute("mensajeExito", reactivar ? "Registro reactivado correctamente." : "Registro desactivado correctamente.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            mensajes.addFlashAttribute("mensajeError", ex.getMessage());
        } catch (Exception ex) {
            mensajes.addFlashAttribute("mensajeError", "No se pudo cambiar el estado. Podría haber registros relacionados.");
        }
        return "redirect:/configuracion/" + catalogo;
    }
}
