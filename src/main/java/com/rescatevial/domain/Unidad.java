package com.rescatevial.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class Unidad {
    private Long id;
    @NotBlank @Size(max = 100) private String nombre;
    @NotNull private Long idTipo;
    @NotNull private Long idEstado;
    @Size(max = 20) private String placa;
    private String tipoNombre;
    private String estadoNombre;
}
