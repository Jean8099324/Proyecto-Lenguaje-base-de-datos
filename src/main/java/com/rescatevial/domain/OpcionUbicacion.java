package com.rescatevial.domain;

import lombok.Data;

@Data
public class OpcionUbicacion {
    private Long id;
    private String nombre;
    private Long idPadre;
    private Long idEstado;
}
