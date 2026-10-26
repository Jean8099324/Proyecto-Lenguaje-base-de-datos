package com.rescatevial.repository;

import com.rescatevial.domain.Usuario;
import com.rescatevial.domain.Rol;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.LinkedHashSet;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

/** Operaciones de usuarios mediante procedimientos almacenados de Oracle. */
@Repository
public class UsuarioRepository {
    private final JdbcTemplate jdbc;
    private final SimpleJdbcCall insertarUsuario;
    private final SimpleJdbcCall actualizarUsuario;

    public UsuarioRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.insertarUsuario = new SimpleJdbcCall(jdbc)
                .withProcedureName("FIDE_INSERTAR_USUARIOS_SP")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("P_NOMBRE", Types.VARCHAR),
                        new SqlParameter("P_PRIMER_APELLIDO", Types.VARCHAR),
                        new SqlParameter("P_SEGUNDO_APELLIDO", Types.VARCHAR),
                        new SqlParameter("P_FECHA_NACIMIENTO", Types.DATE),
                        new SqlParameter("P_USERNAME", Types.VARCHAR),
                        new SqlParameter("P_PASSWORD", Types.VARCHAR),
                        new SqlParameter("P_TIPO_USUARIO", Types.NUMERIC),
                        new SqlParameter("P_ESTADO", Types.NUMERIC));
        this.actualizarUsuario = new SimpleJdbcCall(jdbc)
                .withProcedureName("FIDE_ACTUALIZAR_USUARIO_SP")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("P_ID_USUARIO", Types.NUMERIC),
                        new SqlParameter("P_NOMBRE", Types.VARCHAR),
                        new SqlParameter("P_PRIMER_APELLIDO", Types.VARCHAR),
                        new SqlParameter("P_SEGUNDO_APELLIDO", Types.VARCHAR),
                        new SqlParameter("P_FECHA_NACIMIENTO", Types.DATE),
                        new SqlParameter("P_USERNAME", Types.VARCHAR),
                        new SqlParameter("P_PASSWORD", Types.VARCHAR),
                        new SqlParameter("P_TIPO_USUARIO", Types.NUMERIC),
                        new SqlParameter("P_ESTADO", Types.NUMERIC));
    }

    public Optional<Usuario> findByUsernameAndActivoTrue(String username) {
        // El procedimiento devuelve los datos necesarios para construir los atributos de sesión al autenticar.
        var call = new SimpleJdbcCall(jdbc)
                .withProcedureName("FIDE_OBTENER_USUARIO_POR_USERNAME_ESTADO_ACTIVO_SP")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlParameter("P_USERNAME", Types.VARCHAR),
                        new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR, (rs, row) -> {
                            Usuario usuario = new Usuario();
                            usuario.setId(rs.getLong("ID_USUARIO"));
                            usuario.setNombre(rs.getString("NOMBRE"));
                            usuario.setPrimerApellido(rs.getString("PRIMER_APELLIDO"));
                            usuario.setSegundoApellido(rs.getString("SEGUNDO_APELLIDO"));
                            usuario.setUsername(rs.getString("USERNAME"));
                            usuario.setPassword(rs.getString("PASSWORD"));
                            usuario.setTipoUsuario(rs.getLong("ID_TIPO_USUARIO"));
                            return usuario;
                        }));
        List<Usuario> resultado = cursor(call.execute(new MapSqlParameterSource("P_USERNAME", username)));
        Optional<Usuario> usuarioEncontrado = resultado.stream().findFirst();
        usuarioEncontrado.ifPresent(this::cargarRoles);
        return usuarioEncontrado;
    }

    public Optional<Usuario> findByUsername(String username) {
        var call = new SimpleJdbcCall(jdbc)
                .withProcedureName("FIDE_OBTENER_USUARIO_POR_USERNAME_SP")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlParameter("P_USERNAME", Types.VARCHAR),
                        new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR,
                                (rs, row) -> mapUsuario(rs)));
        List<Usuario> resultado = cursor(call.execute(new MapSqlParameterSource("P_USERNAME", username)));
        Optional<Usuario> usuarioEncontrado = resultado.stream().findFirst();
        usuarioEncontrado.ifPresent(this::cargarRoles);
        return usuarioEncontrado;
    }

    public boolean existsByUsername(String username) {
        return findByUsername(username).isPresent();
    }

    public Optional<Usuario> findById(Long id) {
        var call = new SimpleJdbcCall(jdbc)
                .withProcedureName("FIDE_OBTENER_USUARIO_POR_ID_USUARIO_SP")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlParameter("P_ID_USUARIO", Types.NUMERIC),
                        new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR,
                                (rs, row) -> mapUsuario(rs)));
        List<Usuario> resultado = cursor(call.execute(new MapSqlParameterSource("P_ID_USUARIO", id)));
        Optional<Usuario> usuarioEncontrado = resultado.stream().findFirst();
        usuarioEncontrado.ifPresent(this::cargarRoles);
        return usuarioEncontrado;
    }

    public boolean existsById(Long id) {
        return findById(id).isPresent();
    }

    public List<Usuario> findByActivoTrue() {
        var call = new SimpleJdbcCall(jdbc).withProcedureName("FIDE_OBTENER_USUARIOS_ACTIVOS_SP")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR,
                        (rs, row) -> mapUsuario(rs)));
        return cursor(call.execute(new MapSqlParameterSource()));
    }

    public List<Usuario> findAll() {
        var call = new SimpleJdbcCall(jdbc).withProcedureName("FIDE_OBTENER_USUARIOS_SP")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR,
                        (rs, row) -> mapUsuario(rs)));
        return cursor(call.execute(new MapSqlParameterSource()));
    }

    public List<String> findAllUsernames() {
        var call = new SimpleJdbcCall(jdbc).withProcedureName("FIDE_OBTENER_USUARIOS_SP")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR,
                        (rs, row) -> rs.getString("USERNAME")));
        return cursor(call.execute(new MapSqlParameterSource()));
    }

    public List<Rol> findRolesByUserId(Long userId) {
        var call = new SimpleJdbcCall(jdbc).withProcedureName("FIDE_OBTENER_ROLES_ACTIVOS_POR_ID_USUARIO_SP")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlParameter("P_ID_USUARIO", Types.NUMERIC),
                        new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR,
                                (rs, row) -> {
                                    Rol rol = new Rol();
                                    rol.setId(rs.getLong("ID_ROL"));
                                    rol.setNombre(rs.getString("NOMBRE_ROL"));
                                    return rol;
                                }));
        return cursor(call.execute(new MapSqlParameterSource("P_ID_USUARIO", userId)));
    }

    public List<Rol> findAllRoles() {
        var call = new SimpleJdbcCall(jdbc)
                .withProcedureName("FIDE_OBTENER_ROLES_SP")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR,
                        (rs, row) -> mapRol(rs)));
        return cursor(call.execute(new MapSqlParameterSource()));
    }

    public Optional<Long> findRoleIdByName(String nombreRol) {
        var call = new SimpleJdbcCall(jdbc)
                .withProcedureName("FIDE_OBTENER_ID_ROL_POR_NOMBRE_ROL_SP")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlParameter("P_NOMBRE_ROL", Types.VARCHAR),
                        new SqlOutParameter("P_ID_ROL", Types.NUMERIC));
        Object resultado = call.execute(new MapSqlParameterSource("P_NOMBRE_ROL", nombreRol)).get("P_ID_ROL");
        return resultado == null ? Optional.empty() : Optional.of(((Number) resultado).longValue());
    }

    public void assignRoleToUser(Long userId, Long roleId) {
        // Igual que Powerlete, la relación usuario-rol se mantiene directamente desde el repositorio.
        // Si ya existe (activa o inactiva), reactivarla evita un INSERT duplicado por la PK compuesta.
        int actualizadas = jdbc.update(
                "UPDATE FIDE_USUARIOS_ROLES_TB SET ID_ESTADO = 1 WHERE ID_USUARIO = ? AND ID_ROL = ?",
                userId, roleId);
        if (actualizadas == 0) {
            try {
                jdbc.update("INSERT INTO FIDE_USUARIOS_ROLES_TB (ID_USUARIO, ID_ROL, ID_ESTADO) VALUES (?, ?, 1)",
                        userId, roleId);
            } catch (DuplicateKeyException ex) {
                // Otra operación pudo crear la relación entre la comprobación y el INSERT.
                jdbc.update("UPDATE FIDE_USUARIOS_ROLES_TB SET ID_ESTADO = 1 WHERE ID_USUARIO = ? AND ID_ROL = ?",
                        userId, roleId);
            }
        }

        Integer activas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM FIDE_USUARIOS_ROLES_TB WHERE ID_USUARIO = ? AND ID_ROL = ? AND ID_ESTADO = 1",
                Integer.class, userId, roleId);
        if (activas == null || activas == 0) {
            throw new IllegalStateException("No se pudo activar el rol " + roleId + " para el usuario " + userId);
        }
    }

    public void removeRoleFromUser(Long userId, Long roleId) {
        jdbc.update("UPDATE FIDE_USUARIOS_ROLES_TB SET ID_ESTADO = 2 WHERE ID_USUARIO = ? AND ID_ROL = ?",
                userId, roleId);
    }

    public void deleteById(Long userId) {
        var call = new SimpleJdbcCall(jdbc)
                .withProcedureName("FIDE_ELIMINAR_USUARIO_SP")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlParameter("P_ID_USUARIO", Types.NUMERIC));
        call.execute(new MapSqlParameterSource("P_ID_USUARIO", userId));
    }

    private void cargarRoles(Usuario usuario) {
        usuario.setRoles(new LinkedHashSet<>(findRolesByUserId(usuario.getId())));
    }

    public void insertUsingProcedure(Usuario usuario, String passwordHash) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("P_NOMBRE", usuario.getNombre().trim())
                .addValue("P_PRIMER_APELLIDO", usuario.getPrimerApellido().trim())
                .addValue("P_SEGUNDO_APELLIDO", emptyToNull(usuario.getSegundoApellido()))
                .addValue("P_FECHA_NACIMIENTO", toTimestamp(usuario.getFechaNacimiento()), Types.DATE)
                .addValue("P_USERNAME", usuario.getUsername().trim())
                .addValue("P_PASSWORD", passwordHash)
                .addValue("P_TIPO_USUARIO", usuario.getTipoUsuario())
                .addValue("P_ESTADO", usuario.getEstado());
        insertarUsuario.execute(params);
    }

    public void updateUsingProcedure(Usuario usuario, String passwordHash) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("P_ID_USUARIO", usuario.getId())
                .addValue("P_NOMBRE", usuario.getNombre().trim())
                .addValue("P_PRIMER_APELLIDO", usuario.getPrimerApellido().trim())
                .addValue("P_SEGUNDO_APELLIDO", emptyToNull(usuario.getSegundoApellido()))
                .addValue("P_FECHA_NACIMIENTO", toTimestamp(usuario.getFechaNacimiento()), Types.DATE)
                .addValue("P_USERNAME", usuario.getUsername().trim())
                .addValue("P_PASSWORD", passwordHash)
                .addValue("P_TIPO_USUARIO", usuario.getTipoUsuario())
                .addValue("P_ESTADO", usuario.getEstado());
        actualizarUsuario.execute(params);
    }

    private static Rol mapRol(java.sql.ResultSet rs) throws java.sql.SQLException {
        Rol rol = new Rol();
        rol.setId(rs.getLong("ID_ROL"));
        rol.setNombre(rs.getString("NOMBRE_ROL"));
        var metadata = rs.getMetaData();
        for (int index = 1; index <= metadata.getColumnCount(); index++) {
            if ("ID_ESTADO".equalsIgnoreCase(metadata.getColumnLabel(index))) {
                rol.setEstado(rs.getLong(index));
                break;
            }
        }
        return rol;
    }

    private static LocalDate toLocalDate(java.sql.Date value) {
        return value == null ? null : value.toLocalDate();
    }

    private static java.time.LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private static Timestamp toTimestamp(java.time.LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private static Timestamp toTimestamp(LocalDate value) {
        return value == null ? null : Timestamp.valueOf(value.atStartOfDay());
    }

    private static Usuario mapUsuario(java.sql.ResultSet rs) throws java.sql.SQLException {
        Usuario usuario = new Usuario();
        usuario.setId(rs.getLong("ID_USUARIO"));
        usuario.setNombre(rs.getString("NOMBRE"));
        usuario.setPrimerApellido(rs.getString("PRIMER_APELLIDO"));
        usuario.setSegundoApellido(rs.getString("SEGUNDO_APELLIDO"));
        usuario.setFechaNacimiento(toLocalDate(rs.getDate("FECHA_NACIMIENTO")));
        usuario.setUsername(rs.getString("USERNAME"));
        usuario.setPassword(rs.getString("PASSWORD"));
        usuario.setFechaCreacion(toLocalDateTime(rs.getTimestamp("FECHA_CREACION")));
        usuario.setFechaModificacion(toLocalDateTime(rs.getTimestamp("FECHA_MODIFICACION")));
        usuario.setFechaEliminacion(toLocalDateTime(rs.getTimestamp("FECHA_ELIMINACION")));
        usuario.setTipoUsuario(rs.getLong("ID_TIPO_USUARIO"));
        usuario.setTipoUsuarioNombre(rs.getString("NOMBRE_TIPO_USUARIO"));
        usuario.setEstado(rs.getLong("ID_ESTADO"));
        usuario.setEstadoNombre(rs.getString("NOMBRE_ESTADO"));
        return usuario;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> cursor(Map<String, Object> output) {
        return (List<T>) output.get("P_RESULTADO");
    }
}
