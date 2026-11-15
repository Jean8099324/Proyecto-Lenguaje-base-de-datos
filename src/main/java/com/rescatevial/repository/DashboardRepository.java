
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

    private long contarRegistros(String procedimiento, String columna) {

        var llamada = new SimpleJdbcCall(jdbc)
                .withCatalogName("FIDE_PROYECTO_FINAL_RESCATE_VIAL_CR_PCK")
                .withProcedureName(procedimiento)
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlOutParameter(
                                "P_RESULTADO",
                                Types.REF_CURSOR,
                                (rs, fila) -> rs.getLong(columna)
                        )
                );

        Object resultado = llamada.execute(
                new MapSqlParameterSource()
        ).get("P_RESULTADO");

        if (resultado instanceof List<?> registros) {
            return registros.size();
        }

        return 0;
    }

    public long contarIncidentes() {
        return contarRegistros(
                "FIDE_OBTENER_INCIDENTES_SP",
                "ID_INCIDENTE"
        );
    }

    public long contarUnidades() {
        return contarRegistros(
                "FIDE_OBTENER_UNIDADES_SP",
                "ID_UNIDAD"
        );
    }

    // Total de incidentes cuyo estado operativo es EN ATENCION.
    public long contarIncidentesEnAtencion() {
        String sql = """
                SELECT COUNT(*)
                FROM FIDE_INCIDENTES_TB I
                INNER JOIN FIDE_ESTADOS_TB E ON I.ID_ESTADO = E.ID_ESTADO
                WHERE UPPER(TRIM(E.NOMBRE_ESTADO)) = 'EN ATENCION'
                """;
        Long total = jdbc.queryForObject(sql, Long.class);
        return total != null ? total : 0;
    }

    // Los mismos JOIN y columnas comprobados en SQL Developer.
    public List<Map<String, String>> obtenerIncidentesRecientes() {
        String sql = """
                SELECT I.ID_INCIDENTE,
                       P.NOMBRE_PROVINCIA AS UBICACION,
                       T.NOMBRE_TIPO_INCIDENTE AS TIPO,
                       E.NOMBRE_ESTADO AS ESTADO,
                       I.FECHA_REPORTE,
                       I.HORA_REPORTE
                FROM FIDE_INCIDENTES_TB I
                LEFT JOIN FIDE_UBICACION_INCIDENTES_TB U
                    ON I.ID_INCIDENTE = U.ID_INCIDENTE
                LEFT JOIN FIDE_PROVINCIAS_TB P
                    ON U.ID_PROVINCIA = P.ID_PROVINCIA
                LEFT JOIN FIDE_TIPOS_INCIDENTES_TB T
                    ON I.ID_TIPO_INCIDENTE = T.ID_TIPO_INCIDENTE
                LEFT JOIN FIDE_ESTADOS_TB E
                    ON I.ID_ESTADO = E.ID_ESTADO
                ORDER BY I.FECHA_REPORTE DESC NULLS LAST,
                         I.ID_INCIDENTE DESC
                FETCH FIRST 5 ROWS ONLY
                """;

        return jdbc.query(sql, (rs, fila) -> {
            Map<String, String> incidente = new HashMap<>();
            String estado = texto(rs.getString("ESTADO"), "SIN ESTADO");
            incidente.put("id", String.valueOf(rs.getLong("ID_INCIDENTE")));
            incidente.put("ubicacion", texto(rs.getString("UBICACION"), "Sin ubicación"));
            incidente.put("tipo", texto(rs.getString("TIPO"), "Sin tipo"));
            incidente.put("estado", estado);
            incidente.put("hora", texto(rs.getString("HORA_REPORTE"), "Sin hora"));
            incidente.put("claseEstado", claseEstado(estado));
            return incidente;
        });
    }

    // Devuelve las cantidades por estado real de cada unidad.
    public Map<String, Long> obtenerEstadosUnidades() {
        String sql = """
                SELECT E.NOMBRE_ESTADO, COUNT(*) AS TOTAL
                FROM FIDE_UNIDADES_TB U
                INNER JOIN FIDE_ESTADOS_TB E ON U.ID_ESTADO = E.ID_ESTADO
                GROUP BY E.NOMBRE_ESTADO
                """;

        return jdbc.query(sql, rs -> {
            Map<String, Long> estados = new HashMap<>();
            while (rs.next()) {
                String estado = rs.getString("NOMBRE_ESTADO");
                if (estado != null) {
                    estados.put(estado.trim().toUpperCase(Locale.ROOT), rs.getLong("TOTAL"));
                }
            }
            return estados;
        });
    }

    private String texto(String dato, String alternativa) {
        return dato == null || dato.isBlank() ? alternativa : dato.trim();
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
