package com.rescatevial.domain;

import java.time.LocalDate;
import lombok.Data;

@Data
public class IncidentePendienteDespacho {
    private Long id;
    private LocalDate fechaReporte;
    private String horaReporte;
    private String placaVehiculo;
    private String tipo;
    private String prioridad;
    private String ubicacion;
    private String detalle;
}
