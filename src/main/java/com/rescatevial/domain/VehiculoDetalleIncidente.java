package com.rescatevial.domain;

import lombok.Data;

@Data
public class VehiculoDetalleIncidente {
    private String placa;
    private String codigoChasis;
    private String marca;
    private String modelo;
    private String anio;
}
