package com.rescatevial.domain;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Datos de vehiculos; las etiquetas sirven para mostrar el listado. */
@Data
public class Vehiculo {
    private Long id;
    @Size(max = 10)
    private String placa;
    @Size(max = 17)
    private String codigoChasis;
    @NotNull @Positive
    private Long idMarca;
    @NotNull @Positive
    private Long idModelo;
    @NotNull @Positive
    private Long idAnio;
    private Long idEstado;
    @NotNull @Positive
    private Long idUsuario;

    private String nombreMarca;
    private String nombreModelo;
    private String anioTexto;
    private String nombrePropietario;
    private String nombreEstado;
}
