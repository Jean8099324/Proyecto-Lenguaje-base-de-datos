package com.rescatevial.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class FacturacionIncidente {
    private Long idVenta;
    private LocalDate fechaVenta;
    private BigDecimal totalVenta;
    private Long idFactura;
    private LocalDate fechaFactura;
    private String horaFactura;
    private BigDecimal subtotal;
    private BigDecimal deducciones;
    private BigDecimal impuestos;
    private BigDecimal totalFactura;
    private BigDecimal totalPagado;
    private String estado;
}
