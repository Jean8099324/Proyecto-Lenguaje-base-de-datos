
package com.rescatevial.service;

import com.rescatevial.repository.DashboardRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final DashboardRepository dashboardRepository;

    public DashboardService(DashboardRepository dashboardRepository) {
        this.dashboardRepository = dashboardRepository;
    }

    @Transactional(readOnly = true)
    public long contarIncidentes() {
        return dashboardRepository.contarIncidentes();
    }

    @Transactional(readOnly = true)
    public long contarIncidentesEnAtencion() {
        return dashboardRepository.contarIncidentesEnAtencion();
    }

    @Transactional(readOnly = true)
    public List<Map<String, String>> obtenerIncidentesRecientes() {
        return dashboardRepository.obtenerIncidentesRecientes();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> obtenerEstadosUnidades() {
        return dashboardRepository.obtenerEstadosUnidades();
    }

    @Transactional(readOnly = true)
    public long contarUnidades() {
        return dashboardRepository.obtenerEstadosUnidades().values().stream()
                .mapToLong(valor -> valor.longValue())
                .sum();
    }
}
