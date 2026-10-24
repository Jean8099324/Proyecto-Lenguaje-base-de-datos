package com.rescatevial.repository;

import com.rescatevial.domain.Usuario;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

@Repository
public class UsuarioRepository {
    private final JdbcTemplate jdbc;
    private final SimpleJdbcCall insertarUsuario;

    public UsuarioRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.insertarUsuario = new SimpleJdbcCall(jdbc)
                .withProcedureName("FIDE_INSERTAR_USUARIOS_SP")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("P_ID_USUARIO", Types.NUMERIC),
                        new SqlParameter("P_NOMBRE", Types.VARCHAR),
                        new SqlParameter("P_PRIMER_APELLIDO", Types.VARCHAR),
                        new SqlParameter("P_SEGUNDO_APELLIDO", Types.VARCHAR),
                        new SqlParameter("P_FECHA_NACIMIENTO", Types.DATE),
                        new SqlParameter("P_USERNAME", Types.VARCHAR),
                        new SqlParameter("P_PASSWORD", Types.VARCHAR),
                        new SqlParameter("P_FECHA_CREACION", Types.DATE),
                        new SqlParameter("P_FECHA_MODIFICACION", Types.DATE),
                        new SqlParameter("P_FECHA_ELIMINACION", Types.DATE),
                        new SqlParameter("P_TIPO_USUARIO", Types.NUMERIC),
                        new SqlParameter("P_ESTADO", Types.NUMERIC));
    }

    public List<Usuario> findAll() {
        return jdbc.query("""
                SELECT u.ID_USUARIO, u.NOMBRE, u.PRIMER_APELLIDO, u.SEGUNDO_APELLIDO,
                       u.FECHA_NACIMIENTO, u.USERNAME, u.FECHA_CREACION, u.FECHA_MODIFICACION,
                       u.FECHA_ELIMINACION, u.ID_TIPO_USUARIO, t.NOMBRE_TIPO_USUARIO,
                       u.ID_ESTADO, e.NOMBRE_ESTADO
                FROM FIDE_USUARIOS_TB u
                JOIN FIDE_TIPOS_USUARIOS_TB t ON t.ID_TIPO_USUARIO = u.ID_TIPO_USUARIO
                JOIN FIDE_ESTADOS_TB e ON e.ID_ESTADO = u.ID_ESTADO
                ORDER BY u.ID_USUARIO
                """, (rs, row) -> {
            Usuario usuario = new Usuario();
            usuario.setId(rs.getLong("ID_USUARIO"));
            usuario.setNombre(rs.getString("NOMBRE"));
            usuario.setPrimerApellido(rs.getString("PRIMER_APELLIDO"));
            usuario.setSegundoApellido(rs.getString("SEGUNDO_APELLIDO"));
            usuario.setFechaNacimiento(toLocalDate(rs.getDate("FECHA_NACIMIENTO")));
            usuario.setUsername(rs.getString("USERNAME"));
            usuario.setFechaCreacion(toLocalDateTime(rs.getTimestamp("FECHA_CREACION")));
            usuario.setFechaModificacion(toLocalDateTime(rs.getTimestamp("FECHA_MODIFICACION")));
            usuario.setFechaEliminacion(toLocalDateTime(rs.getTimestamp("FECHA_ELIMINACION")));
            usuario.setTipoUsuario(rs.getLong("ID_TIPO_USUARIO"));
            usuario.setTipoUsuarioNombre(rs.getString("NOMBRE_TIPO_USUARIO"));
            usuario.setEstado(rs.getLong("ID_ESTADO"));
            usuario.setEstadoNombre(rs.getString("NOMBRE_ESTADO"));
            return usuario;
        });
    }

    public List<Map<String, Object>> findTiposUsuario() {
        return jdbc.queryForList("""
                SELECT ID_TIPO_USUARIO AS ID, NOMBRE_TIPO_USUARIO AS NOMBRE
                FROM FIDE_TIPOS_USUARIOS_TB WHERE ID_ESTADO = 1 ORDER BY ID_TIPO_USUARIO
                """);
    }

    public List<Map<String, Object>> findEstados() {
        return jdbc.queryForList("""
                SELECT ID_ESTADO AS ID, NOMBRE_ESTADO AS NOMBRE FROM FIDE_ESTADOS_TB
                WHERE ID_ESTADO IN (1, 2) ORDER BY ID_ESTADO
                """);
    }

    public long nextUserId() {
        Long nextId = jdbc.queryForObject(
                "SELECT NVL(MAX(ID_USUARIO), 0) + 1 FROM FIDE_USUARIOS_TB", Long.class);
        return nextId == null ? 1 : nextId;
    }

    public void insertUsingProcedure(Usuario usuario, String passwordHash) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("P_ID_USUARIO", usuario.getId())
                .addValue("P_NOMBRE", usuario.getNombre().trim())
                .addValue("P_PRIMER_APELLIDO", usuario.getPrimerApellido().trim())
                .addValue("P_SEGUNDO_APELLIDO", emptyToNull(usuario.getSegundoApellido()))
                .addValue("P_FECHA_NACIMIENTO", toTimestamp(usuario.getFechaNacimiento()), Types.DATE)
                .addValue("P_USERNAME", usuario.getUsername().trim())
                .addValue("P_PASSWORD", passwordHash)
                .addValue("P_FECHA_CREACION", toTimestamp(usuario.getFechaCreacion()), Types.DATE)
                .addValue("P_FECHA_MODIFICACION", toTimestamp(usuario.getFechaModificacion()), Types.DATE)
                .addValue("P_FECHA_ELIMINACION", toTimestamp(usuario.getFechaEliminacion()), Types.DATE)
                .addValue("P_TIPO_USUARIO", usuario.getTipoUsuario())
                .addValue("P_ESTADO", usuario.getEstado());
        insertarUsuario.execute(params);
    }

    private static Timestamp toTimestamp(java.time.LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private static Timestamp toTimestamp(LocalDate value) {
        return value == null ? null : Timestamp.valueOf(value.atStartOfDay());
    }

    private static LocalDate toLocalDate(java.sql.Date value) {
        return value == null ? null : value.toLocalDate();
    }

    private static java.time.LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
