
package com.rescatevial.repository;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import com.rescatevial.domain.Factura;

/*
 * PANTALLA 24: FACTURACION
 *
 * DESCRIPCION:
 * REPOSITORIO ENCARGADO DE EJECUTAR
 * LOS PROCEDIMIENTOS ALMACENADOS
 * DE FACTURACION EN ORACLE.
 *
 * PROYECTO: RESCATE VIAL CR
 */

@Repository
public class FacturaRepository {

        // PAQUETE PL/SQL DE ORACLE
        private static final String PAQUETE = "FIDE_PROYECTO_FINAL_RESCATE_VIAL_CR_PCK";

        private final DataSource dataSource;

        public FacturaRepository(DataSource dataSource) {
                this.dataSource = dataSource;
        }

        // MAPEAR LOS DATOS DE ORACLE A JAVA
        private Factura mapearFactura(ResultSet rs)
                        throws SQLException {

                Factura factura = new Factura();

                factura.setIdFactura(
                                rs.getLong("ID_FACTURA"));

                Date fecha = rs.getDate("FECHA_FACTURA");

                if (fecha != null) {
                        factura.setFechaFactura(fecha.toLocalDate());
                }

                factura.setHoraFactura(
                                rs.getString("HORA_FACTURA"));

                factura.setSubtotal(
                                rs.getBigDecimal("SUBTOTAL"));

                factura.setTotalDeducciones(
                                rs.getBigDecimal("TOTAL_DEDUCCIONES"));

                factura.setTotalImpuestos(
                                rs.getBigDecimal("TOTAL_IMPUESTOS"));

                factura.setTotalFactura(
                                rs.getBigDecimal("TOTAL_FACTURA"));

                factura.setIdVenta(
                                rs.getLong("ID_VENTA"));

                factura.setIdEstado(
                                rs.getLong("ID_ESTADO"));

                return factura;
        }

        // CONSULTAR TODAS LAS FACTURAS
        public List<Factura> listar() throws SQLException {

                List<Factura> facturas = new ArrayList<>();

                String sql = "{call " + PAQUETE
                                + ".FIDE_OBTENER_FACTURAS_SP(?)}";

                try (
                                Connection conexion = dataSource.getConnection();
                                CallableStatement procedimiento = conexion.prepareCall(sql)) {

                        procedimiento.registerOutParameter(
                                        1, Types.REF_CURSOR);

                        procedimiento.execute();

                        try (ResultSet rs = (ResultSet) procedimiento.getObject(1)) {

                                while (rs.next()) {
                                        facturas.add(mapearFactura(rs));
                                }
                        }
                }

                return facturas;
        }

        // CONSULTAR FACTURA POR ID
        public Factura buscarPorId(Long idFactura)
                        throws SQLException {

                String sql = "{call " + PAQUETE
                                + ".FIDE_OBTENER_FACTURA_POR_ID_SP(?, ?)}";

                try (
                                Connection conexion = dataSource.getConnection();
                                CallableStatement procedimiento = conexion.prepareCall(sql)) {

                        procedimiento.setLong(1, idFactura);

                        procedimiento.registerOutParameter(
                                        2, Types.REF_CURSOR);

                        procedimiento.execute();

                        try (ResultSet rs = (ResultSet) procedimiento.getObject(2)) {

                                if (rs.next()) {
                                        return mapearFactura(rs);
                                }
                        }
                }

                return null;
        }

        // REGISTRAR NUEVA FACTURA
        public void insertar(Factura factura)
                        throws SQLException {

                String sql = "{call " + PAQUETE
                                + ".FIDE_INSERTAR_FACTURAS_SP(?, ?, ?, ?, ?, ?, ?, ?)}";

                try (
                                Connection conexion = dataSource.getConnection();
                                CallableStatement procedimiento = conexion.prepareCall(sql)) {

                        procedimiento.setDate(
                                        1,
                                        Date.valueOf(factura.getFechaFactura()));

                        procedimiento.setString(
                                        2, factura.getHoraFactura());

                        procedimiento.setBigDecimal(
                                        3, factura.getSubtotal());

                        procedimiento.setBigDecimal(
                                        4, factura.getTotalDeducciones());

                        procedimiento.setBigDecimal(
                                        5, factura.getTotalImpuestos());

                        procedimiento.setBigDecimal(
                                        6, factura.getTotalFactura());

                        procedimiento.setLong(
                                        7, factura.getIdVenta());

                        procedimiento.setLong(
                                        8, factura.getIdEstado());

                        procedimiento.execute();
                }
        }

        // MODIFICAR FACTURA EXISTENTE
        public void modificar(Factura factura)
                        throws SQLException {

                String sql = "{call " + PAQUETE
                                + ".FIDE_MODIFICAR_FACTURAS_SP(?, ?, ?, ?, ?, ?, ?, ?, ?)}";

                try (
                                Connection conexion = dataSource.getConnection();
                                CallableStatement procedimiento = conexion.prepareCall(sql)) {

                        procedimiento.setLong(
                                        1, factura.getIdFactura());

                        procedimiento.setDate(
                                        2,
                                        Date.valueOf(factura.getFechaFactura()));

                        procedimiento.setString(
                                        3, factura.getHoraFactura());

                        procedimiento.setBigDecimal(
                                        4, factura.getSubtotal());

                        procedimiento.setBigDecimal(
                                        5, factura.getTotalDeducciones());

                        procedimiento.setBigDecimal(
                                        6, factura.getTotalImpuestos());

                        procedimiento.setBigDecimal(
                                        7, factura.getTotalFactura());

                        procedimiento.setLong(
                                        8, factura.getIdVenta());

                        procedimiento.setLong(
                                        9, factura.getIdEstado());

                        procedimiento.execute();
                }
        }

        // ELIMINAR FACTURA
        public void eliminar(Long idFactura)
                        throws SQLException {

                String sql = "{call " + PAQUETE
                                + ".FIDE_ELIMINAR_FACTURAS_SP(?)}";

                try (
                                Connection conexion = dataSource.getConnection();
                                CallableStatement procedimiento = conexion.prepareCall(sql)) {

                        procedimiento.setLong(1, idFactura);

                        procedimiento.execute();
                }
        }
}
