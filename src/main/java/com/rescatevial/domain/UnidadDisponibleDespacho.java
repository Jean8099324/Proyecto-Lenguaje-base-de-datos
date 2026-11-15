package com.rescatevial.domain;

import lombok.Data;

@Data
public class UnidadDisponibleDespacho {
    private Long id;
    private String nombre;
    private String placa;
    private String tipo;

    public String getEtiqueta() {
        String identificacion = placa == null || placa.isBlank() ? nombre : nombre + " · " + placa;
        return identificacion + " · " + tipo;
    }
}
