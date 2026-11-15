package com.rescatevial.repository;

import com.rescatevial.domain.IncidentePendienteDespacho;
import com.rescatevial.domain.UnidadDisponibleDespacho;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
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
        String sql = """
                SELECT i.ID_INCIDENTE, i.FECHA_REPORTE, i.HORA_REPORTE,
                       v.PLACA, ti.NOMBRE_TIPO_INCIDENTE, p.NOMBRE_PRIORIDAD,
                       pr.NOMBRE_PROVINCIA, ca.NOMBRE_CANTON, di.NOMBRE_DISTRITO,
                       ui.OTRAS_SENAS, i.DETALLES_INCIDENTE
                  FROM FIDE_INCIDENTES_TB i
                  JOIN FIDE_VEHICULOS_TB v ON v.ID_VEHICULO = i.ID_VEHICULO
                  JOIN FIDE_TIPOS_INCIDENTES_TB ti ON ti.ID_TIPO_INCIDENTE = i.ID_TIPO_INCIDENTE
                  JOIN FIDE_PRIORIDADES_TB p ON p.ID_PRIORIDAD = i.ID_PRIORIDAD
                  JOIN FIDE_UBICACION_INCIDENTES_TB ui ON ui.ID_INCIDENTE = i.ID_INCIDENTE
                  JOIN FIDE_PROVINCIAS_TB pr ON pr.ID_PROVINCIA = ui.ID_PROVINCIA
                  JOIN FIDE_CANTONES_TB ca ON ca.ID_CANTON = ui.ID_CANTON
                  JOIN FIDE_DISTRITOS_TB di ON di.ID_DISTRITO = ui.ID_DISTRITO
                 WHERE i.ID_ESTADO = ?
                 ORDER BY CASE UPPER(p.NOMBRE_PRIORIDAD)
                              WHEN 'ALTA' THEN 1 WHEN 'MEDIA' THEN 2 ELSE 3
                          END,
                          i.FECHA_REPORTE, i.HORA_REPORTE, i.ID_INCIDENTE
                """;
        return jdbc.query(sql, (rs, row) -> {
            IncidentePendienteDespacho item = new IncidentePendienteDespacho();
            item.setId(rs.getLong("ID_INCIDENTE"));
            Date date = rs.getDate("FECHA_REPORTE");
            item.setFechaReporte(date == null ? null : date.toLocalDate());
            item.setHoraReporte(rs.getString("HORA_REPORTE"));
            item.setPlacaVehiculo(rs.getString("PLACA"));
            item.setTipo(rs.getString("NOMBRE_TIPO_INCIDENTE"));
            item.setPrioridad(rs.getString("NOMBRE_PRIORIDAD"));
            item.setUbicacion(location(rs.getString("NOMBRE_PROVINCIA"), rs.getString("NOMBRE_CANTON"),
                    rs.getString("NOMBRE_DISTRITO"), rs.getString("OTRAS_SENAS")));
            item.setDetalle(rs.getString("DETALLES_INCIDENTE"));
            return item;
        }, INCIDENTE_REPORTADO);
    }

    public List<UnidadDisponibleDespacho> findAvailableUnits() {
        String sql = """
                SELECT u.ID_UNIDAD, u.NOMBRE_UNIDAD, u.PLACA_UNIDAD, t.NOMBRE_TIPO_UNIDAD
                  FROM FIDE_UNIDADES_TB u
                  JOIN FIDE_TIPOS_UNIDADES_TB t ON t.ID_TIPO_UNIDAD = u.ID_TIPO_UNIDAD
                 WHERE u.ID_ESTADO = ?
                   AND t.ID_ESTADO = 1
                 ORDER BY t.NOMBRE_TIPO_UNIDAD, u.NOMBRE_UNIDAD, u.ID_UNIDAD
                """;
        return jdbc.query(sql, (rs, row) -> {
            UnidadDisponibleDespacho unit = new UnidadDisponibleDespacho();
            unit.setId(rs.getLong("ID_UNIDAD"));
            unit.setNombre(rs.getString("NOMBRE_UNIDAD"));
            unit.setPlaca(rs.getString("PLACA_UNIDAD"));
            unit.setTipo(rs.getString("NOMBRE_TIPO_UNIDAD"));
            return unit;
        }, UNIDAD_DISPONIBLE);
    }

    public void assign(Long incidentId, Long unitId, Long responsibleId, String observations) {
        Long incidentStatus = lockStatus("FIDE_INCIDENTES_TB", "ID_INCIDENTE", incidentId);
        if (!Long.valueOf(INCIDENTE_REPORTADO).equals(incidentStatus)) {
            throw new IllegalStateException("El incidente ya no está pendiente de despacho.");
        }
        Long unitStatus = lockStatus("FIDE_UNIDADES_TB", "ID_UNIDAD", unitId);
        if (!Long.valueOf(UNIDAD_DISPONIBLE).equals(unitStatus)) {
            throw new IllegalStateException("La unidad seleccionada ya no está disponible.");
        }

        Long historyId = jdbc.queryForObject(
                "SELECT NVL(MAX(ID_HISTORIAL), 0) + 1 FROM FIDE_HISTORIALES_UNIDADES_TB WHERE ID_UNIDAD = ?",
                Long.class, unitId);
        int inserted = jdbc.update("""
                INSERT INTO FIDE_HISTORIALES_UNIDADES_TB
                    (ID_UNIDAD, ID_HISTORIAL, OBSERVACIONES, FECHA_SALIDA_UNIDAD,
                     HORA_SALIDA_UNIDAD, ID_RESPONSABLE, ID_INCIDENTE, ID_ESTADO)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, unitId, historyId, blankToNull(observations), Date.valueOf(LocalDate.now()),
                java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                responsibleId, incidentId, REGISTRO_ACTIVO);
        int unitUpdated = jdbc.update(
                "UPDATE FIDE_UNIDADES_TB SET ID_ESTADO = ? WHERE ID_UNIDAD = ? AND ID_ESTADO = ?",
                UNIDAD_ASIGNADA, unitId, UNIDAD_DISPONIBLE);
        int incidentUpdated = jdbc.update(
                "UPDATE FIDE_INCIDENTES_TB SET ID_ESTADO = ? WHERE ID_INCIDENTE = ? AND ID_ESTADO = ?",
                INCIDENTE_ASIGNADO, incidentId, INCIDENTE_REPORTADO);
        if (inserted != 1 || unitUpdated != 1 || incidentUpdated != 1) {
            throw new IllegalStateException("No fue posible completar la asignación.");
        }
    }

    private Long lockStatus(String table, String idColumn, Long id) {
        List<Long> values = jdbc.query("SELECT ID_ESTADO FROM " + table + " WHERE " + idColumn + " = ? FOR UPDATE",
                (rs, row) -> rs.getLong(1), id);
        if (values.isEmpty()) throw new IllegalArgumentException("El registro seleccionado no existe.");
        return values.getFirst();
    }

    private static String location(String province, String canton, String district, String notes) {
        String value = String.join(", ", province, canton, district);
        return notes == null || notes.isBlank() ? value : value + " — " + notes;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
