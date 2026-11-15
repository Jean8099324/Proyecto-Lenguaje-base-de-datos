
package com.rescatevial.controller;

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

import com.rescatevial.domain.DetalleReparacion;
import com.rescatevial.service.DetalleReparacionService;
import com.rescatevial.service.EstadoService;
import com.rescatevial.service.ReparacionService;

import jakarta.validation.Valid;

/**
 * PROYECTO RESCATE VIAL CR
 *
 * CONTROLADOR DE LA PANTALLA 21
 * DETALLE DE REPARACIONES.
 */
@Controller
@RequestMapping("/detalle-reparaciones")
public class DetalleReparacionController {

        private final DetalleReparacionService detalleService;
        private final ReparacionService reparacionService;
        private final EstadoService estadoService;

        // INYECCION DE DEPENDENCIAS
        public DetalleReparacionController(
                        DetalleReparacionService detalleService,
                        ReparacionService reparacionService,
                        EstadoService estadoService) {

                this.detalleService = detalleService;
                this.reparacionService = reparacionService;
                this.estadoService = estadoService;
        }

        // MOSTRAR LISTADO DE DETALLES
        @GetMapping("/listado")
        public String listado(Model model) {

                model.addAttribute(
                                "detalles",
                                detalleService.getDetalles());

                model.addAttribute(
                                "reparaciones",
                                reparacionService.getReparaciones());

                model.addAttribute(
                                "estados",
                                estadoService.listar());

                model.addAttribute(
                                "detalle",
                                new DetalleReparacion());

                return "detalle-reparaciones/listado";
        }

        // MOSTRAR FORMULARIO PARA MODIFICAR
        @GetMapping("/modificar/{idReparacion}/{idRepuesto}")
        public String modificar(
                        @PathVariable Long idReparacion,
                        @PathVariable Long idRepuesto,
                        Model model,
                        RedirectAttributes redirect) {

                var detalle = detalleService.getDetallePorId(
                                idReparacion, idRepuesto);

                if (detalle.isEmpty()) {

                        redirect.addFlashAttribute(
                                        "error",
                                        "No se encontró el detalle solicitado.");

                        return "redirect:/detalle-reparaciones/listado";
                }

                model.addAttribute("detalle", detalle.get());

                model.addAttribute(
                                "estados",
                                estadoService.listar());

                return "detalle-reparaciones/modifica";
        }

        // GUARDAR DETALLE NUEVO O MODIFICADO
        @PostMapping("/guardar")
        public String guardar(
                        @Valid @ModelAttribute("detalle") DetalleReparacion detalle,
                        BindingResult resultado,
                        @RequestParam(name = "edicion", defaultValue = "false") boolean edicion,
                        RedirectAttributes redirect) {

                if (resultado.hasErrors()) {

                        redirect.addFlashAttribute(
                                        "error",
                                        "Verifica los datos ingresados.");

                        return "redirect:/detalle-reparaciones/listado";
                }

                try {

                        if (edicion) {

                                detalleService.modificar(detalle);

                                redirect.addFlashAttribute(
                                                "exito",
                                                "Detalle actualizado correctamente.");

                        } else {

                                detalleService.insertar(detalle);

                                redirect.addFlashAttribute(
                                                "exito",
                                                "Detalle registrado correctamente.");
                        }

                } catch (Exception e) {

                        redirect.addFlashAttribute(
                                        "error",
                                        "No fue posible guardar el detalle: "
                                                        + e.getMessage());
                }

                return "redirect:/detalle-reparaciones/listado";
        }

        // ELIMINAR DETALLE
        @PostMapping("/eliminar/{idReparacion}/{idRepuesto}")
        public String eliminar(
                        @PathVariable Long idReparacion,
                        @PathVariable Long idRepuesto,
                        RedirectAttributes redirect) {

                try {

                        detalleService.eliminar(
                                        idReparacion,
                                        idRepuesto);

                        redirect.addFlashAttribute(
                                        "exito",
                                        "Detalle eliminado correctamente.");

                } catch (Exception e) {

                        redirect.addFlashAttribute(
                                        "error",
                                        "No fue posible eliminar el detalle: "
                                                        + e.getMessage());
                }

                return "redirect:/detalle-reparaciones/listado";
        }
}
