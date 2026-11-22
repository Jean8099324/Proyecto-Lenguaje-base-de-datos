package com.rescatevial.controller;

import com.rescatevial.service.DespachoService;
import com.rescatevial.service.UsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/despacho")
public class DespachoController {
    private final DespachoService despachoService;
    private final UsuarioService usuarioService;

    public DespachoController(DespachoService despachoService, UsuarioService usuarioService) {
        this.despachoService = despachoService;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String pantalla(@RequestParam(defaultValue = "1") int pagina, Model model) {
        int tamanioPagina = 10;
        int paginaSolicitada = Math.max(1, pagina);
        var resultado = despachoService.pendientes(paginaSolicitada, tamanioPagina);
        long totalPendientes = resultado.total();
        int totalPaginas = (int) Math.max(1, (totalPendientes + tamanioPagina - 1) / tamanioPagina);
        int paginaActual = Math.min(paginaSolicitada, totalPaginas);
        if (paginaActual != paginaSolicitada) {
            resultado = despachoService.pendientes(paginaActual, tamanioPagina);
        }
        var unidades = despachoService.unidadesDisponibles();
        model.addAttribute("incidentesPendientes", resultado.incidentes());
        model.addAttribute("unidadesDisponibles", unidades);
        model.addAttribute("totalPendientes", totalPendientes);
        model.addAttribute("paginaActual", paginaActual);
        model.addAttribute("totalPaginas", totalPaginas);
        model.addAttribute("inicioRegistro", totalPendientes == 0 ? 0 : (paginaActual - 1) * tamanioPagina + 1);
        model.addAttribute("finRegistro", Math.min((long) paginaActual * tamanioPagina, totalPendientes));
        model.addAttribute("totalUnidades", unidades.size());
        return "despacho/listado";
    }

    @PostMapping("/asignar")
    public String asignar(@RequestParam Long idIncidente,
                          @RequestParam Long idUnidad,
                          @RequestParam(required = false) String observaciones,
                          Authentication authentication,
                          RedirectAttributes redirect) {
        try {
            var responsible = usuarioService.getUsuarioPorUsername(authentication.getName())
                    .orElseThrow(() -> new IllegalStateException("No fue posible identificar al responsable del despacho."));
            despachoService.asignar(idIncidente, idUnidad, responsible.getId(), observaciones);
            redirect.addFlashAttribute("mensajeExito", "Unidad asignada correctamente al incidente #"
                    + String.format("%06d", idIncidente) + ".");
        } catch (Exception ex) {
            redirect.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/despacho";
    }
}
