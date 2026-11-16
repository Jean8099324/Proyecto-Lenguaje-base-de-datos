package com.rescatevial.domain;

import lombok.Data;

@Data
public class VehiculoDetalleIncidente {
    private Long idCliente;
    private String nombreCliente;
    private String direccionCliente;
    private String placa;
    private String codigoChasis;
    private String marca;
    private String modelo;
    private String anio;
}
