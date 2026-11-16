
package com.rescatevial.controller;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.rescatevial.domain.Reparacion;
import com.rescatevial.domain.Usuario;
import com.rescatevial.service.EstadoService;
import com.rescatevial.service.ReparacionService;
import com.rescatevial.service.UsuarioService;

import jakarta.validation.Valid;

/**
 * Controlador para administrar las reparaciones
 * del sistema RESCATE VIAL CR.
 */
@Controller
@RequestMapping("/reparaciones")
public class ReparacionController {

    private final ReparacionService reparacionService;
    private final EstadoService estadoService;
    private final UsuarioService usuarioService;

    // INYECCION DE DEPENDENCIAS
    public ReparacionController(
            ReparacionService reparacionService,
            EstadoService estadoService,
            UsuarioService usuarioService) {

        this.reparacionService = reparacionService;
        this.estadoService = estadoService;
        this.usuarioService = usuarioService;
    }

    /*
     * CONSULTAR MECANICOS ACTIVOS
     * UTILIZANDO EL SERVICIO DE USUARIOS.
     *
     * ID_TIPO_USUARIO = 4 CORRESPONDE A MECANICO.
     */
    private List<Usuario> obtenerMecanicos() {

        return usuarioService.getUsuarios(true)
                .stream()
                .filter(usuario -> Long.valueOf(4L)
                        .equals(usuario.getTipoUsuario()))
                .toList();
    }

    // MOSTRAR LISTADO DE REPARACIONES
    @GetMapping("/listado")
    public String inicio(Model model) {

        var reparaciones = reparacionService.getReparaciones();

        model.addAttribute("reparaciones", reparaciones);
        model.addAttribute("totalReparaciones", reparaciones.size());

        // CONSULTAR ESTADOS MEDIANTE EL SERVICIO
        model.addAttribute("estados", estadoService.listar());

        // CONSULTAR MECANICOS ACTIVOS
        model.addAttribute("mecanicos", obtenerMecanicos());

        // PREPARAR FORMULARIO PARA NUEVA REPARACION
        if (!model.containsAttribute("reparacionForm")) {
            model.addAttribute("reparacionForm", new Reparacion());
        }

        return "reparaciones/listado";
    }

    // MOSTRAR EL FLUJO DE UNA REPARACION ESPECIFICA
    @GetMapping("/flujo/{id}")
    public String flujo(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        Reparacion reparacion = reparacionService
                .getReparacionPorId(id)
                .orElse(null);

        if (reparacion == null) {
            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    "No se encontro la reparacion indicada.");
            return "redirect:/reparaciones/listado";
        }

        List<String> ordenFlujo = List.of(
                "RECIBIDO EN TALLER",
                "REVISION INICIADA",
                "COTIZACION GENERADA",
                "COTIZACION APROBADA",
                "REPARACION EN PROCESO",
                "REPARACION FINALIZADA");
        List<com.rescatevial.domain.Estado> catalogoEstados = estadoService.listar();
        List<com.rescatevial.domain.Estado> estadosFlujo = ordenFlujo.stream()
                .map(nombre -> catalogoEstados.stream()
                        .filter(estado -> estado.getNombreEstado() != null
                                && nombre.equalsIgnoreCase(estado.getNombreEstado().trim()))
                        .findFirst()
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();

        int indiceEstadoActual = -1;
        for (int i = 0; i < estadosFlujo.size(); i++) {
            if (estadosFlujo.get(i).getId().equals(reparacion.getEstado())) {
                indiceEstadoActual = i;
                break;
            }
        }

        model.addAttribute("reparacion", reparacion);
        model.addAttribute("estadosFlujo", estadosFlujo);
        model.addAttribute("indiceEstadoActual", indiceEstadoActual);
        model.addAttribute("estadoActual", catalogoEstados.stream()
                .filter(estado -> estado.getId().equals(reparacion.getEstado()))
                .map(estado -> estado.getNombreEstado())
                .findFirst()
                .orElse("Estado sin clasificar"));

        return "reparaciones/flujo";
    }

    // MOSTRAR FORMULARIO PARA MODIFICAR
    @GetMapping("/modificar/{id}")
    public String modificar(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (!model.containsAttribute("reparacionForm")) {

            Reparacion reparacion = reparacionService
                    .getReparacionPorId(id)
                    .orElse(null);

            if (reparacion == null) {

                redirectAttributes.addFlashAttribute(
                        "mensajeError",
                        "No se encontró la reparación indicada.");

                return "redirect:/reparaciones/listado";
            }

            model.addAttribute("reparacionForm", reparacion);
        }

        // CONSULTAR ESTADOS
        model.addAttribute("estados", estadoService.listar());

        // CONSULTAR MECANICOS ACTIVOS
        model.addAttribute("mecanicos", obtenerMecanicos());

        return "reparaciones/modifica";
    }

    // GUARDAR O MODIFICAR REPARACION
    @PostMapping("/guardar")
    public String guardar(
            @Valid @ModelAttribute("reparacionForm") Reparacion reparacion,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        // VALIDAR CAMPOS DEL FORMULARIO
        if (bindingResult.hasErrors()) {

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    "Revise los campos requeridos y sus formatos.");

            redirectAttributes.addFlashAttribute(
                    "reparacionForm",
                    reparacion);

            // SI ES NUEVA REPARACION, REGRESAR AL MODAL
            if (reparacion.getId() == null) {

                redirectAttributes.addFlashAttribute(
                        "modalAbierto",
                        true);

                return "redirect:/reparaciones/listado";
            }

            // SI ES MODIFICACION, REGRESAR AL FORMULARIO
            return "redirect:/reparaciones/modificar/"
                    + reparacion.getId();
        }

        try {

            // GUARDAR UTILIZANDO EL SERVICIO DE REPARACIONES
            reparacionService.save(reparacion);

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    reparacion.getId() == null
                            ? "Reparación registrada correctamente."
                            : "Reparación actualizada correctamente.");

            return "redirect:/reparaciones/listado";

        } catch (DataIntegrityViolationException ex) {

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    "No se pudo guardar la reparación. "
                            + "Revise el incidente, mecánico y estado.");

        } catch (Exception ex) {

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    "No se pudo guardar la reparación: "
                            + ex.getMessage());
        }

        // CONSERVAR LOS DATOS DEL FORMULARIO
        redirectAttributes.addFlashAttribute(
                "reparacionForm",
                reparacion);

        if (reparacion.getId() != null) {

            return "redirect:/reparaciones/modificar/"
                    + reparacion.getId();
        }

        redirectAttributes.addFlashAttribute(
                "modalAbierto",
                true);

        return "redirect:/reparaciones/listado";
    }

    /*
     * ELIMINAR REPARACION
     * MEDIANTE CAMBIO DE ESTADO EN ORACLE.
     */
    @PostMapping("/eliminar/{id}")
    public String eliminar(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {

            reparacionService.delete(id);

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "Reparación eliminada correctamente.");

        } catch (Exception ex) {

            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    "No se pudo eliminar la reparación: "
                            + ex.getMessage());
        }

        return "redirect:/reparaciones/listado";
    }
}
