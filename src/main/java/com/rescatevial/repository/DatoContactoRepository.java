package com.rescatevial.repository;

import com.rescatevial.domain.DatoContacto;
import com.rescatevial.domain.OpcionUbicacion;
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

/** Solo invoca procedimientos del paquete Oracle; nunca consulta tablas desde Java. */
@Repository
public class DatoContactoRepository {
    private static final String PAQUETE = "FIDE_PROYECTO_FINAL_RESCATE_VIAL_CR_PCK";
    private final JdbcTemplate jdbc;
    public DatoContactoRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private SimpleJdbcCall procedimiento(String nombre, SqlParameter... parametros) {
        return new SimpleJdbcCall(jdbc)
                .withCatalogName(PAQUETE)
                .withProcedureName(nombre)
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(parametros);
    }
    @SuppressWarnings("unchecked")
    private static <T> List<T> resultado(Map<String, Object> salida) {
        return (List<T>) salida.get("P_RESULTADO");
    }
    private static DatoContacto telefono(ResultSet rs, int i) throws SQLException {
        DatoContacto d = new DatoContacto();
        d.setIdUsuario(rs.getLong("ID_USUARIO"));
        d.setValor(rs.getString("NUMERO_TELEFONO"));
        d.setIdEstado(rs.getLong("ID_ESTADO"));
        return d;
    }
    private static DatoContacto correo(ResultSet rs, int i) throws SQLException {
        DatoContacto d = new DatoContacto();
        d.setIdUsuario(rs.getLong("ID_USUARIO"));
        d.setValor(rs.getString("CORREO"));
        d.setIdEstado(rs.getLong("ID_ESTADO"));
        return d;
    }
    private static DatoContacto direccion(ResultSet rs, int i) throws SQLException {
        DatoContacto d = new DatoContacto();
        d.setIdUsuario(rs.getLong("ID_USUARIO"));
        d.setIdProvincia(rs.getLong("ID_PROVINCIA"));
        d.setIdCanton(rs.getLong("ID_CANTON"));
        d.setIdDistrito(rs.getLong("ID_DISTRITO"));
        d.setOtrasSenas(rs.getString("OTRAS_SENAS"));
        d.setIdEstado(rs.getLong("ID_ESTADO"));
        return d;
    }

    public List<DatoContacto> telefonos(Long usuario) {
        var proc = procedimiento("FIDE_OBTENER_TELEFONOS_POR_USUARIO_SP",
            new SqlParameter("P_ID_USUARIO", Types.NUMERIC),
            new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR, DatoContactoRepository::telefono));
        return resultado(proc.execute(new MapSqlParameterSource("P_ID_USUARIO", usuario)));
    }
    public List<DatoContacto> correos(Long usuario) {
        var proc = procedimiento("FIDE_OBTENER_CORREOS_POR_USUARIO_SP",
            new SqlParameter("P_ID_USUARIO", Types.NUMERIC),
            new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR, DatoContactoRepository::correo));
        return resultado(proc.execute(new MapSqlParameterSource("P_ID_USUARIO", usuario)));
    }

    public List<DatoContacto> correos() {
        var proc = procedimiento("FIDE_OBTENER_CORREOS_SP",
            new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR, DatoContactoRepository::correo));
        return resultado(proc.execute(new MapSqlParameterSource()));
    }

public List<DatoContacto> direcciones(Long usuario) {
    var proc = procedimiento("FIDE_OBTENER_DIRECCIONES_SP",
        new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR, DatoContactoRepository::direccion));

    List<DatoContacto> direcciones = resultado(
        proc.execute(new MapSqlParameterSource())
    );

    return direcciones.stream()
            .filter(d -> usuario.equals(d.getIdUsuario()))
            .toList();
}


    private static SqlParameter[] telefonoCampos(boolean editar) {
        return editar ? new SqlParameter[]{
            new SqlParameter("P_ID_USUARIO", Types.NUMERIC),
            new SqlParameter("P_NUMERO_TELEFONO", Types.VARCHAR),
            new SqlParameter("P_NUEVO_NUMERO_TELEFONO", Types.VARCHAR),
            new SqlParameter("P_ID_ESTADO", Types.NUMERIC)} : new SqlParameter[]{
            new SqlParameter("P_ID_USUARIO", Types.NUMERIC),
            new SqlParameter("P_NUMERO_TELEFONO", Types.VARCHAR),
            new SqlParameter("P_ID_ESTADO", Types.NUMERIC)};
    }
    public void insertarTelefono(DatoContacto d) {
        procedimiento("FIDE_INSERTAR_TELEFONOS_SP", telefonoCampos(false))
            .execute(new MapSqlParameterSource()
                .addValue("P_ID_USUARIO", d.getIdUsuario())
                .addValue("P_NUMERO_TELEFONO", d.getValor())
                .addValue("P_ID_ESTADO", d.getIdEstado()));
    }
    public void modificarTelefono(Long id, String original, String nuevo, Long estado) {
        procedimiento("FIDE_MODIFICAR_TELEFONOS_SP", telefonoCampos(true))
            .execute(new MapSqlParameterSource()
                .addValue("P_ID_USUARIO", id).addValue("P_NUMERO_TELEFONO", original)
                .addValue("P_NUEVO_NUMERO_TELEFONO", nuevo).addValue("P_ID_ESTADO", estado));
    }
    public void eliminarTelefono(Long id, String valor) {
        procedimiento("FIDE_ELIMINAR_TELEFONOS_SP",
            new SqlParameter("P_ID_USUARIO", Types.NUMERIC),
            new SqlParameter("P_NUMERO_TELEFONO", Types.VARCHAR))
            .execute(new MapSqlParameterSource("P_ID_USUARIO", id).addValue("P_NUMERO_TELEFONO", valor));
    }

    public void insertarCorreo(DatoContacto d) {
        procedimiento("FIDE_INSERTAR_CORREOS_SP",
            new SqlParameter("P_ID_USUARIO", Types.NUMERIC),
            new SqlParameter("P_CORREO", Types.VARCHAR),
            new SqlParameter("P_ID_ESTADO", Types.NUMERIC))
            .execute(new MapSqlParameterSource("P_ID_USUARIO", d.getIdUsuario())
                .addValue("P_CORREO", d.getValor()).addValue("P_ID_ESTADO", d.getIdEstado()));
    }
    public void modificarCorreo(Long id, String original, String nuevo, Long estado) {
        procedimiento("FIDE_MODIFICAR_CORREOS_SP",
            new SqlParameter("P_ID_USUARIO", Types.NUMERIC),
            new SqlParameter("P_CORREO", Types.VARCHAR),
            new SqlParameter("P_NUEVO_CORREO", Types.VARCHAR),
            new SqlParameter("P_ID_ESTADO", Types.NUMERIC))
            .execute(new MapSqlParameterSource("P_ID_USUARIO", id)
                .addValue("P_CORREO", original).addValue("P_NUEVO_CORREO", nuevo)
                .addValue("P_ID_ESTADO", estado));
    }
    public void eliminarCorreo(Long id, String valor) {
        procedimiento("FIDE_ELIMINAR_CORREOS_SP",
            new SqlParameter("P_ID_USUARIO", Types.NUMERIC),
            new SqlParameter("P_CORREO", Types.VARCHAR))
            .execute(new MapSqlParameterSource("P_ID_USUARIO", id).addValue("P_CORREO", valor));
    }

    private static SqlParameter[] direccionCampos() {
        return new SqlParameter[]{
            new SqlParameter("P_ID_USUARIO", Types.NUMERIC),
            new SqlParameter("P_ID_PROVINCIA", Types.NUMERIC),
            new SqlParameter("P_ID_CANTON", Types.NUMERIC),
            new SqlParameter("P_ID_DISTRITO", Types.NUMERIC),
            new SqlParameter("P_OTRAS_SENAS", Types.VARCHAR),
            new SqlParameter("P_ID_ESTADO", Types.NUMERIC)};
    }
    private static MapSqlParameterSource direccionParametros(DatoContacto d) {
        return new MapSqlParameterSource("P_ID_USUARIO", d.getIdUsuario())
            .addValue("P_ID_PROVINCIA", d.getIdProvincia())
            .addValue("P_ID_CANTON", d.getIdCanton())
            .addValue("P_ID_DISTRITO", d.getIdDistrito())
            .addValue("P_OTRAS_SENAS", d.getOtrasSenas())
            .addValue("P_ID_ESTADO", d.getIdEstado());
    }
    public void insertarDireccion(DatoContacto d) {
        procedimiento("FIDE_INSERTAR_DIRECCIONES_SP", direccionCampos())
            .execute(direccionParametros(d));
    }
    public void modificarDireccion(DatoContacto d) {
        procedimiento("FIDE_MODIFICAR_DIRECCIONES_SP", direccionCampos())
            .execute(direccionParametros(d));
    }
    public void eliminarDireccion(DatoContacto d) {
        procedimiento("FIDE_ELIMINAR_DIRECCIONES_SP",
            new SqlParameter("P_ID_USUARIO", Types.NUMERIC),
            new SqlParameter("P_ID_PROVINCIA", Types.NUMERIC),
            new SqlParameter("P_ID_CANTON", Types.NUMERIC),
            new SqlParameter("P_ID_DISTRITO", Types.NUMERIC))
            .execute(new MapSqlParameterSource("P_ID_USUARIO", d.getIdUsuario())
                .addValue("P_ID_PROVINCIA", d.getIdProvincia())
                .addValue("P_ID_CANTON", d.getIdCanton())
                .addValue("P_ID_DISTRITO", d.getIdDistrito()));
    }

    private static OpcionUbicacion opcion(ResultSet rs, String columnaId,
                                             String columnaNombre, String columnaPadre) throws SQLException {
        OpcionUbicacion o = new OpcionUbicacion();
        o.setId(rs.getLong(columnaId));
        o.setNombre(rs.getString(columnaNombre));
        o.setIdEstado(rs.getLong("ID_ESTADO"));
        if (columnaPadre != null) o.setIdPadre(rs.getLong(columnaPadre));
        return o;
    }
    public List<OpcionUbicacion> provincias() {
        var proc = procedimiento("FIDE_OBTENER_PROVINCIAS_SP",
            new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR,
                (rs, i) -> opcion(rs, "ID_PROVINCIA", "NOMBRE_PROVINCIA", null)));
        return resultado(proc.execute(new MapSqlParameterSource()));
    }
    public List<OpcionUbicacion> cantones() {
        var proc = procedimiento("FIDE_OBTENER_CANTONES_SP",
            new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR,
                (rs, i) -> opcion(rs, "ID_CANTON", "NOMBRE_CANTON", "ID_PROVINCIA")));
        return resultado(proc.execute(new MapSqlParameterSource()));
    }
    public List<OpcionUbicacion> distritos() {
        var proc = procedimiento("FIDE_OBTENER_DISTRITOS_SP",
            new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR,
                (rs, i) -> opcion(rs, "ID_DISTRITO", "NOMBRE_DISTRITO", "ID_CANTON")));
        return resultado(proc.execute(new MapSqlParameterSource()));
    }
}
