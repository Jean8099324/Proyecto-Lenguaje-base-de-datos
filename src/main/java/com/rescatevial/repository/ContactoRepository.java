package com.rescatevial.repository;

import com.rescatevial.domain.Contacto;
import com.rescatevial.domain.ContactoIncidente;
import com.rescatevial.domain.IncidenteOpcion;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

/** Todas las operaciones Oracle pasan por procedimientos del PACKAGE, sin SQL directo. */
@Repository
public class ContactoRepository {
    private static final String PAQUETE = "FIDE_PROYECTO_FINAL_RESCATE_VIAL_CR_PCK";
    private final JdbcTemplate jdbc;

    public ContactoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private SimpleJdbcCall llamar(String nombre, SqlParameter... parametros) {
        return new SimpleJdbcCall(jdbc)
                .withCatalogName(PAQUETE)
                .withProcedureName(nombre)
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(parametros);
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> obtenerLista(Map<String, Object> resultado) {
        List<T> datos = (List<T>) resultado.get("P_RESULTADO");
        return datos == null ? List.of() : datos;
    }

    private static Contacto mapContacto(ResultSet rs, int fila) throws SQLException {
        Contacto c = new Contacto();
        c.setId(rs.getLong("ID_CONTACTO"));
        c.setNombreContacto(rs.getString("NOMBRE_CONTACTO"));
        c.setPrimerApellido(rs.getString("PRIMER_APELLIDO"));
        c.setSegundoApellido(rs.getString("SEGUNDO_APELLIDO"));
        c.setTelefonoContacto(rs.getString("TELEFONO_CONTACTO"));
        c.setCorreoContacto(rs.getString("CORREO_CONTACTO"));
        c.setIdEstado(rs.getLong("ID_ESTADO"));
        return c;
    }

    public List<Contacto> listar() {
        var call = llamar("FIDE_OBTENER_CONTACTOS_SP",
                new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR, ContactoRepository::mapContacto));
        return obtenerLista(call.execute(new MapSqlParameterSource()));
    }

    public Optional<Contacto> buscar(Long id) {
        var call = llamar("FIDE_OBTENER_CONTACTO_POR_ID_SP",
                new SqlParameter("P_ID_CONTACTO", Types.NUMERIC),
                new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR, ContactoRepository::mapContacto));
        List<Contacto> datos = obtenerLista(call.execute(new MapSqlParameterSource("P_ID_CONTACTO", id)));
        return datos.stream().findFirst();
    }

    private MapSqlParameterSource parametros(Contacto c) {
        return new MapSqlParameterSource()
                .addValue("P_NOMBRE_CONTACTO", c.getNombreContacto(), Types.VARCHAR)
                .addValue("P_PRIMER_APELLIDO", c.getPrimerApellido(), Types.VARCHAR)
                .addValue("P_SEGUNDO_APELLIDO", c.getSegundoApellido(), Types.VARCHAR)
                .addValue("P_TELEFONO_CONTACTO", c.getTelefonoContacto(), Types.VARCHAR)
                .addValue("P_CORREO_CONTACTO", c.getCorreoContacto(), Types.VARCHAR)
                .addValue("P_ID_ESTADO", c.getIdEstado(), Types.NUMERIC);
    }

    private SqlParameter[] camposContacto(boolean actualizar) {
        var lista = new java.util.ArrayList<SqlParameter>();
        if (actualizar) lista.add(new SqlParameter("P_ID_CONTACTO", Types.NUMERIC));
        lista.add(new SqlParameter("P_NOMBRE_CONTACTO", Types.VARCHAR));
        lista.add(new SqlParameter("P_PRIMER_APELLIDO", Types.VARCHAR));
        lista.add(new SqlParameter("P_SEGUNDO_APELLIDO", Types.VARCHAR));
        lista.add(new SqlParameter("P_TELEFONO_CONTACTO", Types.VARCHAR));
        lista.add(new SqlParameter("P_CORREO_CONTACTO", Types.VARCHAR));
        lista.add(new SqlParameter("P_ID_ESTADO", Types.NUMERIC));
        return lista.toArray(new SqlParameter[0]);
    }

    public void insertar(Contacto c) {
        llamar("FIDE_INSERTAR_CONTACTOS_SP", camposContacto(false)).execute(parametros(c));
    }

    public void modificar(Contacto c) {
        llamar("FIDE_MODIFICAR_CONTACTOS_SP", camposContacto(true))
                .execute(parametros(c).addValue("P_ID_CONTACTO", c.getId(), Types.NUMERIC));
    }

    public void desactivar(Long id) {
        llamar("FIDE_ELIMINAR_CONTACTOS_SP", new SqlParameter("P_ID_CONTACTO", Types.NUMERIC))
                .execute(new MapSqlParameterSource("P_ID_CONTACTO", id));
    }

    private static ContactoIncidente mapRelacion(ResultSet rs, int fila) throws SQLException {
        ContactoIncidente relacion = new ContactoIncidente();
        relacion.setIdIncidente(rs.getLong("ID_INCIDENTE"));
        relacion.setIdContacto(rs.getLong("ID_CONTACTO"));
        relacion.setIdEstado(rs.getLong("ID_ESTADO"));
        return relacion;
    }

    public List<ContactoIncidente> relaciones() {
        var call = llamar("FIDE_OBTENER_INCIDENTES_CONTACTOS_SP",
                new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR, ContactoRepository::mapRelacion));
        return obtenerLista(call.execute(new MapSqlParameterSource()));
    }

    public List<IncidenteOpcion> incidentes() {
        var call = llamar("FIDE_OBTENER_INCIDENTES_SP",
                new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR, (rs, fila) -> {
                    IncidenteOpcion opcion = new IncidenteOpcion();
                    opcion.setId(rs.getLong("ID_INCIDENTE"));
                    String detalles = rs.getString("DETALLES_INCIDENTE");
                    opcion.setDetalle(detalles == null || detalles.isBlank() ? "Sin descripción" : detalles);
                    return opcion;
                }));
        return obtenerLista(call.execute(new MapSqlParameterSource()));
    }

    private MapSqlParameterSource parametrosRelacion(Long incidente, Long contacto) {
        return new MapSqlParameterSource()
                .addValue("P_ID_INCIDENTE", incidente, Types.NUMERIC)
                .addValue("P_ID_CONTACTO", contacto, Types.NUMERIC);
    }

    public void asociar(Long incidente, Long contacto, Long estado) {
        llamar("FIDE_INSERTAR_INCIDENTES_CONTACTOS_SP",
                new SqlParameter("P_ID_INCIDENTE", Types.NUMERIC),
                new SqlParameter("P_ID_CONTACTO", Types.NUMERIC),
                new SqlParameter("P_ID_ESTADO", Types.NUMERIC))
                .execute(parametrosRelacion(incidente, contacto)
                        .addValue("P_ID_ESTADO", estado, Types.NUMERIC));
    }

    public void modificarRelacion(Long incidente, Long contacto, Long estado) {
        llamar("FIDE_MODIFICAR_INCIDENTES_CONTACTOS_SP",
                new SqlParameter("P_ID_INCIDENTE", Types.NUMERIC),
                new SqlParameter("P_ID_CONTACTO", Types.NUMERIC),
                new SqlParameter("P_ID_ESTADO", Types.NUMERIC))
                .execute(parametrosRelacion(incidente, contacto)
                        .addValue("P_ID_ESTADO", estado, Types.NUMERIC));
    }

    public void desactivarRelacion(Long incidente, Long contacto) {
        llamar("FIDE_ELIMINAR_INCIDENTES_CONTACTOS_SP",
                new SqlParameter("P_ID_INCIDENTE", Types.NUMERIC),
                new SqlParameter("P_ID_CONTACTO", Types.NUMERIC))
                .execute(parametrosRelacion(incidente, contacto));
    }
}
