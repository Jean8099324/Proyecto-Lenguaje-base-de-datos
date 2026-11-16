package com.rescatevial.domain;

import java.util.List;
import lombok.Data;

@Data
public class VehiculoDetalleIncidente {
    private Long idCliente;
    private String nombreCliente;
    private String direccionCliente;
    private List<String> direccionesCliente = List.of();
    private String placa;
    private String codigoChasis;
    private String marca;
    private String modelo;
    private String anio;
}
