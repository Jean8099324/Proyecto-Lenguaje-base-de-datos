package com.rescatevial.repository;

import com.rescatevial.domain.Unidad;
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

@Repository
public class UnidadRepository {
    private static final String PACKAGE = "FIDE_PROYECTO_FINAL_RESCATE_VIAL_CR_PCK";
    private final JdbcTemplate jdbc;
    public UnidadRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    private SimpleJdbcCall call(String name, SqlParameter... params) { return new SimpleJdbcCall(jdbc).withCatalogName(PACKAGE).withProcedureName(name).withoutProcedureColumnMetaDataAccess().declareParameters(params); }
    private static Unidad map(java.sql.ResultSet rs, int row) throws java.sql.SQLException { Unidad u = new Unidad(); u.setId(rs.getLong("ID_UNIDAD")); u.setNombre(rs.getString("NOMBRE_UNIDAD")); u.setIdTipo(rs.getLong("ID_TIPO_UNIDAD")); u.setIdEstado(rs.getLong("ID_ESTADO")); u.setPlaca(rs.getString("PLACA_UNIDAD")); u.setTipoNombre(rs.getString("NOMBRE_TIPO_UNIDAD")); u.setEstadoNombre(rs.getString("NOMBRE_ESTADO")); var incidente = rs.getObject("ID_INCIDENTE"); u.setIncidenteAsignadoId(incidente instanceof Number number ? number.longValue() : null); return u; }
    private static Unidad mapBasica(java.sql.ResultSet rs, int row) throws java.sql.SQLException { Unidad u = new Unidad(); u.setId(rs.getLong("ID_UNIDAD")); u.setNombre(rs.getString("NOMBRE_UNIDAD")); u.setIdTipo(rs.getLong("ID_TIPO_UNIDAD")); u.setIdEstado(rs.getLong("ID_ESTADO")); u.setPlaca(rs.getString("PLACA_UNIDAD")); return u; }
    @SuppressWarnings("unchecked") private static <T> List<T> cursor(Map<String,Object> out) { return (List<T>) out.get("P_RESULTADO"); }
    public List<Unidad> listar() { return cursor(call("FIDE_OBTENER_UNIDADES_SP", new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR, UnidadRepository::map)).execute(new MapSqlParameterSource())); }
    public Optional<Unidad> buscar(Long id) { var out=call("FIDE_OBTENER_UNIDAD_POR_ID_SP", new SqlParameter("P_ID_UNIDAD",Types.NUMERIC),new SqlOutParameter("P_RESULTADO",Types.REF_CURSOR,UnidadRepository::mapBasica)).execute(new MapSqlParameterSource("P_ID_UNIDAD",id)); return UnidadRepository.<Unidad>cursor(out).stream().findFirst(); }
    private MapSqlParameterSource params(Unidad u) { return new MapSqlParameterSource().addValue("P_NOMBRE_UNIDAD",u.getNombre(),Types.VARCHAR).addValue("P_ID_TIPO_UNIDAD",u.getIdTipo(),Types.NUMERIC).addValue("P_ID_ESTADO",u.getIdEstado(),Types.NUMERIC).addValue("P_PLACA_UNIDAD",u.getPlaca(),Types.VARCHAR); }
    public void insertar(Unidad u) { call("FIDE_INSERTAR_UNIDADES_SP",new SqlParameter("P_NOMBRE_UNIDAD",Types.VARCHAR),new SqlParameter("P_ID_TIPO_UNIDAD",Types.NUMERIC),new SqlParameter("P_ID_ESTADO",Types.NUMERIC),new SqlParameter("P_PLACA_UNIDAD",Types.VARCHAR)).execute(params(u)); }
    public void modificar(Unidad u) { call("FIDE_MODIFICAR_UNIDADES_SP",new SqlParameter("P_ID_UNIDAD",Types.NUMERIC),new SqlParameter("P_NOMBRE_UNIDAD",Types.VARCHAR),new SqlParameter("P_ID_TIPO_UNIDAD",Types.NUMERIC),new SqlParameter("P_ID_ESTADO",Types.NUMERIC),new SqlParameter("P_PLACA_UNIDAD",Types.VARCHAR)).execute(params(u).addValue("P_ID_UNIDAD",u.getId(),Types.NUMERIC)); }
    public void desactivar(Long id) { call("FIDE_ELIMINAR_UNIDADES_SP",new SqlParameter("P_ID_UNIDAD",Types.NUMERIC)).execute(new MapSqlParameterSource("P_ID_UNIDAD",id)); }
    public List<Map<String,Object>> historiales() {
        return OraclePackageSupport.rows(OraclePackageSupport.call(jdbc,
                        "FIDE_OBTENER_HISTORIALES_UNIDADES_SP", OraclePackageSupport.cursor())
                .execute(new MapSqlParameterSource()));
    }

    public List<Map<String,Object>> historial(Long id) {
        Map<Long, String> estados = new java.util.HashMap<>();
        OraclePackageSupport.rows(OraclePackageSupport.call(jdbc, "FIDE_OBTENER_ESTADOS_SP",
                        OraclePackageSupport.cursor()).execute(new MapSqlParameterSource()))
                .forEach(row -> estados.put(((Number) row.get("ID_ESTADO")).longValue(),
                        String.valueOf(row.get("NOMBRE_ESTADO"))));

        return historiales().stream()
                .filter(row -> id.equals(((Number) row.get("ID_UNIDAD")).longValue()))
                .sorted((left, right) -> Long.compare(
                        ((Number) right.get("ID_HISTORIAL")).longValue(),
                        ((Number) left.get("ID_HISTORIAL")).longValue()))
                .map(row -> {
                    Map<String, Object> historial = new java.util.LinkedHashMap<>(row);
                    Number estadoId = (Number) row.get("ID_ESTADO");
                    historial.put("NOMBRE_ESTADO", estadoId == null ? null : estados.get(estadoId.longValue()));
                    return historial;
                })
                .toList();
    }
}
