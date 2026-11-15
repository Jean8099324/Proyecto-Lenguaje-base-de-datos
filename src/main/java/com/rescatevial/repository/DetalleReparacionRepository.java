
package com.rescatevial.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import com.rescatevial.domain.DetalleReparacion;

/**
 * PROYECTO RESCATE VIAL CR
 *
 * REPOSITORIO PARA ADMINISTRAR LOS DETALLES
 * DE REPARACIONES MEDIANTE PROCEDIMIENTOS
 * ALMACENADOS DEL PAQUETE ORACLE.
 */
@Repository
public class DetalleReparacionRepository {

    private final SimpleJdbcCall insertarDetalle;
    private final SimpleJdbcCall modificarDetalle;
    private final SimpleJdbcCall eliminarDetalle;
    private final SimpleJdbcCall obtenerDetalles;

    // CONFIGURACION DE LOS PROCEDIMIENTOS ORACLE
    public DetalleReparacionRepository(JdbcTemplate jdbc) {

        String paquete = "FIDE_PROYECTO_FINAL_RESCATE_VIAL_CR_PCK";

        // INSERTAR DETALLE
        this.insertarDetalle = new SimpleJdbcCall(jdbc)
                .withCatalogName(paquete)
                .withProcedureName(
                        "FIDE_INSERTAR_DETALLE_REPARACIONES_SP")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(parametrosDetalle());

        // MODIFICAR DETALLE
        this.modificarDetalle = new SimpleJdbcCall(jdbc)
                .withCatalogName(paquete)
                .withProcedureName(
                        "FIDE_MODIFICAR_DETALLE_REPARACIONES_SP")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(parametrosDetalle());

        // ELIMINAR DETALLE
        this.eliminarDetalle = new SimpleJdbcCall(jdbc)
                .withCatalogName(paquete)
                .withProcedureName(
                        "FIDE_ELIMINAR_DETALLE_REPARACIONES_SP")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter(
                                "P_ID_REPARACION", Types.NUMERIC),
                        new SqlParameter(
                                "P_ID_REPUESTO", Types.NUMERIC));

        // CONSULTAR TODOS LOS DETALLES
        this.obtenerDetalles = new SimpleJdbcCall(jdbc)
                .withCatalogName(paquete)
                .withProcedureName(
                        "FIDE_OBTENER_DETALLE_REPARACIONES_SP")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlOutParameter(
                                "P_RESULTADO",
                                Types.REF_CURSOR,
                                (rs, rowNum) -> mapDetalle(rs)));
    }

    // PARAMETROS PARA INSERTAR Y MODIFICAR
    private static SqlParameter[] parametrosDetalle() {

        return new SqlParameter[] {
                new SqlParameter(
                        "P_ID_REPARACION", Types.NUMERIC),
                new SqlParameter(
                        "P_ID_REPUESTO", Types.NUMERIC),
                new SqlParameter(
                        "P_CANTIDAD_REPUESTOS", Types.NUMERIC),
                new SqlParameter(
                        "P_PRECIO_UNITARIO_APLICADO", Types.NUMERIC),
                new SqlParameter(
                        "P_ID_ESTADO", Types.NUMERIC)
        };
    }

    // CONSTRUIR PARAMETROS DEL DETALLE
    private static MapSqlParameterSource parametros(
            DetalleReparacion detalle) {

        return new MapSqlParameterSource()
                .addValue(
                        "P_ID_REPARACION",
                        detalle.getIdReparacion())
                .addValue(
                        "P_ID_REPUESTO",
                        detalle.getIdRepuesto())
                .addValue(
                        "P_CANTIDAD_REPUESTOS",
                        detalle.getCantidadRepuestos())
                .addValue(
                        "P_PRECIO_UNITARIO_APLICADO",
                        detalle.getPrecioUnitarioAplicado())
                .addValue(
                        "P_ID_ESTADO",
                        detalle.getEstado());
    }

    // INSERTAR DETALLE MEDIANTE PROCEDIMIENTO
    public void insertUsingProcedure(
            DetalleReparacion detalle) {

        insertarDetalle.execute(parametros(detalle));
    }

    // MODIFICAR DETALLE MEDIANTE PROCEDIMIENTO
    public void updateUsingProcedure(
            DetalleReparacion detalle) {

        modificarDetalle.execute(parametros(detalle));
    }

    // ELIMINAR DETALLE MEDIANTE PROCEDIMIENTO
    public void deleteById(
            Long idReparacion,
            Long idRepuesto) {

        eliminarDetalle.execute(
                new MapSqlParameterSource()
                        .addValue(
                                "P_ID_REPARACION",
                                idReparacion)
                        .addValue(
                                "P_ID_REPUESTO",
                                idRepuesto));
    }

    // CONSULTAR TODOS LOS DETALLES
    public List<DetalleReparacion> findAll() {

        Map<String, Object> resultado = obtenerDetalles.execute(
                new MapSqlParameterSource());

        return cursor(resultado);
    }

    // BUSCAR DETALLE POR SU CLAVE COMPUESTA
    public Optional<DetalleReparacion> findById(
            Long idReparacion,
            Long idRepuesto) {

        return findAll().stream()
                .filter(detalle -> idReparacion.equals(
                        detalle.getIdReparacion())
                        && idRepuesto.equals(
                                detalle.getIdRepuesto()))
                .findFirst();
    }

    // CONVERTIR RESULTADO ORACLE A OBJETO JAVA
    private static DetalleReparacion mapDetalle(
            ResultSet rs) throws SQLException {

        DetalleReparacion detalle = new DetalleReparacion();

        detalle.setIdReparacion(
                rs.getLong("ID_REPARACION"));

        detalle.setIdRepuesto(
                rs.getLong("ID_REPUESTO"));

        detalle.setCantidadRepuestos(
                rs.getBigDecimal("CANTIDAD_REPUESTOS"));

        detalle.setPrecioUnitarioAplicado(
                rs.getBigDecimal("PRECIO_UNITARIO_APLICADO"));

        detalle.setEstado(
                rs.getLong("ID_ESTADO"));

        return detalle;
    }

    // OBTENER REGISTROS DEL CURSOR ORACLE
    @SuppressWarnings("unchecked")
    private static <T> List<T> cursor(
            Map<String, Object> resultado) {

        List<T> registros = (List<T>) resultado.get("P_RESULTADO");

        return registros == null
                ? List.of()
                : registros;
    }
}
