package com.rescatevial.domain;

import java.util.List;
import java.util.Map;

public record DashboardData(long totalIncidentes, long incidentesEnAtencion, long totalClientes,
                            long totalUnidades, long unidadesDisponibles, long unidadesAsignadas,
                            long unidadesEnServicio,
                            long unidadesFueraOperacion, List<Map<String, String>> incidentesRecientes) {
}
