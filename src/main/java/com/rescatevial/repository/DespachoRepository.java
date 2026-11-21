package com.rescatevial.repository;

import com.rescatevial.domain.IncidentePendienteDespacho;
import com.rescatevial.domain.UnidadDisponibleDespacho;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
public class DespachoRepository {
    private static final long UNIDAD_DISPONIBLE = 9L;
    private final JdbcTemplate jdbc;

    public DespachoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<IncidentePendienteDespacho> findPendingIncidents() {
        return filas("FIDE_OBTENER_INCIDENTES_PENDIENTES_DESPACHO_SP").stream()
                .map(row -> {
                    IncidentePendienteDespacho item = new IncidentePendienteDespacho();
                    item.setId(numero(row.get("ID_INCIDENTE")));
                    item.setFechaReporte(fecha(row.get("FECHA_REPORTE")));
                    item.setHoraReporte(texto(row.get("HORA_REPORTE")));
                    item.setPlacaVehiculo(texto(row.get("PLACA_VEHICULO")));
                    item.setTipo(texto(row.get("TIPO")));
                    item.setPrioridad(texto(row.get("PRIORIDAD")));
                    item.setUbicacion(texto(row.get("UBICACION")));
                    item.setDetalle(texto(row.get("DETALLE")));
                    return item;
                })
                .toList();
    }

    public List<UnidadDisponibleDespacho> findAvailableUnits() {
        List<Map<String, Object>> rows = OraclePackageSupport.rows(
                OraclePackageSupport.call(jdbc, "FIDE_OBTENER_UNIDADES_DISPONIBLES_DESPACHO_SP",
                        new SqlParameter("P_ID_ESTADO", Types.NUMERIC),
                        OraclePackageSupport.cursor())
                        .execute(new MapSqlParameterSource("P_ID_ESTADO", UNIDAD_DISPONIBLE)));

        return rows.stream().map(row -> {
            UnidadDisponibleDespacho unit = new UnidadDisponibleDespacho();
            unit.setId(numero(row.get("ID_UNIDAD")));
            unit.setNombre(texto(row.get("NOMBRE_UNIDAD")));
            unit.setPlaca(texto(row.get("PLACA_UNIDAD")));
            unit.setTipo(texto(row.get("TIPO")));
            return unit;
        }).toList();
    }

    public void assign(Long incidentId, Long unitId, Long responsibleId, String observations) {
        Map<String, Object> result = OraclePackageSupport.call(jdbc,
                        "FIDE_ASIGNAR_UNIDAD_INCIDENTE_SP",
                        new SqlParameter("P_ID_UNIDAD", Types.NUMERIC),
                        new SqlParameter("P_ID_INCIDENTE", Types.NUMERIC),
                        new SqlParameter("P_OBSERVACIONES", Types.VARCHAR),
                        new SqlParameter("P_ID_RESPONSABLE", Types.NUMERIC),
                        new SqlOutParameter("P_RESULTADO", Types.NUMERIC))
                .execute(new MapSqlParameterSource()
                        .addValue("P_ID_UNIDAD", unitId)
                        .addValue("P_ID_INCIDENTE", incidentId)
                        .addValue("P_OBSERVACIONES", blankToNull(observations))
                        .addValue("P_ID_RESPONSABLE", responsibleId));

        if (!(result.get("P_RESULTADO") instanceof Number)) {
            throw new IllegalStateException("Oracle no devolvió el identificador del historial.");
        }
    }

    private List<Map<String, Object>> filas(String procedure) {
        return OraclePackageSupport.rows(OraclePackageSupport.call(jdbc, procedure,
                OraclePackageSupport.cursor()).execute(new MapSqlParameterSource()));
    }

    private static Long numero(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private static LocalDate fecha(Object value) {
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }
        return value instanceof LocalDate date ? date : null;
    }

    private static String texto(Object value) {
        return value == null ? null : value.toString();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
