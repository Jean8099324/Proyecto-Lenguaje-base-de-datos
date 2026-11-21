package com.rescatevial.repository;

import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

@Repository
public class DashboardRepository {
    private final JdbcTemplate jdbc;

    public DashboardRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long contarIncidentes() {
        return ejecutarFuncion("FIDE_CONTAR_INCIDENTES_FN").longValue();
    }

    public long contarIncidentesEnAtencion() {
        return ejecutarFuncion("FIDE_CONTAR_INCIDENTES_EN_ATENCION_FN").longValue();
    }

    public List<Map<String, String>> obtenerIncidentesRecientes() {
        List<Map<String, Object>> filas = OraclePackageSupport.rows(
                OraclePackageSupport.call(jdbc, "FIDE_OBTENER_INCIDENTES_RECIENTES_SP",
                        OraclePackageSupport.cursor())
                        .execute(new MapSqlParameterSource()));

        return filas.stream().map(row -> {
            String estado = texto(row.get("NOMBRE_ESTADO"), "SIN ESTADO");
            Map<String, String> incidente = new HashMap<>();
            incidente.put("id", texto(row.get("ID_INCIDENTE"), ""));
            incidente.put("ubicacion", texto(row.get("NOMBRE_PROVINCIA"), "Sin ubicación"));
            incidente.put("tipo", texto(row.get("NOMBRE_TIPO_INCIDENTE"), "Sin tipo"));
            incidente.put("estado", estado);
            incidente.put("hora", texto(row.get("HORA_REPORTE"), "Sin hora"));
            incidente.put("claseEstado", claseEstado(estado));
            return incidente;
        }).toList();
    }

    public Map<String, Long> obtenerEstadosUnidades() {
        List<Map<String, Object>> filas = OraclePackageSupport.rows(
                OraclePackageSupport.call(jdbc, "FIDE_OBTENER_UNIDADES_POR_ESTADO_SP",
                        OraclePackageSupport.cursor())
                        .execute(new MapSqlParameterSource()));

        Map<String, Long> resultado = new HashMap<>();
        filas.forEach(row -> {
            String estado = texto(row.get("NOMBRE_ESTADO"), "");
            if (!estado.isBlank()) {
                resultado.put(estado.trim().toUpperCase(Locale.ROOT),
                        numero(row.get("TOTAL_UNIDADES")));
            }
        });
        return resultado;
    }

    private Number ejecutarFuncion(String funcion) {
        SimpleJdbcCall llamada = new SimpleJdbcCall(jdbc)
                .withCatalogName(OraclePackageSupport.PACKAGE)
                .withFunctionName(funcion)
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlOutParameter("RETURN_VALUE", Types.NUMERIC));
        Number resultado = llamada.executeFunction(Number.class, new MapSqlParameterSource());
        return resultado == null ? 0 : resultado;
    }

    private static long numero(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private static String texto(Object value, String alternativa) {
        if (value == null || value.toString().isBlank()) {
            return alternativa;
        }
        return value.toString().trim();
    }

    private static String claseEstado(String estado) {
        return switch (estado.trim().toUpperCase(Locale.ROOT)) {
            case "EN ATENCION", "EN ATENCIÓN", "TRASLADO" -> "attending";
            case "ASIGNADO", "UNIDAD EN CAMINO" -> "dispatched";
            case "FINALIZADO" -> "completed";
            default -> "pending";
        };
    }
}
