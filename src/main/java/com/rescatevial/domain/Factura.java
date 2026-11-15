
package com.rescatevial.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/*
 * PANTALLA 24: FACTURACION
 *
 * DESCRIPCION:
 * CLASE QUE REPRESENTA LOS DATOS
 * DE LA TABLA FIDE_FACTURAS_TB.
 *
 * PROYECTO: RESCATE VIAL CR
 */

public class Factura {

    // IDENTIFICADOR DE LA FACTURA
    private Long idFactura;

    // FECHA Y HORA DE FACTURACION
    private LocalDate fechaFactura;
    private String horaFactura;

    // IMPORTES DE LA FACTURA
    private BigDecimal subtotal;
    private BigDecimal totalDeducciones;
    private BigDecimal totalImpuestos;
    private BigDecimal totalFactura;

    // RELACIONES CON VENTAS Y ESTADOS
    private Long idVenta;
    private Long idEstado;

    // CAMPOS DE AUDITORIA
    private String creadoPor;
    private String modificadoPor;
    private String accion;
    private OffsetDateTime fechaCreacion;
    private OffsetDateTime fechaModificacion;

    // CONSTRUCTOR VACIO
    public Factura() {
    }

    // GETTERS Y SETTERS

    public Long getIdFactura() {
        return idFactura;
    }

    public void setIdFactura(Long idFactura) {
        this.idFactura = idFactura;
    }

    public LocalDate getFechaFactura() {
        return fechaFactura;
    }

    public void setFechaFactura(LocalDate fechaFactura) {
        this.fechaFactura = fechaFactura;
    }

    public String getHoraFactura() {
        return horaFactura;
    }

    public void setHoraFactura(String horaFactura) {
        this.horaFactura = horaFactura;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getTotalDeducciones() {
        return totalDeducciones;
    }

    public void setTotalDeducciones(BigDecimal totalDeducciones) {
        this.totalDeducciones = totalDeducciones;
    }

    public BigDecimal getTotalImpuestos() {
        return totalImpuestos;
    }

    public void setTotalImpuestos(BigDecimal totalImpuestos) {
        this.totalImpuestos = totalImpuestos;
    }

    public BigDecimal getTotalFactura() {
        return totalFactura;
    }

    public void setTotalFactura(BigDecimal totalFactura) {
        this.totalFactura = totalFactura;
    }

    public Long getIdVenta() {
        return idVenta;
    }

    public void setIdVenta(Long idVenta) {
        this.idVenta = idVenta;
    }

    public Long getIdEstado() {
        return idEstado;
    }

    public void setIdEstado(Long idEstado) {
        this.idEstado = idEstado;
    }

    public String getCreadoPor() {
        return creadoPor;
    }

    public void setCreadoPor(String creadoPor) {
        this.creadoPor = creadoPor;
    }

    public String getModificadoPor() {
        return modificadoPor;
    }

    public void setModificadoPor(String modificadoPor) {
        this.modificadoPor = modificadoPor;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(OffsetDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public OffsetDateTime getFechaModificacion() {
        return fechaModificacion;
    }

    public void setFechaModificacion(OffsetDateTime fechaModificacion) {
        this.fechaModificacion = fechaModificacion;
    }
}
