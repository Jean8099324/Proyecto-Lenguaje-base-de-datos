package com.rescatevial.domain;

import java.time.LocalDate;
import lombok.Data;

@Data
public class HistorialServicioIncidente {
    private String etapa;
    private String recurso;
    private String responsable;
    private String estado;
    private String detalle;
    private LocalDate fechaInicio;
    private String horaInicio;
    private LocalDate fechaFin;
    private String horaFin;
}
