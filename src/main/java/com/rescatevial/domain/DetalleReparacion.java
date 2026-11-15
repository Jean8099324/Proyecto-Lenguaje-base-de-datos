
package com.rescatevial.domain;

import java.io.Serializable;
import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

/**
 * PROYECTO RESCATE VIAL CR
 *
 * CLASE PARA REPRESENTAR LOS DETALLES
 * DE LAS REPARACIONES.
 */
@Data
public class DetalleReparacion implements Serializable {

    private static final long serialVersionUID = 1L;

    // IDENTIFICADOR DE LA REPARACION
    @NotNull
    @Positive
    private Long idReparacion;

    // IDENTIFICADOR DEL REPUESTO
    @NotNull
    @Positive
    private Long idRepuesto;

    // CANTIDAD DE REPUESTOS UTILIZADOS
    @PositiveOrZero
    private BigDecimal cantidadRepuestos;

    // PRECIO UNITARIO APLICADO
    @PositiveOrZero
    private BigDecimal precioUnitarioAplicado;

    // ESTADO DEL DETALLE
    @NotNull
    @Positive
    private Long estado;

    // CALCULAR SUBTOTAL DEL DETALLE
    public BigDecimal getSubtotal() {

        if (cantidadRepuestos == null
                || precioUnitarioAplicado == null) {
            return BigDecimal.ZERO;
        }

        return cantidadRepuestos.multiply(
                precioUnitarioAplicado);
    }
}
