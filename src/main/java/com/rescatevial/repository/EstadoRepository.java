package com.rescatevial.repository;

import com.rescatevial.domain.Estado;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

@Repository
public class EstadoRepository {
    private final JdbcTemplate jdbc;

    public EstadoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Estado> findAll() {
        var llamada = new SimpleJdbcCall(jdbc).withProcedureName("FIDE_OBTENER_ESTADOS_SP")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR, (rs, fila) -> {
                    Estado estado = new Estado();
                    estado.setId(rs.getLong("ID_ESTADO"));
                    estado.setNombreEstado(rs.getString("NOMBRE_ESTADO"));
                    return estado;
                }));
        return cursor(llamada.execute(new MapSqlParameterSource()));
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> cursor(Map<String, Object> salida) {
        return (List<T>) salida.get("P_RESULTADO");
    }
}
