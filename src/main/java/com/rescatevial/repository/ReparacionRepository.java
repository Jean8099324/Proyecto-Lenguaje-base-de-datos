
package com.rescatevial.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import com.rescatevial.domain.Reparacion;

/**
 * Operaciones de reparaciones mediante procedimientos
 * almacenados del PACKAGE de Oracle.
 */
@Repository
public class ReparacionRepository {

    private final SimpleJdbcCall insertarReparacion;
    private final SimpleJdbcCall modificarReparacion;
    private final SimpleJdbcCall eliminarReparacion;
    private final SimpleJdbcCall obtenerReparaciones;
    private final SimpleJdbcCall obtenerReparacionPorId;

    public ReparacionRepository(JdbcTemplate jdbc) {

        String paquete = "FIDE_PROYECTO_FINAL_RESCATE_VIAL_CR_PCK";

        // PROCEDIMIENTO PARA INSERTAR REPARACIONES
        this.insertarReparacion = new SimpleJdbcCall(jdbc)
                .withCatalogName(paquete)
                .withProcedureName("FIDE_INSERTAR_REPARACIONES_SP")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        parametrosReparacion());

        // PROCEDIMIENTO PARA MODIFICAR REPARACIONES
        this.modificarReparacion = new SimpleJdbcCall(jdbc)
                .withCatalogName(paquete)
                .withProcedureName("FIDE_MODIFICAR_REPARACIONES_SP")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        parametrosModificarReparacion());

        // PROCEDIMIENTO PARA ELIMINAR REPARACIONES
        this.eliminarReparacion = new SimpleJdbcCall(jdbc)
                .withCatalogName(paquete)
                .withProcedureName("FIDE_ELIMINAR_REPARACIONES_SP")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("P_ID_REPARACION", Types.NUMERIC));

        // PROCEDIMIENTO PARA OBTENER TODAS LAS REPARACIONES
        this.obtenerReparaciones = new SimpleJdbcCall(jdbc)
                .withCatalogName(paquete)
                .withProcedureName("FIDE_OBTENER_REPARACIONES_SP")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlOutParameter(
                                "P_RESULTADO",
                                Types.REF_CURSOR,
                                (rs, rowNum) -> mapReparacion(rs)));

        // PROCEDIMIENTO PARA OBTENER REPARACION POR ID
        this.obtenerReparacionPorId = new SimpleJdbcCall(jdbc)
                .withCatalogName(paquete)
                .withProcedureName("FIDE_OBTENER_REPARACION_POR_ID_SP")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("P_ID_REPARACION", Types.NUMERIC),
                        new SqlOutParameter(
                                "P_RESULTADO",
                                Types.REF_CURSOR,
                                (rs, rowNum) -> mapReparacion(rs)));
    }

    // PARAMETROS PARA INSERTAR
    private static SqlParameter[] parametrosReparacion() {
        return new SqlParameter[] {
                new SqlParameter("P_DESCRIPCION_REPARACION", Types.VARCHAR),
                new SqlParameter("P_HORAS_TRABAJADAS", Types.NUMERIC),
                new SqlParameter("P_TARIFA_HORA", Types.NUMERIC),
                new SqlParameter("P_COSTO_SERVICIO", Types.NUMERIC),
                new SqlParameter("P_FECHA_INGRESO_TALLER", Types.DATE),
                new SqlParameter("P_FECHA_INICIO_REPARACION", Types.DATE),
                new SqlParameter("P_FECHA_FIN_REPARACION", Types.DATE),
                new SqlParameter("P_FECHA_SALIDA_TALLER", Types.DATE),
                new SqlParameter("P_ID_INCIDENTE", Types.NUMERIC),
                new SqlParameter("P_ID_ESTADO", Types.NUMERIC),
                new SqlParameter("P_ID_MECANICO", Types.NUMERIC)
        };
    }

    // PARAMETROS PARA MODIFICAR
    private static SqlParameter[] parametrosModificarReparacion() {
        SqlParameter[] originales = parametrosReparacion();
        SqlParameter[] parametros = new SqlParameter[originales.length + 1];

        parametros[0] = new SqlParameter("P_ID_REPARACION", Types.NUMERIC);
        System.arraycopy(originales, 0, parametros, 1, originales.length);

        return parametros;
    }

    // INSERTAR REPARACION
    public void insertUsingProcedure(Reparacion reparacion) {
        insertarReparacion.execute(parametros(reparacion));
    }

    // MODIFICAR REPARACION
    public void updateUsingProcedure(Reparacion reparacion) {
        MapSqlParameterSource params = parametros(reparacion)
                .addValue("P_ID_REPARACION", reparacion.getId());

        modificarReparacion.execute(params);
    }

    // ELIMINAR REPARACION (CAMBIO DE ESTADO)
    public void deleteById(Long id) {
        eliminarReparacion.execute(
                new MapSqlParameterSource("P_ID_REPARACION", id));
    }

    // OBTENER TODAS LAS REPARACIONES
    public List<Reparacion> findAll() {
        return cursor(
                obtenerReparaciones.execute(new MapSqlParameterSource()));
    }

    // OBTENER REPARACION POR ID
    public Optional<Reparacion> findById(Long id) {
        List<Reparacion> resultado = cursor(
                obtenerReparacionPorId.execute(
                        new MapSqlParameterSource("P_ID_REPARACION", id)));

        return resultado.stream().findFirst();
    }

    // CONSTRUIR PARAMETROS DE LOS PROCEDIMIENTOS
    private static MapSqlParameterSource parametros(Reparacion reparacion) {
        return new MapSqlParameterSource()
                .addValue("P_DESCRIPCION_REPARACION",
                        reparacion.getDescripcionReparacion())
                .addValue("P_HORAS_TRABAJADAS",
                        reparacion.getHorasTrabajadas())
                .addValue("P_TARIFA_HORA",
                        reparacion.getTarifaHora())
                .addValue("P_COSTO_SERVICIO",
                        reparacion.getCostoServicio())
                .addValue("P_FECHA_INGRESO_TALLER",
                        toSqlDate(reparacion.getFechaIngresoTaller()), Types.DATE)
                .addValue("P_FECHA_INICIO_REPARACION",
                        toSqlDate(reparacion.getFechaInicioReparacion()), Types.DATE)
                .addValue("P_FECHA_FIN_REPARACION",
                        toSqlDate(reparacion.getFechaFinReparacion()), Types.DATE)
                .addValue("P_FECHA_SALIDA_TALLER",
                        toSqlDate(reparacion.getFechaSalidaTaller()), Types.DATE)
                .addValue("P_ID_INCIDENTE",
                        reparacion.getIdIncidente())
                .addValue("P_ID_ESTADO",
                        reparacion.getEstado())
                .addValue("P_ID_MECANICO",
                        reparacion.getIdMecanico());
    }

    // CONVERTIR FECHAS DE JAVA A SQL
    private static java.sql.Date toSqlDate(LocalDate fecha) {
        return fecha == null ? null : java.sql.Date.valueOf(fecha);
    }

    // CONVERTIR FECHAS DE SQL A JAVA
    private static LocalDate toLocalDate(java.sql.Date fecha) {
        return fecha == null ? null : fecha.toLocalDate();
    }

    // CONVERTIR RESULTADOS DEL CURSOR A REPARACION
    private static Reparacion mapReparacion(ResultSet rs)
            throws SQLException {

        Reparacion reparacion = new Reparacion();

        reparacion.setId(rs.getLong("ID_REPARACION"));
        reparacion.setDescripcionReparacion(
                rs.getString("DESCRIPCION_REPARACION"));
        reparacion.setHorasTrabajadas(
                rs.getBigDecimal("HORAS_TRABAJADAS"));
        reparacion.setTarifaHora(
                rs.getBigDecimal("TARIFA_HORA"));
        reparacion.setCostoServicio(
                rs.getBigDecimal("COSTO_SERVICIO"));

        reparacion.setFechaIngresoTaller(
                toLocalDate(rs.getDate("FECHA_INGRESO_TALLER")));
        reparacion.setFechaInicioReparacion(
                toLocalDate(rs.getDate("FECHA_INICIO_REPARACION")));
        reparacion.setFechaFinReparacion(
                toLocalDate(rs.getDate("FECHA_FIN_REPARACION")));
        reparacion.setFechaSalidaTaller(
                toLocalDate(rs.getDate("FECHA_SALIDA_TALLER")));

        reparacion.setIdIncidente(rs.getLong("ID_INCIDENTE"));
        reparacion.setEstado(rs.getLong("ID_ESTADO"));
        reparacion.setIdMecanico(rs.getLong("ID_MECANICO"));

        return reparacion;
    }

    // OBTENER LOS RESULTADOS DEL SYS_REFCURSOR
    @SuppressWarnings("unchecked")
    private static <T> List<T> cursor(Map<String, Object> output) {
        return (List<T>) output.get("P_RESULTADO");
    }
}
