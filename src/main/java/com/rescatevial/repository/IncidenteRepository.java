package com.rescatevial.repository;

import com.rescatevial.domain.Incidente;
import com.rescatevial.domain.OpcionIncidente;
import com.rescatevial.domain.VehiculoIncidenteForm;
import java.sql.Date;
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
public class IncidenteRepository {
    private final JdbcTemplate jdbc;

    public IncidenteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Incidente> findAll() {
        var call = new SimpleJdbcCall(jdbc).withCatalogName("FIDE_PROYECTO_FINAL_RESCATE_VIAL_CR_PCK")
                .withProcedureName("FIDE_OBTENER_INCIDENTES_SP").withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR,
                        (rs, row) -> mapIncidente(rs)));
        return cursor(call.execute(new MapSqlParameterSource()));
    }

    public Optional<Incidente> findById(Long id) {
        var call = new SimpleJdbcCall(jdbc).withCatalogName("FIDE_PROYECTO_FINAL_RESCATE_VIAL_CR_PCK")
                .withProcedureName("FIDE_OBTENER_INCIDENTE_POR_ID_SP").withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlParameter("P_ID_INCIDENTE", Types.NUMERIC),
                        new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR,
                                (rs, row) -> mapIncidente(rs)));
        Map<String, Object> result = call.execute(new MapSqlParameterSource("P_ID_INCIDENTE", id));
        return IncidenteRepository.<Incidente>cursor(result).stream().findFirst();
    }

    public void insert(Incidente incidente) {
        var call = new SimpleJdbcCall(jdbc).withCatalogName("FIDE_PROYECTO_FINAL_RESCATE_VIAL_CR_PCK")
                .withProcedureName("FIDE_INSERTAR_INCIDENTES_SP").withoutProcedureColumnMetaDataAccess()
                .declareParameters(incidentParameters(false));
        call.execute(parameters(incidente, false));
    }

    public void update(Incidente incidente) {
        var call = new SimpleJdbcCall(jdbc).withCatalogName("FIDE_PROYECTO_FINAL_RESCATE_VIAL_CR_PCK")
                .withProcedureName("FIDE_MODIFICAR_INCIDENTES_SP").withoutProcedureColumnMetaDataAccess()
                .declareParameters(incidentParameters(true));
        call.execute(parameters(incidente, true));
    }

    public void deactivate(Long id) {
        var call = new SimpleJdbcCall(jdbc).withCatalogName("FIDE_PROYECTO_FINAL_RESCATE_VIAL_CR_PCK")
                .withProcedureName("FIDE_ELIMINAR_INCIDENTES_SP").withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlParameter("P_ID_INCIDENTE", Types.NUMERIC));
        call.execute(new MapSqlParameterSource("P_ID_INCIDENTE", id));
    }

    public List<OpcionIncidente> findVehicles() {
        var call = new SimpleJdbcCall(jdbc).withCatalogName("FIDE_PROYECTO_FINAL_RESCATE_VIAL_CR_PCK")
                .withProcedureName("FIDE_OBTENER_VEHICULOS_SP").withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR, (rs, row) -> {
                    OpcionIncidente option = new OpcionIncidente();
                    option.setId(rs.getLong("ID_VEHICULO"));
                    option.setEtiqueta(rs.getString("PLACA") == null ? "Sin placa" : rs.getString("PLACA"));
                    option.setEstadoId(rs.getLong("ID_ESTADO"));
                    return option;
                }));
        return IncidenteRepository.<OpcionIncidente>cursor(call.execute(new MapSqlParameterSource())).stream()
                .filter(vehicle -> Long.valueOf(1).equals(vehicle.getEstadoId())).toList();
    }

    public List<OpcionIncidente> findBrands() {
        return options("FIDE_OBTENER_MARCAS_SP", "ID_MARCA", "NOMBRE_MARCA", null, "ID_ESTADO");
    }

    public List<OpcionIncidente> findModels() {
        return options("FIDE_OBTENER_MODELOS_SP", "ID_MODELO", "NOMBRE_MODELO", "ID_MARCA", "ID_ESTADO");
    }

    public List<OpcionIncidente> findYears() {
        return options("FIDE_OBTENER_ANIOS_SP", "ID_ANIO", "ANIO", "ID_MODELO", "ID_ESTADO");
    }

    public void insertVehicle(com.rescatevial.domain.VehiculoIncidenteForm vehicle) {
        var call = new SimpleJdbcCall(jdbc).withCatalogName("FIDE_PROYECTO_FINAL_RESCATE_VIAL_CR_PCK")
                .withProcedureName("FIDE_INSERTAR_VEHICULOS_SP").withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlParameter("P_PLACA", Types.VARCHAR),
                        new SqlParameter("P_CODIGO_CHASIS", Types.VARCHAR),
                        new SqlParameter("P_ID_MARCA", Types.NUMERIC),
                        new SqlParameter("P_ID_MODELO", Types.NUMERIC),
                        new SqlParameter("P_ID_ANIO", Types.NUMERIC),
                        new SqlParameter("P_ID_ESTADO", Types.NUMERIC),
                        new SqlParameter("P_ID_USUARIO", Types.NUMERIC));
        call.execute(new MapSqlParameterSource()
                .addValue("P_PLACA", vehicle.getPlaca().trim().toUpperCase(), Types.VARCHAR)
                .addValue("P_CODIGO_CHASIS", emptyToNull(vehicle.getCodigoChasis()), Types.VARCHAR)
                .addValue("P_ID_MARCA", vehicle.getIdMarca(), Types.NUMERIC)
                .addValue("P_ID_MODELO", vehicle.getIdModelo(), Types.NUMERIC)
                .addValue("P_ID_ANIO", vehicle.getIdAnio(), Types.NUMERIC)
                .addValue("P_ID_ESTADO", vehicle.getIdEstado(), Types.NUMERIC)
                .addValue("P_ID_USUARIO", vehicle.getIdUsuario(), Types.NUMERIC));
    }

    public Optional<OpcionIncidente> findVehicleByPlate(String plate) {
        String normalized = VehiculoIncidenteForm.normalizarPlaca(plate);
        return findAllVehicleRows().stream()
                .filter(vehicle -> normalized.equals(VehiculoIncidenteForm.normalizarPlaca(vehicle.getEtiqueta())))
                .findFirst();
    }

    private List<OpcionIncidente> findAllVehicleRows() {
        var call = new SimpleJdbcCall(jdbc).withCatalogName("FIDE_PROYECTO_FINAL_RESCATE_VIAL_CR_PCK")
                .withProcedureName("FIDE_OBTENER_VEHICULOS_SP").withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR, (rs, row) -> {
                    OpcionIncidente option = new OpcionIncidente();
                    option.setId(rs.getLong("ID_VEHICULO"));
                    option.setEtiqueta(rs.getString("PLACA"));
                    option.setEstadoId(rs.getLong("ID_ESTADO"));
                    return option;
                }));
        return cursor(call.execute(new MapSqlParameterSource()));
    }

    public List<OpcionIncidente> findIncidentTypes() {
        return options("FIDE_OBTENER_TIPOS_INCIDENTES_SP", "ID_TIPO_INCIDENTE", "NOMBRE_TIPO_INCIDENTE");
    }

    public List<OpcionIncidente> findPriorities() {
        return options("FIDE_OBTENER_PRIORIDADES_SP", "ID_PRIORIDAD", "NOMBRE_PRIORIDAD");
    }

    public List<OpcionIncidente> findLocations() {
        return options("FIDE_OBTENER_UBICACION_INCIDENTES_SP", "ID_UBICACION_INCIDENTE", "OTRAS_SENAS");
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private List<OpcionIncidente> options(String procedure, String idColumn, String labelColumn) {
        return options(procedure, idColumn, labelColumn, null, null);
    }

    private List<OpcionIncidente> options(String procedure, String idColumn, String labelColumn,
                                          String relatedColumn, String statusColumn) {
        var call = new SimpleJdbcCall(jdbc).withCatalogName("FIDE_PROYECTO_FINAL_RESCATE_VIAL_CR_PCK")
                .withProcedureName(procedure).withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR, (rs, row) -> {
                    OpcionIncidente option = new OpcionIncidente();
                    option.setId(rs.getLong(idColumn));
                    String label = rs.getString(labelColumn);
                    option.setEtiqueta(label == null || label.isBlank() ? "Registro " + option.getId() : label);
                    if (relatedColumn != null) option.setRelacionId(rs.getLong(relatedColumn));
                    if (statusColumn != null) option.setEstadoId(rs.getLong(statusColumn));
                    return option;
                }));
        List<OpcionIncidente> options = cursor(call.execute(new MapSqlParameterSource()));
        return statusColumn == null ? options : options.stream()
                .filter(option -> Long.valueOf(1).equals(option.getEstadoId())).toList();
    }

    private static SqlParameter[] incidentParameters(boolean withId) {
        if (withId) return new SqlParameter[] {
                new SqlParameter("P_ID_INCIDENTE", Types.NUMERIC),
                new SqlParameter("P_ID_VEHICULO", Types.NUMERIC),
                new SqlParameter("P_FECHA_REPORTE", Types.DATE),
                new SqlParameter("P_HORA_REPORTE", Types.VARCHAR),
                new SqlParameter("P_DETALLES_INCIDENTE", Types.VARCHAR),
                new SqlParameter("P_ID_UBICACION_INCIDENTE", Types.NUMERIC),
                new SqlParameter("P_ID_TIPO_INCIDENTE", Types.NUMERIC),
                new SqlParameter("P_ID_PRIORIDAD", Types.NUMERIC),
                new SqlParameter("P_ID_ESTADO", Types.NUMERIC),
                new SqlParameter("P_ID_USUARIO", Types.NUMERIC),
                new SqlParameter("P_FECHA_CIERRE", Types.DATE),
                new SqlParameter("P_HORA_CIERRE", Types.VARCHAR)
        };
        return new SqlParameter[] {
                new SqlParameter("P_ID_VEHICULO", Types.NUMERIC),
                new SqlParameter("P_FECHA_REPORTE", Types.DATE),
                new SqlParameter("P_HORA_REPORTE", Types.VARCHAR),
                new SqlParameter("P_DETALLES_INCIDENTE", Types.VARCHAR),
                new SqlParameter("P_ID_UBICACION_INCIDENTE", Types.NUMERIC),
                new SqlParameter("P_ID_TIPO_INCIDENTE", Types.NUMERIC),
                new SqlParameter("P_ID_PRIORIDAD", Types.NUMERIC),
                new SqlParameter("P_ID_ESTADO", Types.NUMERIC),
                new SqlParameter("P_ID_USUARIO", Types.NUMERIC),
                new SqlParameter("P_FECHA_CIERRE", Types.DATE),
                new SqlParameter("P_HORA_CIERRE", Types.VARCHAR)
        };
    }

    private static MapSqlParameterSource parameters(Incidente i, boolean withId) {
        var p = new MapSqlParameterSource();
        if (withId) p.addValue("P_ID_INCIDENTE", i.getId(), Types.NUMERIC);
        return p.addValue("P_ID_VEHICULO", i.getIdVehiculo(), Types.NUMERIC)
                .addValue("P_FECHA_REPORTE", sqlDate(i.getFechaReporte()), Types.DATE)
                .addValue("P_HORA_REPORTE", i.getHoraReporte(), Types.VARCHAR)
                .addValue("P_DETALLES_INCIDENTE", i.getDetalles(), Types.VARCHAR)
                .addValue("P_ID_UBICACION_INCIDENTE", i.getIdUbicacion(), Types.NUMERIC)
                .addValue("P_ID_TIPO_INCIDENTE", i.getIdTipoIncidente(), Types.NUMERIC)
                .addValue("P_ID_PRIORIDAD", i.getIdPrioridad(), Types.NUMERIC)
                .addValue("P_ID_ESTADO", i.getIdEstado(), Types.NUMERIC)
                .addValue("P_ID_USUARIO", i.getIdUsuario(), Types.NUMERIC)
                .addValue("P_FECHA_CIERRE", sqlDate(i.getFechaCierre()), Types.DATE)
                .addValue("P_HORA_CIERRE", i.getHoraCierre(), Types.VARCHAR);
    }

    private static Date sqlDate(java.time.LocalDate value) {
        return value == null ? null : Date.valueOf(value);
    }

    private static Incidente mapIncidente(java.sql.ResultSet rs) throws java.sql.SQLException {
        Incidente i = new Incidente();
        i.setId(rs.getLong("ID_INCIDENTE"));
        i.setIdVehiculo(rs.getLong("ID_VEHICULO"));
        i.setFechaReporte(rs.getDate("FECHA_REPORTE") == null ? null : rs.getDate("FECHA_REPORTE").toLocalDate());
        i.setHoraReporte(rs.getString("HORA_REPORTE"));
        i.setDetalles(rs.getString("DETALLES_INCIDENTE"));
        i.setIdUbicacion(rs.getLong("ID_UBICACION_INCIDENTE"));
        i.setIdTipoIncidente(rs.getLong("ID_TIPO_INCIDENTE"));
        i.setIdPrioridad(rs.getLong("ID_PRIORIDAD"));
        i.setIdEstado(rs.getLong("ID_ESTADO"));
        long idUsuario = rs.getLong("ID_USUARIO");
        i.setIdUsuario(rs.wasNull() ? null : idUsuario);
        i.setFechaCierre(rs.getDate("FECHA_CIERRE") == null ? null : rs.getDate("FECHA_CIERRE").toLocalDate());
        i.setHoraCierre(rs.getString("HORA_CIERRE"));
        return i;
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> cursor(Map<String, Object> result) {
        return (List<T>) result.get("P_RESULTADO");
    }
}
