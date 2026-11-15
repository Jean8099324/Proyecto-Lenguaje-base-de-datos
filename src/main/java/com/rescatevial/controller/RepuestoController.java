
package com.rescatevial.controller;

import java.sql.SQLException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.rescatevial.domain.Repuesto;
import com.rescatevial.service.RepuestoService;

/*
 * PANTALLA 22: REPUESTOS
 * DESCRIPCION: CONTROLADOR PARA GESTIONAR
 * EL LISTADO, REGISTRO, MODIFICACION Y
 * ELIMINACION DE REPUESTOS.
 */

@Controller
@RequestMapping("/repuestos")
public class RepuestoController {

    private final RepuestoService repuestoService;

    public RepuestoController(RepuestoService repuestoService) {
        this.repuestoService = repuestoService;
    }

    // MOSTRAR LISTADO DE REPUESTOS
    @GetMapping("/listado")
    public String listado(Model model) throws SQLException {

        model.addAttribute(
                "repuestos",
                repuestoService.listar());

        return "repuestos/listado";
    }

    // MOSTRAR FORMULARIO PARA CREAR UN REPUESTO
    @GetMapping("/nuevo")
    public String nuevo(Model model) {

        model.addAttribute("repuesto", new Repuesto());
        model.addAttribute("modoEdicion", false);

        return "repuestos/modifica";
    }

    // MOSTRAR FORMULARIO PARA MODIFICAR UN REPUESTO
    @GetMapping("/modificar")
    public String modificar(
            @RequestParam("idRepuesto") Long idRepuesto,
            Model model) throws SQLException {

        Repuesto repuesto = repuestoService.buscarPorId(idRepuesto);

        if (repuesto == null) {
            return "redirect:/repuestos/listado";
        }

        model.addAttribute("repuesto", repuesto);
        model.addAttribute("modoEdicion", true);

        return "repuestos/modifica";
    }

    // GUARDAR UN NUEVO REPUESTO
    @PostMapping("/guardar")
    public String guardar(
            Repuesto repuesto,
            RedirectAttributes redirectAttributes) {

        try {

            repuestoService.insertar(repuesto);

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Repuesto registrado correctamente.");

        } catch (SQLException | IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "No se pudo registrar el repuesto: "
                            + e.getMessage());
        }

        return "redirect:/repuestos/listado";
    }

    // ACTUALIZAR UN REPUESTO
    @PostMapping("/actualizar")
    public String actualizar(
            Repuesto repuesto,
            RedirectAttributes redirectAttributes) {

        try {

            repuestoService.modificar(repuesto);

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Repuesto actualizado correctamente.");

        } catch (SQLException | IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "No se pudo actualizar el repuesto: "
                            + e.getMessage());
        }

        return "redirect:/repuestos/listado";
    }

    // ELIMINAR UN REPUESTO
    @PostMapping("/eliminar")
    public String eliminar(
            @RequestParam("idRepuesto") Long idRepuesto,
            RedirectAttributes redirectAttributes) {

        try {

            repuestoService.eliminar(idRepuesto);

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Operacion de eliminacion realizada correctamente.");

        } catch (SQLException | IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "No se pudo eliminar el repuesto: "
                            + e.getMessage());
        }

        return "redirect:/repuestos/listado";
    }
}
