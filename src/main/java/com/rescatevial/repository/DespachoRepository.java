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
    private final JdbcTemplate jdbc;

    public DespachoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public PaginaIncidentes findPendingIncidents(int numeroPagina, int tamanioPagina) {
        Map<String, Object> salida = OraclePackageSupport.call(jdbc,
                        "FIDE_OBTENER_INCIDENTES_PENDIENTES_DESPACHO_SP",
                        new SqlParameter("P_NUMERO_PAGINA", Types.NUMERIC),
                        new SqlParameter("P_TAMANIO_PAGINA", Types.NUMERIC),
                        new SqlOutParameter("P_TOTAL_INCIDENTES", Types.NUMERIC),
                        OraclePackageSupport.cursor())
                .execute(new MapSqlParameterSource()
                        .addValue("P_NUMERO_PAGINA", numeroPagina)
                        .addValue("P_TAMANIO_PAGINA", tamanioPagina));
        Number total = (Number) salida.get("P_TOTAL_INCIDENTES");
        List<IncidentePendienteDespacho> incidentes = OraclePackageSupport.rows(salida).stream()
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
        return new PaginaIncidentes(incidentes, total == null ? 0L : total.longValue());
    }

    public record PaginaIncidentes(List<IncidentePendienteDespacho> incidentes, long total) {}

    public List<UnidadDisponibleDespacho> findAvailableUnits() {
        List<Map<String, Object>> rows = OraclePackageSupport.rows(
                OraclePackageSupport.call(jdbc, "FIDE_OBTENER_UNIDADES_DISPONIBLES_SP",
                        OraclePackageSupport.cursor())
                        .execute(new MapSqlParameterSource()));

        return rows.stream().map(row -> {
            UnidadDisponibleDespacho unit = new UnidadDisponibleDespacho();
            unit.setId(numero(row.get("ID_UNIDAD")));
            unit.setNombre(texto(row.get("NOMBRE_UNIDAD")));
            unit.setPlaca(texto(row.get("PLACA_UNIDAD")));
            unit.setTipo(texto(row.get("NOMBRE_TIPO_UNIDAD")));
            return unit;
        }).toList();
    }

    public void assign(Long incidentId, Long unitId, Long responsibleId, String observations) {
        Map<String, Object> incident = obtenerRegistro("FIDE_OBTENER_INCIDENTE_POR_ID_SP", "P_ID_INCIDENTE", incidentId);
        Map<String, Object> unit = obtenerRegistro("FIDE_OBTENER_UNIDAD_POR_ID_SP", "P_ID_UNIDAD", unitId);
        Map<String, Long> estados = obtenerEstados();
        long estadoAsignado = estadoId(estados, "ASIGNADO");
        long estadoUnidadAsignada = estadoId(estados, "ASIGNADA");
        long idHistorial = siguienteIdHistorial(unitId);

        ejecutar("FIDE_INSERTAR_HISTORIALES_UNIDADES_SP", new MapSqlParameterSource()
                        .addValue("P_ID_UNIDAD", unitId)
                        .addValue("P_ID_HISTORIAL", idHistorial)
                        .addValue("P_OBSERVACIONES", blankToNull(observations), Types.VARCHAR)
                        .addValue("P_FECHA_SALIDA_UNIDAD", null, Types.DATE)
                        .addValue("P_HORA_SALIDA_UNIDAD", null, Types.VARCHAR)
                        .addValue("P_FECHA_REGRESO_UNIDAD", null, Types.DATE)
                        .addValue("P_HORA_REGRESO_UNIDAD", null, Types.VARCHAR)
                        .addValue("P_ID_RESPONSABLE", responsibleId)
                        .addValue("P_ID_INCIDENTE", incidentId)
                        .addValue("P_ID_ESTADO", estadoUnidadAsignada)
                        .addValue("P_KILOMETRAJE_INICIAL", null, Types.NUMERIC)
                        .addValue("P_KILOMETRAJE_FINAL", null, Types.NUMERIC),
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
                new SqlParameter("P_KILOMETRAJE_FINAL", Types.NUMERIC));

        ejecutar("FIDE_MODIFICAR_INCIDENTES_SP", new MapSqlParameterSource()
                        .addValue("P_ID_INCIDENTE", incidentId)
                        .addValue("P_ID_VEHICULO", incident.get("ID_VEHICULO"))
                        .addValue("P_FECHA_REPORTE", incident.get("FECHA_REPORTE"), Types.DATE)
                        .addValue("P_HORA_REPORTE", incident.get("HORA_REPORTE"), Types.VARCHAR)
                        .addValue("P_DETALLES_INCIDENTE", incident.get("DETALLES_INCIDENTE"), Types.VARCHAR)
                        .addValue("P_ID_TIPO_INCIDENTE", incident.get("ID_TIPO_INCIDENTE"))
                        .addValue("P_ID_PRIORIDAD", incident.get("ID_PRIORIDAD"))
                        .addValue("P_ID_ESTADO", estadoAsignado)
                        .addValue("P_ID_USUARIO", incident.get("ID_USUARIO"))
                        .addValue("P_FECHA_CIERRE", incident.get("FECHA_CIERRE"), Types.DATE)
                        .addValue("P_HORA_CIERRE", incident.get("HORA_CIERRE"), Types.VARCHAR),
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
                new SqlParameter("P_HORA_CIERRE", Types.VARCHAR));

        ejecutar("FIDE_MODIFICAR_UNIDADES_SP", new MapSqlParameterSource()
                        .addValue("P_ID_UNIDAD", unitId)
                        .addValue("P_NOMBRE_UNIDAD", unit.get("NOMBRE_UNIDAD"), Types.VARCHAR)
                        .addValue("P_ID_TIPO_UNIDAD", unit.get("ID_TIPO_UNIDAD"))
                        .addValue("P_ID_ESTADO", estadoUnidadAsignada)
                        .addValue("P_PLACA_UNIDAD", unit.get("PLACA_UNIDAD"), Types.VARCHAR),
                new SqlParameter("P_ID_UNIDAD", Types.NUMERIC),
                new SqlParameter("P_NOMBRE_UNIDAD", Types.VARCHAR),
                new SqlParameter("P_ID_TIPO_UNIDAD", Types.NUMERIC),
                new SqlParameter("P_ID_ESTADO", Types.NUMERIC),
                new SqlParameter("P_PLACA_UNIDAD", Types.VARCHAR));
    }

    private void ejecutar(String procedimiento, MapSqlParameterSource valores, SqlParameter... parametros) {
        OraclePackageSupport.call(jdbc, procedimiento, parametros).execute(valores);
    }

    private Map<String, Object> obtenerRegistro(String procedimiento, String parametro, Long id) {
        List<Map<String, Object>> filas = OraclePackageSupport.rows(
                OraclePackageSupport.call(jdbc, procedimiento,
                                new SqlParameter(parametro, Types.NUMERIC), OraclePackageSupport.cursor())
                        .execute(new MapSqlParameterSource(parametro, id)));
        return filas.stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el registro solicitado."));
    }

    private long siguienteIdHistorial(Long idUnidad) {
        Map<String, Object> resultado = OraclePackageSupport.call(jdbc,
                        "FIDE_OBTENER_SIGUIENTE_ID_HISTORIAL_UNIDAD_SP",
                        new SqlParameter("P_ID_UNIDAD", Types.NUMERIC),
                        new SqlOutParameter("P_ID_HISTORIAL", Types.NUMERIC))
                .execute(new MapSqlParameterSource("P_ID_UNIDAD", idUnidad));
        Number siguiente = (Number) resultado.get("P_ID_HISTORIAL");
        if (siguiente == null) throw new IllegalStateException("Oracle no devolvió el siguiente ID de historial.");
        return siguiente.longValue();
    }

    private Map<String, Long> obtenerEstados() {
        Map<String, Long> estados = new java.util.HashMap<>();
        OraclePackageSupport.rows(OraclePackageSupport.call(jdbc, "FIDE_OBTENER_ESTADOS_SP",
                        OraclePackageSupport.cursor()).execute(new MapSqlParameterSource()))
                .forEach(fila -> {
                    Long id = numero(fila.get("ID_ESTADO"));
                    if (id != null) estados.put(texto(fila.get("NOMBRE_ESTADO")).toUpperCase(java.util.Locale.ROOT), id);
                });
        return estados;
    }

    private static long estadoId(Map<String, Long> estados, String nombre) {
        Long id = estados.get(nombre.toUpperCase(java.util.Locale.ROOT));
        if (id == null) throw new IllegalStateException("No existe el estado " + nombre + " en Oracle.");
        return id;
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
