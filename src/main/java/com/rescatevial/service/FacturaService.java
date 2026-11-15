
package com.rescatevial.service;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

import org.springframework.stereotype.Service;

import com.rescatevial.domain.Factura;
import com.rescatevial.repository.FacturaRepository;

/*
 * PANTALLA 24: FACTURACION
 *
 * DESCRIPCION:
 * SERVICIO ENCARGADO DE GESTIONAR
 * LAS OPERACIONES CRUD DE FACTURAS
 * Y VALIDAR LOS IMPORTES.
 *
 * PROYECTO: RESCATE VIAL CR
 */

@Service
public class FacturaService {

        private final FacturaRepository facturaRepository;

        public FacturaService(FacturaRepository facturaRepository) {
                this.facturaRepository = facturaRepository;
        }

        // CONSULTAR TODAS LAS FACTURAS
        public List<Factura> listar() throws SQLException {
                return facturaRepository.listar();
        }

        // CONSULTAR FACTURA POR ID
        public Factura buscarPorId(Long idFactura)
                        throws SQLException {

                if (idFactura == null || idFactura <= 0) {
                        throw new IllegalArgumentException(
                                        "Debe indicar un ID de factura valido.");
                }

                return facturaRepository.buscarPorId(idFactura);
        }

        // REGISTRAR UNA NUEVA FACTURA
        public void insertar(Factura factura)
                        throws SQLException {

                validarFactura(factura);

                facturaRepository.insertar(factura);
        }

        // MODIFICAR UNA FACTURA EXISTENTE
        public void modificar(Factura factura)
                        throws SQLException {

                if (factura == null
                                || factura.getIdFactura() == null
                                || factura.getIdFactura() <= 0) {

                        throw new IllegalArgumentException(
                                        "Debe indicar el ID de la factura.");
                }

                validarFactura(factura);

                facturaRepository.modificar(factura);
        }

        // ELIMINAR FACTURA
        public void eliminar(Long idFactura)
                        throws SQLException {

                if (idFactura == null || idFactura <= 0) {
                        throw new IllegalArgumentException(
                                        "Debe indicar un ID de factura valido.");
                }

                facturaRepository.eliminar(idFactura);
        }

        // VALIDAR LOS DATOS DE LA FACTURA
        private void validarFactura(Factura factura) {

                if (factura == null) {
                        throw new IllegalArgumentException(
                                        "Los datos de la factura son obligatorios.");
                }

                // VALIDAR FECHA
                if (factura.getFechaFactura() == null) {
                        throw new IllegalArgumentException(
                                        "Debe ingresar la fecha de la factura.");
                }

                // VALIDAR HORA
                if (factura.getHoraFactura() == null
                                || factura.getHoraFactura().isBlank()) {

                        throw new IllegalArgumentException(
                                        "Debe ingresar la hora de la factura.");
                }

                // VALIDAR IMPORTES
                validarImporte(factura.getSubtotal(), "subtotal");

                validarImporte(
                                factura.getTotalDeducciones(),
                                "total de deducciones");

                validarImporte(
                                factura.getTotalImpuestos(),
                                "total de impuestos");

                validarImporte(
                                factura.getTotalFactura(),
                                "total de factura");

                // VALIDAR RELACION CON VENTA
                if (factura.getIdVenta() == null
                                || factura.getIdVenta() <= 0) {

                        throw new IllegalArgumentException(
                                        "Debe seleccionar una venta valida.");
                }

                // VALIDAR ESTADO
                if (factura.getIdEstado() == null
                                || factura.getIdEstado() <= 0) {

                        throw new IllegalArgumentException(
                                        "Debe seleccionar un estado valido.");
                }

                // VERIFICAR COHERENCIA DE IMPORTES
                BigDecimal totalCalculado = factura.getSubtotal()
                                .subtract(factura.getTotalDeducciones())
                                .add(factura.getTotalImpuestos());

                if (totalCalculado.compareTo(
                                factura.getTotalFactura()) != 0) {

                        throw new IllegalArgumentException(
                                        "El total de la factura debe ser igual "
                                                        + "al subtotal menos las deducciones "
                                                        + "mas los impuestos.");
                }
        }

        // VALIDAR QUE LOS IMPORTES NO SEAN NEGATIVOS
        private void validarImporte(
                        BigDecimal importe,
                        String nombreCampo) {

                if (importe == null) {
                        throw new IllegalArgumentException(
                                        "Debe ingresar el " + nombreCampo + ".");
                }

                if (importe.compareTo(BigDecimal.ZERO) < 0) {
                        throw new IllegalArgumentException(
                                        "El " + nombreCampo
                                                        + " no puede ser negativo.");
                }
        }
}
