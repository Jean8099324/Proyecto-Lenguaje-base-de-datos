package com.rescatevial.domain;

import lombok.Data;

/** Opcion de marca, modelo o anio en los formularios. */
@Data
public class OpcionVehiculo {
    private Long id;
    private String nombre;
    private Long padreId;
    private Long estadoId;
}
