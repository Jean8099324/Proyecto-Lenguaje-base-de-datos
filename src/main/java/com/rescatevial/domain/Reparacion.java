package com.rescatevial.domain;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class Reparacion implements Serializable {

    private static final long serialVersionUID = 1L;

    // IDENTIFICADOR DE LA REPARACION
    @Positive
    private Long id;

    // DESCRIPCION DE LA REPARACION
    @Size(max = 500)
    private String descripcionReparacion;

    // INFORMACION DEL SERVICIO
    @PositiveOrZero
    private BigDecimal horasTrabajadas;

    @PositiveOrZero
    private BigDecimal tarifaHora;

    @PositiveOrZero
    private BigDecimal costoServicio;

    // FECHAS DE LA REPARACION
    private LocalDate fechaIngresoTaller;
    private LocalDate fechaInicioReparacion;
    private LocalDate fechaFinReparacion;
    private LocalDate fechaSalidaTaller;

    // RELACIONES CON OTRAS TABLAS
    @NotNull
    @Positive
    private Long idIncidente;

    @NotNull
    @Positive
    private Long estado;

    @NotNull
    @Positive
    private Long idMecanico;

}
