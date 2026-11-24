package com.rescatevial.controller;

import com.rescatevial.service.DashboardService;
import java.util.Locale;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

    private final DashboardService dashboardService;

    public IndexController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/")
    public String inicio() {
        return "auth/login";
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {

        var datos = dashboardService.cargarDashboard();
        long totalUnidades = datos.totalUnidades();
        long disponibles = datos.unidadesDisponibles();
        long asignadas = datos.unidadesAsignadas();
        long enServicio = datos.unidadesEnServicio();
        model.addAttribute("clientesRegistrados", datos.totalClientes());
        model.addAttribute("incidentesRegistrados", datos.totalIncidentes());
        model.addAttribute("unidadesRegistradas", totalUnidades);
        model.addAttribute("incidentesEnAtencion", datos.incidentesEnAtencion());
        model.addAttribute("incidentesRecientes", datos.incidentesRecientes());
        long fueraOperacion = datos.unidadesFueraOperacion();

        model.addAttribute("unidadesDisponibles", disponibles);
        model.addAttribute("unidadesAsignadas", asignadas);
        model.addAttribute("unidadesEnServicio", enServicio);
        model.addAttribute("unidadesFueraOperacion", fueraOperacion);

        // Conserva los tres colores originales; solo calcula proporciones reales.
        double anguloDisponible = totalUnidades > 0 ? 360.0 * disponibles / totalUnidades : 0;
        double anguloAsignadas = totalUnidades > 0
                ? 360.0 * (disponibles + asignadas) / totalUnidades : 0;
        double anguloServicio = totalUnidades > 0
                ? 360.0 * (disponibles + asignadas + enServicio) / totalUnidades : 0;
        String estiloGrafico = String.format(Locale.ROOT,
                "background: conic-gradient(#2c9b5f 0deg %.3fdeg, "
                + "#e2a526 %.3fdeg %.3fdeg, #3979c5 %.3fdeg %.3fdeg, "
                + "#d9dee3 %.3fdeg 360deg)",
                anguloDisponible, anguloDisponible, anguloAsignadas, anguloAsignadas,
                anguloServicio, anguloServicio);
        model.addAttribute("estiloGraficoUnidades", estiloGrafico);

        return "dashboard/index";
    }
}
