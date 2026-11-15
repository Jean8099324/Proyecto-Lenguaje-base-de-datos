
package com.rescatevial.repository;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import com.rescatevial.domain.Repuesto;

/*
 * PANTALLA 22: REPUESTOS
 * DESCRIPCION: REPOSITORIO PARA EJECUTAR LOS PROCEDIMIENTOS
 * ALMACENADOS DE REPUESTOS EN ORACLE.
 *
 * TODAS LAS OPERACIONES SE REALIZAN MEDIANTE EL PAQUETE
 * FIDE_PROYECTO_FINAL_RESCATE_VIAL_CR_PCK.
 */

@Repository
public class RepuestoRepository {

    private static final String PAQUETE = "FIDE_PROYECTO_FINAL_RESCATE_VIAL_CR_PCK";

    private final DataSource dataSource;

    public RepuestoRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // CONVERTIR UN REGISTRO DE ORACLE EN UN OBJETO REPUESTO
    private Repuesto mapearRepuesto(ResultSet rs) throws SQLException {

        Repuesto repuesto = new Repuesto();

        repuesto.setIdRepuesto(rs.getLong("ID_REPUESTO"));
        repuesto.setNombreRepuesto(rs.getString("NOMBRE_REPUESTO"));
        repuesto.setDescripcionRepuesto(
                rs.getString("DESCRIPCION_REPUESTO"));
        repuesto.setPrecioUnitario(rs.getBigDecimal("PRECIO_UNITARIO"));
        repuesto.setIdEstado(rs.getLong("ID_ESTADO"));

        return repuesto;
    }

    // LISTAR TODOS LOS REPUESTOS
    public List<Repuesto> listar() throws SQLException {

        List<Repuesto> repuestos = new ArrayList<>();

        String sql = "{call " + PAQUETE
                + ".FIDE_OBTENER_REPUESTOS_SP(?)}";

        try (Connection conexion = dataSource.getConnection();
                CallableStatement cs = conexion.prepareCall(sql)) {

            cs.registerOutParameter(1, Types.REF_CURSOR);
            cs.execute();

            try (ResultSet rs = (ResultSet) cs.getObject(1)) {

                while (rs.next()) {
                    repuestos.add(mapearRepuesto(rs));
                }
            }
        }

        return repuestos;
    }

    // CONSULTAR UN REPUESTO POR ID
    public Repuesto buscarPorId(Long idRepuesto) throws SQLException {

        String sql = "{call " + PAQUETE
                + ".FIDE_OBTENER_REPUESTOS_ID_SP(?, ?)}";

        try (Connection conexion = dataSource.getConnection();
                CallableStatement cs = conexion.prepareCall(sql)) {

            cs.setLong(1, idRepuesto);
            cs.registerOutParameter(2, Types.REF_CURSOR);
            cs.execute();

            try (ResultSet rs = (ResultSet) cs.getObject(2)) {

                if (rs.next()) {
                    return mapearRepuesto(rs);
                }
            }
        }

        return null;
    }

    // INSERTAR UN REPUESTO
    public void insertar(Repuesto repuesto) throws SQLException {

        String sql = "{call " + PAQUETE
                + ".FIDE_INSERTAR_REPUESTOS_SP(?, ?, ?, ?)}";

        try (Connection conexion = dataSource.getConnection();
                CallableStatement cs = conexion.prepareCall(sql)) {

            cs.setString(1, repuesto.getNombreRepuesto());
            cs.setString(2, repuesto.getDescripcionRepuesto());
            cs.setBigDecimal(3, repuesto.getPrecioUnitario());
            cs.setLong(4, repuesto.getIdEstado());

            cs.execute();
        }
    }

    // MODIFICAR UN REPUESTO
    public void modificar(Repuesto repuesto) throws SQLException {

        String sql = "{call " + PAQUETE
                + ".FIDE_MODIFICAR_REPUESTOS_SP(?, ?, ?, ?, ?)}";

        try (Connection conexion = dataSource.getConnection();
                CallableStatement cs = conexion.prepareCall(sql)) {

            cs.setLong(1, repuesto.getIdRepuesto());
            cs.setString(2, repuesto.getNombreRepuesto());
            cs.setString(3, repuesto.getDescripcionRepuesto());
            cs.setBigDecimal(4, repuesto.getPrecioUnitario());
            cs.setLong(5, repuesto.getIdEstado());

            cs.execute();
        }
    }

    // ELIMINAR UN REPUESTO
    public void eliminar(Long idRepuesto) throws SQLException {

        String sql = "{call " + PAQUETE
                + ".FIDE_ELIMINAR_REPUESTOS_SP(?)}";

        try (Connection conexion = dataSource.getConnection();
                CallableStatement cs = conexion.prepareCall(sql)) {

            cs.setLong(1, idRepuesto);
            cs.execute();
        }
    }
}
