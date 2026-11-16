package com.rescatevial.repository;

import java.sql.Types;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
public class DashboardRepository {
    private final JdbcTemplate jdbc;

    public DashboardRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long contarIncidentes() {
        return incidentes().size();
    }

    public long contarUnidades() {
        return unidades().size();
    }

    public long contarIncidentesEnAtencion() {
        Map<Long, String> estados = estados();
        return incidentes().stream()
                .filter(row -> "EN ATENCION".equalsIgnoreCase(
                        estados.get(toLong(row.get("ID_ESTADO")))))
                .count();
    }

    public List<Map<String, String>> obtenerIncidentesRecientes() {
        Map<Long, String> provincias = nombres("FIDE_OBTENER_PROVINCIAS_SP", "ID_PROVINCIA", "NOMBRE_PROVINCIA");
        Map<Long, String> tipos = nombres("FIDE_OBTENER_TIPOS_INCIDENTES_SP",
                "ID_TIPO_INCIDENTE", "NOMBRE_TIPO_INCIDENTE");
        Map<Long, String> estados = estados();

        return incidentes().stream()
                .sorted(Comparator
                        .comparing((Map<String, Object> row) -> fecha(row.get("FECHA_REPORTE")),
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(row -> toLong(row.get("ID_INCIDENTE")),
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .map(row -> {
                    String estado = texto(estados.get(toLong(row.get("ID_ESTADO"))), "SIN ESTADO");
                    Map<String, String> incidente = new HashMap<>();
                    incidente.put("id", String.valueOf(toLong(row.get("ID_INCIDENTE"))));
                    incidente.put("ubicacion", texto(provincias.get(toLong(row.get("ID_PROVINCIA"))),
                            "Sin ubicación"));
                    incidente.put("tipo", texto(tipos.get(toLong(row.get("ID_TIPO_INCIDENTE"))), "Sin tipo"));
                    incidente.put("estado", estado);
                    incidente.put("hora", texto(row.get("HORA_REPORTE"), "Sin hora"));
                    incidente.put("claseEstado", claseEstado(estado));
                    return incidente;
                })
                .toList();
    }

    public Map<String, Long> obtenerEstadosUnidades() {
        Map<Long, String> estados = estados();
        Map<String, Long> resultado = new HashMap<>();
        unidades().forEach(row -> {
            String nombre = estados.get(toLong(row.get("ID_ESTADO")));
            if (nombre != null) {
                String clave = nombre.trim().toUpperCase(Locale.ROOT);
                resultado.merge(clave, 1L, Long::sum);
            }
        });
        return resultado;
    }

    private List<Map<String, Object>> incidentes() {
        return filas("FIDE_OBTENER_INCIDENTES_SP");
    }

    private List<Map<String, Object>> unidades() {
        return filas("FIDE_OBTENER_UNIDADES_SP");
    }

    private Map<Long, String> estados() {
        return nombres("FIDE_OBTENER_ESTADOS_SP", "ID_ESTADO", "NOMBRE_ESTADO");
    }

    private Map<Long, String> nombres(String procedimiento, String id, String nombre) {
        Map<Long, String> resultado = new HashMap<>();
        filas(procedimiento).forEach(row -> resultado.put(toLong(row.get(id)), texto(row.get(nombre), "")));
        return resultado;
    }

    private List<Map<String, Object>> filas(String procedimiento) {
        return OraclePackageSupport.rows(
                OraclePackageSupport.call(jdbc, procedimiento, OraclePackageSupport.cursor())
                        .execute(new MapSqlParameterSource()));
    }

    private static LocalDate fecha(Object value) {
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        return value instanceof LocalDate date ? date : null;
    }

    private static Long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private String texto(Object value, String alternativa) {
        if (value == null || value.toString().isBlank()) {
            return alternativa;
        }
        return value.toString().trim();
    }

    private String claseEstado(String estado) {
        return switch (estado.trim().toUpperCase(Locale.ROOT)) {
            case "EN ATENCION", "TRASLADO" -> "attending";
            case "ASIGNADO", "UNIDAD EN CAMINO" -> "dispatched";
            case "FINALIZADO" -> "completed";
            default -> "pending";
        };
    }
}
