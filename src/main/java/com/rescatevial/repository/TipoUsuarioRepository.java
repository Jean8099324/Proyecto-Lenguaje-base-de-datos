package com.rescatevial.repository;

import com.rescatevial.domain.Estado;
import com.rescatevial.domain.TipoUsuario;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

@Repository
public class TipoUsuarioRepository {
    private final JdbcTemplate jdbc;

    public TipoUsuarioRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<TipoUsuario> findAllActivos() {
        var llamada = new SimpleJdbcCall(jdbc).withProcedureName("FIDE_OBTENER_TIPOS_USUARIOS_SP")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR, (rs, fila) -> {
                    TipoUsuario tipo = new TipoUsuario();
                    tipo.setId(rs.getLong("ID_TIPO_USUARIO"));
                    tipo.setNombreTipoUsuario(rs.getString("NOMBRE_TIPO_USUARIO"));
                    return tipo;
                }));
        return cursor(llamada.execute(new MapSqlParameterSource()));
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> cursor(Map<String, Object> salida) {
        return (List<T>) salida.get("P_RESULTADO");
    }
}
