package com.rescatevial.repository;

import com.rescatevial.domain.IncidentePendienteDespacho;
import com.rescatevial.domain.UnidadDisponibleDespacho;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
public class DespachoRepository {
    private static final long INCIDENTE_REPORTADO = 3L;
    private static final long INCIDENTE_ASIGNADO = 4L;
    private static final long UNIDAD_DISPONIBLE = 9L;
    private static final long UNIDAD_ASIGNADA = 10L;
    private static final long REGISTRO_ACTIVO = 1L;
    private final JdbcTemplate jdbc;

    public DespachoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<IncidentePendienteDespacho> findPendingIncidents() {
        Map<Long, Map<String, Object>> vehicles = byId("FIDE_OBTENER_VEHICULOS_SP", "ID_VEHICULO");
        Map<Long, String> types = names("FIDE_OBTENER_TIPOS_INCIDENTES_SP",
                "ID_TIPO_INCIDENTE", "NOMBRE_TIPO_INCIDENTE");
        Map<Long, String> priorities = names("FIDE_OBTENER_PRIORIDADES_SP",
                "ID_PRIORIDAD", "NOMBRE_PRIORIDAD");
        Map<Long, Map<String, Object>> locations = byId("FIDE_OBTENER_UBICACION_INCIDENTES_SP", "ID_INCIDENTE");
        Map<Long, String> provinces = names("FIDE_OBTENER_PROVINCIAS_SP", "ID_PROVINCIA", "NOMBRE_PROVINCIA");
        Map<Long, String> cantons = names("FIDE_OBTENER_CANTONES_SP", "ID_CANTON", "NOMBRE_CANTON");
        Map<Long, String> districts = names("FIDE_OBTENER_DISTRITOS_SP", "ID_DISTRITO", "NOMBRE_DISTRITO");

        return filas("FIDE_OBTENER_INCIDENTES_SP").stream()
                .filter(row -> INCIDENTE_REPORTADO == value(row.get("ID_ESTADO")))
                .map(row -> {
                    Long incidentId = number(row.get("ID_INCIDENTE"));
                    Map<String, Object> vehicle = vehicles.get(number(row.get("ID_VEHICULO")));
                    Map<String, Object> location = locations.get(incidentId);
                    IncidentePendienteDespacho item = new IncidentePendienteDespacho();
                    item.setId(incidentId);
                    item.setFechaReporte(date(row.get("FECHA_REPORTE")));
                    item.setHoraReporte(text(row.get("HORA_REPORTE")));
                    item.setPlacaVehiculo(text(vehicle == null ? null : vehicle.get("PLACA")));
                    item.setTipo(types.get(number(row.get("ID_TIPO_INCIDENTE"))));
                    item.setPrioridad(priorities.get(number(row.get("ID_PRIORIDAD"))));
                    item.setUbicacion(location(
                            provinces.get(number(location == null ? null : location.get("ID_PROVINCIA"))),
                            cantons.get(number(location == null ? null : location.get("ID_CANTON"))),
                            districts.get(number(location == null ? null : location.get("ID_DISTRITO"))),
                            location == null ? null : text(location.get("OTRAS_SENAS"))));
                    item.setDetalle(text(row.get("DETALLES_INCIDENTE")));
                    return item;
                })
                .sorted(Comparator.comparing(IncidentePendienteDespacho::getPrioridad,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(IncidentePendienteDespacho::getFechaReporte,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(IncidentePendienteDespacho::getId))
                .toList();
    }

    public List<UnidadDisponibleDespacho> findAvailableUnits() {
        Map<Long, String> types = names("FIDE_OBTENER_TIPOS_UNIDADES_SP",
                "ID_TIPO_UNIDAD", "NOMBRE_TIPO_UNIDAD");
        return filas("FIDE_OBTENER_UNIDADES_SP").stream()
                .filter(row -> UNIDAD_DISPONIBLE == value(row.get("ID_ESTADO")))
                .filter(row -> activeType(row.get("ID_TIPO_UNIDAD")))
                .map(row -> {
                    UnidadDisponibleDespacho unit = new UnidadDisponibleDespacho();
                    unit.setId(number(row.get("ID_UNIDAD")));
                    unit.setNombre(text(row.get("NOMBRE_UNIDAD")));
                    unit.setPlaca(text(row.get("PLACA_UNIDAD")));
                    unit.setTipo(types.get(number(row.get("ID_TIPO_UNIDAD"))));
                    return unit;
                })
                .sorted(Comparator.comparing(UnidadDisponibleDespacho::getTipo,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(UnidadDisponibleDespacho::getNombre,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(UnidadDisponibleDespacho::getId))
                .toList();
    }

    public void assign(Long incidentId, Long unitId, Long responsibleId, String observations) {
        Map<String, Object> incident = findById("FIDE_OBTENER_INCIDENTE_POR_ID_SP",
                "P_ID_INCIDENTE", incidentId);
        Map<String, Object> unit = findById("FIDE_OBTENER_UNIDAD_POR_ID_SP",
                "P_ID_UNIDAD", unitId);
        if (incident == null || INCIDENTE_REPORTADO != value(incident.get("ID_ESTADO"))) {
            throw new IllegalStateException("El incidente ya no está pendiente de despacho.");
        }
        if (unit == null || UNIDAD_DISPONIBLE != value(unit.get("ID_ESTADO"))) {
            throw new IllegalStateException("La unidad seleccionada ya no está disponible.");
        }

        long historyId = filas("FIDE_OBTENER_HISTORIALES_UNIDADES_SP").stream()
                .filter(row -> unitId.equals(number(row.get("ID_UNIDAD"))))
                .map(row -> number(row.get("ID_HISTORIAL")))
                .filter(java.util.Objects::nonNull)
                .max(Long::compareTo)
                .orElse(0L) + 1;

        OraclePackageSupport.call(jdbc, "FIDE_INSERTAR_HISTORIALES_UNIDADES_SP",
                        new SqlParameter("P_ID_UNIDAD", Types.NUMERIC),
                        new SqlParameter("P_ID_HISTORIAL", Types.NUMERIC),
                        new SqlParameter("P_OBSERVACIONES", Types.VARCHAR),
                        new SqlParameter("P_FECHA_SALIDA_UNIDAD", Types.DATE),
                        new SqlParameter("P_HORA_SALIDA_UNIDAD", Types.VARCHAR),
                        new SqlParameter("P_FECHA_REGRESO_UNIDAD", Types.DATE),
                        new SqlParameter("P_HORA_REGRESO_UNIDAD", Types.VARCHAR),
                        new SqlParameter("P_ID_RESPONSABLE", Types.NUMERIC),
                        new SqlParameter("P_ID_INCIDENTE", Types.NUMERIC),
                        new SqlParameter("P_ID_ESTADO", Types.NUMERIC),
                        new SqlParameter("P_KILOMETRAJE_INICIAL", Types.NUMERIC),
                        new SqlParameter("P_KILOMETRAJE_FINAL", Types.NUMERIC))
                .execute(new MapSqlParameterSource()
                        .addValue("P_ID_UNIDAD", unitId)
                        .addValue("P_ID_HISTORIAL", historyId)
                        .addValue("P_OBSERVACIONES", blankToNull(observations))
                        .addValue("P_FECHA_SALIDA_UNIDAD", java.sql.Date.valueOf(LocalDate.now()))
                        .addValue("P_HORA_SALIDA_UNIDAD", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")))
                        .addValue("P_ID_RESPONSABLE", responsibleId)
                        .addValue("P_ID_INCIDENTE", incidentId)
                        .addValue("P_ID_ESTADO", REGISTRO_ACTIVO));

        OraclePackageSupport.call(jdbc, "FIDE_MODIFICAR_UNIDADES_SP",
                        new SqlParameter("P_ID_UNIDAD", Types.NUMERIC),
                        new SqlParameter("P_NOMBRE_UNIDAD", Types.VARCHAR),
                        new SqlParameter("P_ID_TIPO_UNIDAD", Types.NUMERIC),
                        new SqlParameter("P_ID_ESTADO", Types.NUMERIC),
                        new SqlParameter("P_PLACA_UNIDAD", Types.VARCHAR))
                .execute(new MapSqlParameterSource()
                        .addValue("P_ID_UNIDAD", unitId)
                        .addValue("P_NOMBRE_UNIDAD", unit.get("NOMBRE_UNIDAD"))
                        .addValue("P_ID_TIPO_UNIDAD", unit.get("ID_TIPO_UNIDAD"))
                        .addValue("P_ID_ESTADO", UNIDAD_ASIGNADA)
                        .addValue("P_PLACA_UNIDAD", unit.get("PLACA_UNIDAD")));

        OraclePackageSupport.call(jdbc, "FIDE_MODIFICAR_INCIDENTES_SP",
                        incidentParameters())
                .execute(new MapSqlParameterSource()
                        .addValue("P_ID_INCIDENTE", incidentId)
                        .addValue("P_ID_VEHICULO", incident.get("ID_VEHICULO"))
                        .addValue("P_FECHA_REPORTE", incident.get("FECHA_REPORTE"))
                        .addValue("P_HORA_REPORTE", incident.get("HORA_REPORTE"))
                        .addValue("P_DETALLES_INCIDENTE", incident.get("DETALLES_INCIDENTE"))
                        .addValue("P_ID_TIPO_INCIDENTE", incident.get("ID_TIPO_INCIDENTE"))
                        .addValue("P_ID_PRIORIDAD", incident.get("ID_PRIORIDAD"))
                        .addValue("P_ID_ESTADO", INCIDENTE_ASIGNADO)
                        .addValue("P_ID_USUARIO", incident.get("ID_USUARIO"))
                        .addValue("P_FECHA_CIERRE", incident.get("FECHA_CIERRE"))
                        .addValue("P_HORA_CIERRE", incident.get("HORA_CIERRE")));
    }

    private SqlParameter[] incidentParameters() {
        return new SqlParameter[] {
            new SqlParameter("P_ID_INCIDENTE", Types.NUMERIC),
            new SqlParameter("P_ID_VEHICULO", Types.NUMERIC),
            new SqlParameter("P_FECHA_REPORTE", Types.DATE),
            new SqlParameter("P_HORA_REPORTE", Types.VARCHAR),
            new SqlParameter("P_DETALLES_INCIDENTE", Types.VARCHAR),
            new SqlParameter("P_ID_TIPO_INCIDENTE", Types.NUMERIC),
            new SqlParameter("P_ID_PRIORIDAD", Types.NUMERIC),
            new SqlParameter("P_ID_ESTADO", Types.NUMERIC),
            new SqlParameter("P_ID_USUARIO", Types.NUMERIC),
            new SqlParameter("P_FECHA_CIERRE", Types.DATE),
            new SqlParameter("P_HORA_CIERRE", Types.VARCHAR)
        };
    }

    private boolean activeType(Object id) {
        return filas("FIDE_OBTENER_TIPOS_UNIDADES_SP").stream()
                .anyMatch(row -> java.util.Objects.equals(number(row.get("ID_TIPO_UNIDAD")), number(id))
                        && REGISTRO_ACTIVO == value(row.get("ID_ESTADO")));
    }

    private Map<Long, Map<String, Object>> byId(String procedure, String idColumn) {
        Map<Long, Map<String, Object>> result = new HashMap<>();
        filas(procedure).forEach(row -> result.put(number(row.get(idColumn)), row));
        return result;
    }

    private Map<String, Object> findById(String procedure, String parameter, Long id) {
        List<Map<String, Object>> rows = OraclePackageSupport.rows(
                OraclePackageSupport.call(jdbc, procedure,
                                new SqlParameter(parameter, Types.NUMERIC),
                                OraclePackageSupport.cursor())
                        .execute(new MapSqlParameterSource(parameter, id)));
        return rows.stream().findFirst().orElse(null);
    }

    private Map<Long, String> names(String procedure, String id, String name) {
        Map<Long, String> result = new HashMap<>();
        filas(procedure).forEach(row -> result.put(number(row.get(id)), text(row.get(name))));
        return result;
    }

    private List<Map<String, Object>> filas(String procedure) {
        return OraclePackageSupport.rows(OraclePackageSupport.call(jdbc, procedure,
                OraclePackageSupport.cursor()).execute(new MapSqlParameterSource()));
    }

    private static Long number(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private static long value(Object value) {
        Long number = number(value);
        return number == null ? Long.MIN_VALUE : number;
    }

    private static LocalDate date(Object value) {
        return value instanceof java.sql.Date date ? date.toLocalDate() : null;
    }

    private static String text(Object value) {
        return value == null ? null : value.toString();
    }

    private static String location(String province, String canton, String district, String notes) {
        String value = String.join(", ", province == null ? "" : province,
                canton == null ? "" : canton, district == null ? "" : district);
        return notes == null || notes.isBlank() ? value : value + " — " + notes;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
