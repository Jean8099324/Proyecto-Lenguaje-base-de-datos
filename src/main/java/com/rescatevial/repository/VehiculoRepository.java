package com.rescatevial.repository;

import com.rescatevial.domain.OpcionVehiculo;
import com.rescatevial.domain.Vehiculo;
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

/** No consulta tablas con SQL desde Java: todo pasa por el PACKAGE Oracle. */
@Repository
public class VehiculoRepository {
    private static final String PAQUETE = "FIDE_PROYECTO_FINAL_RESCATE_VIAL_CR_PCK";
    private final JdbcTemplate jdbc;

    public VehiculoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private SimpleJdbcCall procedimiento(String nombre, SqlParameter... parametros) {
        return new SimpleJdbcCall(jdbc)
                .withCatalogName(PAQUETE)
                .withProcedureName(nombre)
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(parametros);
    }

    private static Vehiculo mapVehiculo(ResultSet rs, int fila) throws SQLException {
        Vehiculo v = new Vehiculo();
        v.setId(rs.getLong("ID_VEHICULO"));
        v.setPlaca(rs.getString("PLACA"));
        v.setCodigoChasis(rs.getString("CODIGO_CHASIS"));
        v.setIdMarca(rs.getLong("ID_MARCA"));
        v.setIdModelo(rs.getLong("ID_MODELO"));
        v.setIdAnio(rs.getLong("ID_ANIO"));
        v.setIdEstado(rs.getLong("ID_ESTADO"));
        v.setIdUsuario(rs.getLong("ID_USUARIO"));
        return v;
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> cursor(Map<String, Object> salida) {
        return (List<T>) salida.get("P_RESULTADO");
    }

    public List<Vehiculo> listar() {
        var call = procedimiento("FIDE_OBTENER_VEHICULOS_SP",
                new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR, VehiculoRepository::mapVehiculo));
        return cursor(call.execute(new MapSqlParameterSource()));
    }

    public Optional<Vehiculo> buscar(Long id) {
        var call = procedimiento("FIDE_OBTENER_VEHICULO_POR_ID_SP",
                new SqlParameter("P_ID_VEHICULO", Types.NUMERIC),
                new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR, VehiculoRepository::mapVehiculo));
        List<Vehiculo> registros = cursor(call.execute(new MapSqlParameterSource("P_ID_VEHICULO", id)));
        return registros.stream().findFirst();
    }

    public List<OpcionVehiculo> marcas() {
        var call = procedimiento("FIDE_OBTENER_MARCAS_SP",
                new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR, (rs, fila) -> {
                    OpcionVehiculo dato = new OpcionVehiculo();
                    dato.setId(rs.getLong("ID_MARCA"));
                    dato.setNombre(rs.getString("NOMBRE_MARCA"));
                    dato.setEstadoId(rs.getLong("ID_ESTADO"));
                    return dato;
                }));
        return cursor(call.execute(new MapSqlParameterSource()));
    }

    public List<OpcionVehiculo> modelos() {
        var call = procedimiento("FIDE_OBTENER_MODELOS_SP",
                new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR, (rs, fila) -> {
                    OpcionVehiculo dato = new OpcionVehiculo();
                    dato.setId(rs.getLong("ID_MODELO"));
                    dato.setNombre(rs.getString("NOMBRE_MODELO"));
                    dato.setPadreId(rs.getLong("ID_MARCA"));
                    dato.setEstadoId(rs.getLong("ID_ESTADO"));
                    return dato;
                }));
        return cursor(call.execute(new MapSqlParameterSource()));
    }

    public List<OpcionVehiculo> anios() {
        var call = procedimiento("FIDE_OBTENER_ANIOS_SP",
                new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR, (rs, fila) -> {
                    OpcionVehiculo dato = new OpcionVehiculo();
                    dato.setId(rs.getLong("ID_ANIO"));
                    Object anio = rs.getObject("ANIO");
                    dato.setNombre(anio == null ? "Sin año" : String.valueOf(anio));
                    dato.setPadreId(rs.getLong("ID_MODELO"));
                    dato.setEstadoId(rs.getLong("ID_ESTADO"));
                    return dato;
                }));
        return cursor(call.execute(new MapSqlParameterSource()));
    }

    private static MapSqlParameterSource parametros(Vehiculo v) {
        return new MapSqlParameterSource()
                .addValue("P_PLACA", v.getPlaca(), Types.VARCHAR)
                .addValue("P_CODIGO_CHASIS", v.getCodigoChasis(), Types.VARCHAR)
                .addValue("P_ID_MARCA", v.getIdMarca(), Types.NUMERIC)
                .addValue("P_ID_MODELO", v.getIdModelo(), Types.NUMERIC)
                .addValue("P_ID_ANIO", v.getIdAnio(), Types.NUMERIC)
                .addValue("P_ID_ESTADO", v.getIdEstado(), Types.NUMERIC)
                .addValue("P_ID_USUARIO", v.getIdUsuario(), Types.NUMERIC);
    }

    public void insertar(Vehiculo v) {
        var call = procedimiento("FIDE_INSERTAR_VEHICULOS_SP",
                new SqlParameter("P_PLACA", Types.VARCHAR),
                new SqlParameter("P_CODIGO_CHASIS", Types.VARCHAR),
                new SqlParameter("P_ID_MARCA", Types.NUMERIC),
                new SqlParameter("P_ID_MODELO", Types.NUMERIC),
                new SqlParameter("P_ID_ANIO", Types.NUMERIC),
                new SqlParameter("P_ID_ESTADO", Types.NUMERIC),
                new SqlParameter("P_ID_USUARIO", Types.NUMERIC));
        call.execute(parametros(v));
    }

    public void modificar(Vehiculo v) {
        var call = procedimiento("FIDE_MODIFICAR_VEHICULOS_SP",
                new SqlParameter("P_ID_VEHICULO", Types.NUMERIC),
                new SqlParameter("P_PLACA", Types.VARCHAR),
                new SqlParameter("P_CODIGO_CHASIS", Types.VARCHAR),
                new SqlParameter("P_ID_MARCA", Types.NUMERIC),
                new SqlParameter("P_ID_MODELO", Types.NUMERIC),
                new SqlParameter("P_ID_ANIO", Types.NUMERIC),
                new SqlParameter("P_ID_ESTADO", Types.NUMERIC),
                new SqlParameter("P_ID_USUARIO", Types.NUMERIC));
        call.execute(parametros(v).addValue("P_ID_VEHICULO", v.getId(), Types.NUMERIC));
    }

    public void desactivar(Long id) {
        var call = procedimiento("FIDE_ELIMINAR_VEHICULOS_SP",
                new SqlParameter("P_ID_VEHICULO", Types.NUMERIC));
        call.execute(new MapSqlParameterSource("P_ID_VEHICULO", id));
    }
}
