package com.rescatevial.repository;

import com.rescatevial.domain.ConfiguracionRegistro;
import com.rescatevial.domain.ConfiguracionTipo;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

/** Únicamente invoca procedimientos del paquete Oracle. No ejecuta SQL directo sobre tablas. */
@Repository
public class ConfiguracionRepository {
    private static final String PAQUETE = "FIDE_PROYECTO_FINAL_RESCATE_VIAL_CR_PCK";
    private final JdbcTemplate jdbc;
    public ConfiguracionRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    private SimpleJdbcCall llamada(String nombre, SqlParameter... parametros) {
        return new SimpleJdbcCall(jdbc)
                .withCatalogName(PAQUETE)
                .withProcedureName(nombre)
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(parametros);
    }
    private static SqlParameter numero(String nombre) { return new SqlParameter(nombre, Types.NUMERIC); }
    private static SqlParameter texto(String nombre) { return new SqlParameter(nombre, Types.VARCHAR); }

    private static ConfiguracionRegistro fila(ResultSet rs, ConfiguracionTipo tipo) throws SQLException {
        ConfiguracionRegistro r = new ConfiguracionRegistro();
        switch (tipo) {
            case ESTADOS -> {
                r.setId(rs.getLong("ID_ESTADO"));
                r.setNombre(rs.getString("NOMBRE_ESTADO"));
            }
            case TIPOS_USUARIOS -> {
                r.setId(rs.getLong("ID_TIPO_USUARIO"));
                r.setNombre(rs.getString("NOMBRE_TIPO_USUARIO"));
                r.setIdEstado(rs.getLong("ID_ESTADO"));
            }
            case PROVINCIAS -> {
                r.setId(rs.getLong("ID_PROVINCIA"));
                r.setNombre(rs.getString("NOMBRE_PROVINCIA"));
                r.setIdEstado(rs.getLong("ID_ESTADO"));
            }
            case CANTONES -> {
                r.setId(rs.getLong("ID_CANTON"));
                r.setNombre(rs.getString("NOMBRE_CANTON"));
                r.setIdEstado(rs.getLong("ID_ESTADO"));
                r.setIdPadre(rs.getLong("ID_PROVINCIA"));
            }
            case DISTRITOS -> {
                r.setId(rs.getLong("ID_DISTRITO"));
                r.setNombre(rs.getString("NOMBRE_DISTRITO"));
                r.setIdEstado(rs.getLong("ID_ESTADO"));
                r.setIdPadre(rs.getLong("ID_CANTON"));
            }
        }
        return r;
    }

    @SuppressWarnings("unchecked")
    public List<ConfiguracionRegistro> listar(ConfiguracionTipo tipo) {
        String nombre = switch (tipo) {
            case ESTADOS -> "FIDE_OBTENER_ESTADOS_SP";
            case TIPOS_USUARIOS -> "FIDE_OBTENER_TIPOS_USUARIOS_SP";
            case PROVINCIAS -> "FIDE_OBTENER_PROVINCIAS_SP";
            case CANTONES -> "FIDE_OBTENER_CANTONES_SP";
            case DISTRITOS -> "FIDE_OBTENER_DISTRITOS_SP";
        };
        var call = llamada(nombre, new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR,
                (rs, i) -> fila(rs, tipo)));
        Map<String, Object> result = call.execute(new MapSqlParameterSource());
        List<ConfiguracionRegistro> lista = (List<ConfiguracionRegistro>) result.get("P_RESULTADO");
        return lista == null ? List.of() : lista;
    }

    /** Las firmas corresponden a ENCABEZADO.SQL del proyecto. */
    public void insertar(ConfiguracionTipo tipo, ConfiguracionRegistro r) {
        switch (tipo) {
            case ESTADOS -> llamada("FIDE_INSERTAR_ESTADO_SP", texto("P_NOMBRE_ESTADO"))
                    .execute(new MapSqlParameterSource("P_NOMBRE_ESTADO", r.getNombre()));
            case TIPOS_USUARIOS -> llamada("FIDE_INSERTAR_TIPOS_USUARIOS_SP",
                    texto("P_NOMBRE_TIPO_USUARIO"), numero("P_ID_ESTADO"))
                    .execute(new MapSqlParameterSource("P_NOMBRE_TIPO_USUARIO", r.getNombre())
                            .addValue("P_ID_ESTADO", 1L));
            case PROVINCIAS -> llamada("FIDE_INSERTAR_PROVINCIAS_SP",
                    numero("P_ID_PROVINCIA"), texto("P_NOMBRE_PROVINCIA"), numero("P_ID_ESTADO"))
                    .execute(new MapSqlParameterSource("P_ID_PROVINCIA", r.getId())
                            .addValue("P_NOMBRE_PROVINCIA", r.getNombre())
                            .addValue("P_ID_ESTADO", 1L));
            case CANTONES -> llamada("FIDE_INSERTAR_CANTONES_SP",
                    numero("P_ID_CANTON"), texto("P_NOMBRE_CANTON"), numero("P_ID_PROVINCIA"), numero("P_ID_ESTADO"))
                    .execute(new MapSqlParameterSource("P_ID_CANTON", r.getId())
                            .addValue("P_NOMBRE_CANTON", r.getNombre())
                            .addValue("P_ID_PROVINCIA", r.getIdPadre()).addValue("P_ID_ESTADO", 1L));
            case DISTRITOS -> llamada("FIDE_INSERTAR_DISTRITOS_SP",
                    numero("P_ID_DISTRITO"), texto("P_NOMBRE_DISTRITO"), numero("P_ID_CANTON"), numero("P_ID_ESTADO"))
                    .execute(new MapSqlParameterSource("P_ID_DISTRITO", r.getId())
                            .addValue("P_NOMBRE_DISTRITO", r.getNombre())
                            .addValue("P_ID_CANTON", r.getIdPadre()).addValue("P_ID_ESTADO", 1L));
        }
    }

    public void modificar(ConfiguracionTipo tipo, ConfiguracionRegistro r) {
        switch (tipo) {
            case ESTADOS -> llamada("FIDE_MODIFICAR_ESTADO_SP",
                    numero("P_ID_ESTADO"), texto("P_NOMBRE_ESTADO"))
                    .execute(new MapSqlParameterSource("P_ID_ESTADO", r.getId())
                            .addValue("P_NOMBRE_ESTADO", r.getNombre()));
            case TIPOS_USUARIOS -> llamada("FIDE_MODIFICAR_TIPOS_USUARIOS_SP",
                    numero("P_ID_TIPO_USUARIO"), texto("P_NOMBRE_TIPO_USUARIO"), numero("P_ID_ESTADO"))
                    .execute(new MapSqlParameterSource("P_ID_TIPO_USUARIO", r.getId())
                            .addValue("P_NOMBRE_TIPO_USUARIO", r.getNombre())
                            .addValue("P_ID_ESTADO", r.getIdEstado()));
            case PROVINCIAS -> llamada("FIDE_MODIFICAR_PROVINCIAS_SP",
                    numero("P_ID_PROVINCIA"), texto("P_NOMBRE_PROVINCIA"), numero("P_ID_ESTADO"))
                    .execute(new MapSqlParameterSource("P_ID_PROVINCIA", r.getId())
                            .addValue("P_NOMBRE_PROVINCIA", r.getNombre())
                            .addValue("P_ID_ESTADO", r.getIdEstado()));
            case CANTONES -> llamada("FIDE_MODIFICAR_CANTONES_SP",
                    numero("P_ID_CANTON"), texto("P_NOMBRE_CANTON"), numero("P_ID_PROVINCIA"), numero("P_ID_ESTADO"))
                    .execute(new MapSqlParameterSource("P_ID_CANTON", r.getId())
                            .addValue("P_NOMBRE_CANTON", r.getNombre())
                            .addValue("P_ID_PROVINCIA", r.getIdPadre())
                            .addValue("P_ID_ESTADO", r.getIdEstado()));
            case DISTRITOS -> llamada("FIDE_MODIFICAR_DISTRITOS_SP",
                    numero("P_ID_DISTRITO"), texto("P_NOMBRE_DISTRITO"), numero("P_ID_CANTON"), numero("P_ID_ESTADO"))
                    .execute(new MapSqlParameterSource("P_ID_DISTRITO", r.getId())
                            .addValue("P_NOMBRE_DISTRITO", r.getNombre())
                            .addValue("P_ID_CANTON", r.getIdPadre())
                            .addValue("P_ID_ESTADO", r.getIdEstado()));
        }
    }

    public void desactivar(ConfiguracionTipo tipo, Long id) {
        String nombre = switch (tipo) {
            case ESTADOS -> throw new IllegalArgumentException("Los estados del sistema no admiten desactivación.");
            case TIPOS_USUARIOS -> "FIDE_ELIMINAR_TIPOS_USUARIOS_SP";
            case PROVINCIAS -> "FIDE_ELIMINAR_PROVINCIAS_SP";
            case CANTONES -> "FIDE_ELIMINAR_CANTONES_SP";
            case DISTRITOS -> "FIDE_ELIMINAR_DISTRITOS_SP";
        };
        String argumento = switch (tipo) {
            case TIPOS_USUARIOS -> "P_ID_TIPO_USUARIO";
            case PROVINCIAS -> "P_ID_PROVINCIA";
            case CANTONES -> "P_ID_CANTON";
            case DISTRITOS -> "P_ID_DISTRITO";
            case ESTADOS -> throw new IllegalArgumentException("Operación no disponible.");
        };
        llamada(nombre, numero(argumento)).execute(new MapSqlParameterSource(argumento, id));
    }
}
