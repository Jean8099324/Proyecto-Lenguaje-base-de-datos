package com.rescatevial.repository;

import com.rescatevial.domain.Incidente;
import com.rescatevial.domain.ContactoIncidente;
import com.rescatevial.domain.FacturacionIncidente;
import com.rescatevial.domain.HistorialServicioIncidente;
import com.rescatevial.domain.VehiculoDetalleIncidente;
import com.rescatevial.domain.OpcionIncidente;
import com.rescatevial.domain.VehiculoIncidenteForm;
import java.sql.Date;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Comparator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Repository
public class IncidenteRepository {
    private static final Logger log = LoggerFactory.getLogger(IncidenteRepository.class);
    private final JdbcTemplate jdbc;

    public IncidenteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Incidente> findAll() {
        var call = new SimpleJdbcCall(jdbc).withCatalogName("FIDE_PROYECTO_FINAL_RESCATE_VIAL_CR_PCK")
                .withProcedureName("FIDE_OBTENER_INCIDENTES_SP").withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR,
                        (rs, row) -> mapIncidenteListado(rs)));
        return medir("FIDE_OBTENER_INCIDENTES_SP", () -> cursor(call.execute(new MapSqlParameterSource())));
    }

    private static Incidente mapIncidenteListado(java.sql.ResultSet rs) throws java.sql.SQLException {
        Incidente incident = new Incidente();
        incident.setId(rs.getLong("ID_INCIDENTE"));
        incident.setPlacaVehiculo(rs.getString("PLACA"));
        incident.setVehiculoEtiqueta(rs.getString("PLACA"));
        incident.setFechaReporte(rs.getDate("FECHA_REPORTE") == null
                ? null : rs.getDate("FECHA_REPORTE").toLocalDate());
        incident.setHoraReporte(rs.getString("HORA_REPORTE"));
        incident.setDetalles(rs.getString("DETALLES_INCIDENTE"));
        incident.setTipoEtiqueta(rs.getString("NOMBRE_TIPO_INCIDENTE"));
        incident.setPrioridadEtiqueta(rs.getString("NOMBRE_PRIORIDAD"));
        incident.setEstadoEtiqueta(rs.getString("NOMBRE_ESTADO"));
        String province = rs.getString("NOMBRE_PROVINCIA");
        String canton = rs.getString("NOMBRE_CANTON");
        String district = rs.getString("NOMBRE_DISTRITO");
        String location = String.join(", ", java.util.stream.Stream.of(province, canton, district)
                .filter(value -> value != null && !value.isBlank()).toList());
        String otherSigns = rs.getString("OTRAS_SENAS");
        if (otherSigns != null && !otherSigns.isBlank()) {
            location += (location.isBlank() ? "" : " — ") + otherSigns;
        }
        incident.setOtrasSenas(otherSigns);
        incident.setUbicacionEtiqueta(location);
        return incident;
    }

    public List<Incidente> findByUserId(Long userId) {
        var call = new SimpleJdbcCall(jdbc).withCatalogName("FIDE_PROYECTO_FINAL_RESCATE_VIAL_CR_PCK")
                .withProcedureName("FIDE_OBTENER_INCIDENTES_POR_USUARIO_SP").withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlParameter("P_ID_USUARIO", Types.NUMERIC),
                        new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR, (rs, row) -> {
                            Incidente incident = new Incidente();
                            incident.setId(rs.getLong("ID_INCIDENTE"));
                            incident.setIdUsuario(userId);
                            incident.setIdCliente(userId);
                            incident.setIdVehiculo(rs.getLong("ID_VEHICULO"));
                            incident.setPlacaVehiculo(rs.getString("PLACA"));
                            incident.setVehiculoEtiqueta(rs.getString("PLACA"));
                            incident.setFechaReporte(rs.getDate("FECHA_REPORTE") == null ? null
                                    : rs.getDate("FECHA_REPORTE").toLocalDate());
                            incident.setHoraReporte(rs.getString("HORA_REPORTE"));
                            incident.setDetalles(rs.getString("DETALLES_INCIDENTE"));
                            incident.setIdTipoIncidente(rs.getLong("ID_TIPO_INCIDENTE"));
                            incident.setIdPrioridad(rs.getLong("ID_PRIORIDAD"));
                            incident.setIdEstado(rs.getLong("ID_ESTADO"));
                            incident.setFechaCierre(rs.getDate("FECHA_CIERRE") == null ? null
                                    : rs.getDate("FECHA_CIERRE").toLocalDate());
                            incident.setHoraCierre(rs.getString("HORA_CIERRE"));
                            return incident;
                        }));
        return cursor(call.execute(new MapSqlParameterSource("P_ID_USUARIO", userId)));
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

    public boolean existsActiveIncidentForVehicle(Long vehicleId, Long excludedIncidentId) {
        var activos = new EstadoRepository(jdbc).findAll().stream()
                .filter(e -> java.util.Set.of("REPORTADO", "ASIGNADO", "UNIDAD EN CAMINO", "EN ATENCION", "TRASLADO")
                        .contains(e.getNombreEstado().trim().toUpperCase(java.util.Locale.ROOT)))
                .map(e -> e.getId()).collect(java.util.stream.Collectors.toSet());
        return findAll().stream().anyMatch(i -> java.util.Objects.equals(vehicleId, i.getIdVehiculo())
                && !java.util.Objects.equals(excludedIncidentId, i.getId()) && activos.contains(i.getIdEstado()));
    }

    public void insert(Incidente incidente) {
        var call = new SimpleJdbcCall(jdbc).withCatalogName("FIDE_PROYECTO_FINAL_RESCATE_VIAL_CR_PCK")
                .withProcedureName("FIDE_INSERTAR_INCIDENTES_SP").withoutProcedureColumnMetaDataAccess()
                .declareParameters(incidentParameters(false));
        Map<String, Object> result = call.execute(parameters(incidente, false));
        incidente.setId(((Number) result.get("P_ID_INCIDENTE")).longValue());
        insertLocation(incidente);
        insertReporter(incidente);
    }

    public void update(Incidente incidente) {
        var call = new SimpleJdbcCall(jdbc).withCatalogName("FIDE_PROYECTO_FINAL_RESCATE_VIAL_CR_PCK")
                .withProcedureName("FIDE_MODIFICAR_INCIDENTES_SP").withoutProcedureColumnMetaDataAccess()
                .declareParameters(incidentParameters(true));
        call.execute(parameters(incidente, true));
        updateLocation(incidente);
    }

    public void deactivate(Long id) {
        var call = new SimpleJdbcCall(jdbc).withCatalogName("FIDE_PROYECTO_FINAL_RESCATE_VIAL_CR_PCK")
                .withProcedureName("FIDE_ELIMINAR_INCIDENTES_SP").withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlParameter("P_ID_INCIDENTE", Types.NUMERIC));
        call.execute(new MapSqlParameterSource("P_ID_INCIDENTE", id));
    }

    public List<OpcionIncidente> findVehicles() {
        return findVehicles(null);
    }

    public List<OpcionIncidente> findVehicles(Long incidentId) {
        var call = new SimpleJdbcCall(jdbc).withCatalogName("FIDE_PROYECTO_FINAL_RESCATE_VIAL_CR_PCK")
                .withProcedureName("FIDE_OBTENER_VEHICULOS_SP").withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR, (rs, row) -> {
                    OpcionIncidente option = new OpcionIncidente();
                    option.setId(rs.getLong("ID_VEHICULO"));
                    option.setEtiqueta(rs.getString("PLACA") == null ? "Sin placa" : rs.getString("PLACA"));
                    option.setEstadoId(rs.getLong("ID_ESTADO"));
                    return option;
                }));
        List<OpcionIncidente> vehicles = medir("FIDE_OBTENER_VEHICULOS_SP", () ->
                IncidenteRepository.<OpcionIncidente>cursor(call.execute(new MapSqlParameterSource()))).stream()
                .filter(vehicle -> Long.valueOf(1).equals(vehicle.getEstadoId())).toList();
        java.util.Set<String> activos = java.util.Set.of("REPORTADO", "ASIGNADO",
                "UNIDAD EN CAMINO", "EN ATENCION", "TRASLADO");
        java.util.Set<Long> busyVehicleIds = findAll().stream()
                .filter(incident -> incident.getEstadoEtiqueta() != null
                        && activos.contains(incident.getEstadoEtiqueta().trim().toUpperCase(java.util.Locale.ROOT)))
                .filter(incident -> !java.util.Objects.equals(incidentId, incident.getId()))
                .map(incidente -> incidente.getIdVehiculo())
                .collect(java.util.stream.Collectors.toSet());
        vehicles.forEach(vehicle -> vehicle.setIncidenteEnProceso(busyVehicleIds.contains(vehicle.getId())));
        return vehicles;
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
        return medir("FIDE_OBTENER_VEHICULOS_SP", () -> cursor(call.execute(new MapSqlParameterSource())));
    }

    public List<OpcionIncidente> findIncidentTypes() {
        return options("FIDE_OBTENER_TIPOS_INCIDENTES_SP", "ID_TIPO_INCIDENTE", "NOMBRE_TIPO_INCIDENTE");
    }

    public List<OpcionIncidente> findPriorities() {
        return options("FIDE_OBTENER_PRIORIDADES_SP", "ID_PRIORIDAD", "NOMBRE_PRIORIDAD");
    }

    public List<OpcionIncidente> findProvinces() {
        return options("FIDE_OBTENER_PROVINCIAS_SP", "ID_PROVINCIA", "NOMBRE_PROVINCIA", null, "ID_ESTADO");
    }

    public List<OpcionIncidente> findCantons() {
        return options("FIDE_OBTENER_CANTONES_SP", "ID_CANTON", "NOMBRE_CANTON", "ID_PROVINCIA", "ID_ESTADO");
    }

    public List<OpcionIncidente> findCantonsByProvince(Long provinceId) {
        return relatedOptions("FIDE_OBTENER_CANTONES_POR_PROVINCIA_SP", "P_ID_PROVINCIA", provinceId,
                "ID_CANTON", "NOMBRE_CANTON", "ID_PROVINCIA", "ID_ESTADO");
    }

    public List<OpcionIncidente> findDistricts() {
        return options("FIDE_OBTENER_DISTRITOS_SP", "ID_DISTRITO", "NOMBRE_DISTRITO", "ID_CANTON", "ID_ESTADO");
    }

    public List<OpcionIncidente> findDistrictsByCanton(Long cantonId) {
        return relatedOptions("FIDE_OBTENER_DISTRITOS_POR_CANTON_SP", "P_ID_CANTON", cantonId,
                "ID_DISTRITO", "NOMBRE_DISTRITO", "ID_CANTON", "ID_ESTADO");
    }

    private List<OpcionIncidente> relatedOptions(String procedure, String parameterName, Long parentId,
                                                  String idColumn, String labelColumn,
                                                  String relatedColumn, String statusColumn) {
        var call = new SimpleJdbcCall(jdbc).withCatalogName("FIDE_PROYECTO_FINAL_RESCATE_VIAL_CR_PCK")
                .withProcedureName(procedure).withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlParameter(parameterName, Types.NUMERIC),
                        new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR, (rs, row) -> {
                            OpcionIncidente option = new OpcionIncidente();
                            option.setId(rs.getLong(idColumn));
                            option.setEtiqueta(rs.getString(labelColumn));
                            option.setRelacionId(rs.getLong(relatedColumn));
                            option.setEstadoId(rs.getLong(statusColumn));
                            return option;
                        }));
        return medir(procedure, () -> IncidenteRepository.<OpcionIncidente>cursor(
                call.execute(new MapSqlParameterSource(parameterName, parentId))))
                .stream().filter(option -> Long.valueOf(1).equals(option.getEstadoId())).toList();
    }

    private void insertLocation(Incidente incidente) {
        locationCall("FIDE_INSERTAR_UBICACION_INCIDENTES_SP").execute(locationParameters(incidente));
        boolean stored = packageRows("FIDE_OBTENER_UBICACION_POR_ID_SP",
                new SqlParameter("P_ID_INCIDENTE", Types.NUMERIC),
                new MapSqlParameterSource("P_ID_INCIDENTE", incidente.getId()))
                .stream()
                .anyMatch(row -> java.util.Objects.equals(incidente.getIdProvincia(), number(row.get("ID_PROVINCIA")))
                        && java.util.Objects.equals(incidente.getIdCanton(), number(row.get("ID_CANTON")))
                        && java.util.Objects.equals(incidente.getIdDistrito(), number(row.get("ID_DISTRITO"))));
        if (!stored) {
            throw new IllegalStateException("Oracle no registró la ubicación del incidente.");
        }
    }

    private void updateLocation(Incidente incidente) {
        locationCall("FIDE_MODIFICAR_UBICACION_INCIDENTES_SP").execute(locationParameters(incidente));
    }

    private SimpleJdbcCall locationCall(String procedure) {
        return new SimpleJdbcCall(jdbc).withCatalogName("FIDE_PROYECTO_FINAL_RESCATE_VIAL_CR_PCK")
                .withProcedureName(procedure).withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlParameter("P_ID_INCIDENTE", Types.NUMERIC),
                        new SqlParameter("P_ID_PROVINCIA", Types.NUMERIC),
                        new SqlParameter("P_ID_CANTON", Types.NUMERIC),
                        new SqlParameter("P_ID_DISTRITO", Types.NUMERIC),
                        new SqlParameter("P_OTRAS_SENAS", Types.VARCHAR),
                        new SqlParameter("P_ID_ESTADO", Types.NUMERIC));
    }

    private static MapSqlParameterSource locationParameters(Incidente incidente) {
        return new MapSqlParameterSource()
                .addValue("P_ID_INCIDENTE", incidente.getId(), Types.NUMERIC)
                .addValue("P_ID_PROVINCIA", incidente.getIdProvincia(), Types.NUMERIC)
                .addValue("P_ID_CANTON", incidente.getIdCanton(), Types.NUMERIC)
                .addValue("P_ID_DISTRITO", incidente.getIdDistrito(), Types.NUMERIC)
                .addValue("P_OTRAS_SENAS", emptyToNull(incidente.getOtrasSenas()), Types.VARCHAR)
                .addValue("P_ID_ESTADO", 1L, Types.NUMERIC);
    }

    public void insertReporter(Incidente incidente) {
        var contactCall = new SimpleJdbcCall(jdbc).withCatalogName("FIDE_PROYECTO_FINAL_RESCATE_VIAL_CR_PCK")
                .withProcedureName("FIDE_INSERTAR_CONTACTOS_SP").withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlParameter("P_NOMBRE_CONTACTO", Types.VARCHAR),
                        new SqlParameter("P_PRIMER_APELLIDO", Types.VARCHAR),
                        new SqlParameter("P_SEGUNDO_APELLIDO", Types.VARCHAR),
                        new SqlParameter("P_TELEFONO_CONTACTO", Types.VARCHAR),
                        new SqlParameter("P_CORREO_CONTACTO", Types.VARCHAR),
                        new SqlParameter("P_ID_ESTADO", Types.NUMERIC),
                        new SqlOutParameter("P_ID_CONTACTO", Types.NUMERIC));
        Map<String, Object> result = contactCall.execute(new MapSqlParameterSource()
                .addValue("P_NOMBRE_CONTACTO", emptyToNull(incidente.getNombreReportante()), Types.VARCHAR)
                .addValue("P_PRIMER_APELLIDO", emptyToNull(incidente.getPrimerApellidoReportante()), Types.VARCHAR)
                .addValue("P_SEGUNDO_APELLIDO", emptyToNull(incidente.getSegundoApellidoReportante()), Types.VARCHAR)
                .addValue("P_TELEFONO_CONTACTO", emptyToNull(incidente.getTelefonoReportante()), Types.VARCHAR)
                .addValue("P_CORREO_CONTACTO", emptyToNull(incidente.getCorreoReportante()), Types.VARCHAR)
                .addValue("P_ID_ESTADO", 1L, Types.NUMERIC));
        if (!(result.get("P_ID_CONTACTO") instanceof Number))
            throw new IllegalStateException("Oracle no registró el contacto del incidente.");
        Long contactId = ((Number) result.get("P_ID_CONTACTO")).longValue();
        var relationCall = new SimpleJdbcCall(jdbc).withCatalogName("FIDE_PROYECTO_FINAL_RESCATE_VIAL_CR_PCK")
                .withProcedureName("FIDE_INSERTAR_INCIDENTES_CONTACTOS_SP").withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlParameter("P_ID_INCIDENTE", Types.NUMERIC),
                        new SqlParameter("P_ID_CONTACTO", Types.NUMERIC),
                        new SqlParameter("P_ID_ESTADO", Types.NUMERIC));
        relationCall.execute(new MapSqlParameterSource()
                .addValue("P_ID_INCIDENTE", incidente.getId(), Types.NUMERIC)
                .addValue("P_ID_CONTACTO", contactId, Types.NUMERIC)
                .addValue("P_ID_ESTADO", 1L, Types.NUMERIC));
        if (findContactsByIncident(incidente.getId()).stream().noneMatch(c -> contactId.equals(c.getId())))
            throw new IllegalStateException("Oracle no asoció el contacto al incidente.");
    }

    public List<ContactoIncidente> findContactsByIncident(Long incidentId) {
        var call = new SimpleJdbcCall(jdbc).withCatalogName("FIDE_PROYECTO_FINAL_RESCATE_VIAL_CR_PCK")
                .withProcedureName("FIDE_OBTENER_CONTACTOS_POR_INCIDENTE_SP").withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlParameter("P_ID_INCIDENTE", Types.NUMERIC),
                        new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR, (rs, row) -> {
                            ContactoIncidente contact = new ContactoIncidente();
                            contact.setId(rs.getLong("ID_CONTACTO"));
                            contact.setNombre(rs.getString("NOMBRE_CONTACTO"));
                            contact.setPrimerApellido(rs.getString("PRIMER_APELLIDO"));
                            contact.setSegundoApellido(rs.getString("SEGUNDO_APELLIDO"));
                            contact.setTelefono(rs.getString("TELEFONO_CONTACTO"));
                            contact.setCorreo(rs.getString("CORREO_CONTACTO"));
                            return contact;
                        }));
        return cursor(call.execute(new MapSqlParameterSource("P_ID_INCIDENTE", incidentId)));
    }

    public Optional<VehiculoDetalleIncidente> findVehicleDetail(Long vehicleId) {
        Map<String, Object> vehicle = packageRows("FIDE_OBTENER_VEHICULO_POR_ID_SP",
                new SqlParameter("P_ID_VEHICULO", Types.NUMERIC),
                new MapSqlParameterSource("P_ID_VEHICULO", vehicleId)).stream().findFirst().orElse(null);
        if (vehicle == null) {
            return Optional.empty();
        }
        Long clientId = number(vehicle.get("ID_USUARIO"));
        Map<String, Object> client = packageRows("FIDE_OBTENER_USUARIO_POR_ID_USUARIO_SP",
                new SqlParameter("P_ID_USUARIO", Types.NUMERIC),
                new MapSqlParameterSource("P_ID_USUARIO", clientId)).stream().findFirst().orElse(null);
        Map<Long, String> brands = names("FIDE_OBTENER_MARCAS_SP", "ID_MARCA", "NOMBRE_MARCA");
        Map<Long, String> models = names("FIDE_OBTENER_MODELOS_SP", "ID_MODELO", "NOMBRE_MODELO");
        Map<Long, String> years = names("FIDE_OBTENER_ANIOS_SP", "ID_ANIO", "ANIO");
        Map<Long, String> provinces = names("FIDE_OBTENER_PROVINCIAS_SP", "ID_PROVINCIA", "NOMBRE_PROVINCIA");
        Map<Long, String> cantons = names("FIDE_OBTENER_CANTONES_SP", "ID_CANTON", "NOMBRE_CANTON");
        Map<Long, String> districts = names("FIDE_OBTENER_DISTRITOS_SP", "ID_DISTRITO", "NOMBRE_DISTRITO");
        List<String> addresses = packageRows("FIDE_OBTENER_DIRECCIONES_SP").stream()
                .filter(row -> clientId.equals(number(row.get("ID_USUARIO"))))
                .filter(row -> Long.valueOf(1).equals(number(row.get("ID_ESTADO"))))
                .map(row -> fullAddress(row, provinces, cantons, districts))
                .toList();

        VehiculoDetalleIncidente result = new VehiculoDetalleIncidente();
        result.setIdCliente(clientId);
        result.setNombreCliente(client == null ? null : fullName(text(client.get("NOMBRE")),
                text(client.get("PRIMER_APELLIDO")), text(client.get("SEGUNDO_APELLIDO"))));
        result.setDireccionesCliente(addresses);
        result.setDireccionCliente(String.join(" | ", addresses));
        result.setPlaca(text(vehicle.get("PLACA")));
        result.setCodigoChasis(text(vehicle.get("CODIGO_CHASIS")));
        result.setMarca(brands.get(number(vehicle.get("ID_MARCA"))));
        result.setModelo(models.get(number(vehicle.get("ID_MODELO"))));
        result.setAnio(years.get(number(vehicle.get("ID_ANIO"))));
        return Optional.of(result);
    }

    public Optional<String> findVehicleOwnerName(Long vehicleId) {
        Map<String, Object> vehicle = packageRows("FIDE_OBTENER_VEHICULO_POR_ID_SP",
                new SqlParameter("P_ID_VEHICULO", Types.NUMERIC),
                new MapSqlParameterSource("P_ID_VEHICULO", vehicleId)).stream().findFirst().orElse(null);
        if (vehicle == null) return Optional.empty();
        Long clientId = number(vehicle.get("ID_USUARIO"));
        if (clientId == null) return Optional.empty();
        Map<String, Object> client = packageRows("FIDE_OBTENER_USUARIO_POR_ID_USUARIO_SP",
                new SqlParameter("P_ID_USUARIO", Types.NUMERIC),
                new MapSqlParameterSource("P_ID_USUARIO", clientId)).stream().findFirst().orElse(null);
        if (client == null) return Optional.empty();
        String name = fullName(text(client.get("NOMBRE")), text(client.get("PRIMER_APELLIDO")),
                text(client.get("SEGUNDO_APELLIDO")));
        return name.isBlank() ? Optional.empty() : Optional.of(name);
    }

    public List<HistorialServicioIncidente> findServiceHistory(Long incidentId) {
        List<HistorialServicioIncidente> history = new ArrayList<>();
        Map<Long, Map<String, Object>> units = byId("FIDE_OBTENER_UNIDADES_SP", "ID_UNIDAD");
        Map<Long, String> unitTypes = names("FIDE_OBTENER_TIPOS_UNIDADES_SP",
                "ID_TIPO_UNIDAD", "NOMBRE_TIPO_UNIDAD");
        Map<Long, Map<String, Object>> users = byId("FIDE_OBTENER_USUARIOS_SP", "ID_USUARIO");
        Map<Long, String> states = names("FIDE_OBTENER_ESTADOS_SP", "ID_ESTADO", "NOMBRE_ESTADO");
        history.addAll(packageRows("FIDE_OBTENER_HISTORIALES_UNIDADES_SP").stream()
                .filter(row -> incidentId.equals(number(row.get("ID_INCIDENTE"))))
                .map(row -> {
            HistorialServicioIncidente item = new HistorialServicioIncidente();
            item.setEtapa("Asistencia vial");
            Map<String, Object> unit = units.get(number(row.get("ID_UNIDAD")));
            Map<String, Object> user = users.get(number(row.get("ID_RESPONSABLE")));
            item.setRecurso(unitTypes.get(number(unit == null ? null : unit.get("ID_TIPO_UNIDAD")))
                    + " · " + text(unit == null ? null : unit.get("NOMBRE_UNIDAD"))
                    + (unit == null || unit.get("PLACA_UNIDAD") == null ? "" : " · " + unit.get("PLACA_UNIDAD")));
            item.setResponsable(user == null ? null : fullName(text(user.get("NOMBRE")),
                    text(user.get("PRIMER_APELLIDO")), text(user.get("SEGUNDO_APELLIDO"))));
            item.setEstado(states.get(number(row.get("ID_ESTADO"))));
            item.setDetalle(text(row.get("OBSERVACIONES")));
            item.setFechaInicio(toLocalDate(row.get("FECHA_SALIDA_UNIDAD")));
            item.setHoraInicio(text(row.get("HORA_SALIDA_UNIDAD")));
            item.setFechaFin(toLocalDate(row.get("FECHA_REGRESO_UNIDAD")));
            item.setHoraFin(text(row.get("HORA_REGRESO_UNIDAD")));
            return item;
        }).toList());
        history.addAll(packageRows("FIDE_OBTENER_REPARACIONES_SP").stream()
                .filter(row -> incidentId.equals(number(row.get("ID_INCIDENTE"))))
                .map(row -> {
            HistorialServicioIncidente item = new HistorialServicioIncidente();
            item.setEtapa("Taller");
            Map<String, Object> user = users.get(number(row.get("ID_MECANICO")));
            item.setRecurso("Reparación #" + number(row.get("ID_REPARACION")));
            item.setResponsable(user == null ? null : fullName(text(user.get("NOMBRE")),
                    text(user.get("PRIMER_APELLIDO")), text(user.get("SEGUNDO_APELLIDO"))));
            item.setEstado(states.get(number(row.get("ID_ESTADO"))));
            item.setDetalle(text(row.get("DESCRIPCION_REPARACION")));
            item.setFechaInicio(toLocalDate(row.get("FECHA_INGRESO_TALLER")));
            item.setFechaFin(toLocalDate(row.get("FECHA_SALIDA_TALLER")));
            return item;
        }).toList());
        history.sort(Comparator.comparing((HistorialServicioIncidente item) -> item.getFechaInicio(),
                Comparator.nullsLast(Comparator.naturalOrder())));
        return history;
    }

    public List<FacturacionIncidente> findBilling(Long incidentId) {
        Map<Long, Map<String, Object>> invoices = byId("FIDE_OBTENER_FACTURAS_SP", "ID_FACTURA");
        Map<Long, String> states = names("FIDE_OBTENER_ESTADOS_SP", "ID_ESTADO", "NOMBRE_ESTADO");
        Map<Long, java.math.BigDecimal> payments = new java.util.HashMap<>();
        packageRows("FIDE_OBTENER_PAGOS_SP").forEach(row -> payments.merge(number(row.get("ID_FACTURA")),
                decimal(row.get("MONTO_APORTADO")), (acumulado, monto) -> acumulado.add(monto)));
        return packageRows("FIDE_OBTENER_VENTAS_SP").stream()
                .filter(row -> incidentId.equals(number(row.get("ID_INCIDENTE"))))
                .sorted(Comparator.comparing(row -> toLocalDate(row.get("FECHA_VENTA")),
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(row -> {
            FacturacionIncidente billing = new FacturacionIncidente();
            billing.setIdVenta(number(row.get("ID_VENTA")));
            billing.setFechaVenta(toLocalDate(row.get("FECHA_VENTA")));
            billing.setTotalVenta(decimal(row.get("TOTAL_VENTA")));
            Map<String, Object> invoice = invoices.values().stream()
                    .filter(candidate -> number(row.get("ID_VENTA")).equals(number(candidate.get("ID_VENTA"))))
                    .findFirst().orElse(null);
            Long invoiceId = invoice == null ? null : number(invoice.get("ID_FACTURA"));
            billing.setIdFactura(invoiceId);
            billing.setFechaFactura(invoice == null ? null : toLocalDate(invoice.get("FECHA_FACTURA")));
            billing.setHoraFactura(invoice == null ? null : text(invoice.get("HORA_FACTURA")));
            billing.setSubtotal(invoice == null ? null : decimal(invoice.get("SUBTOTAL")));
            billing.setDeducciones(invoice == null ? null : decimal(invoice.get("TOTAL_DEDUCCIONES")));
            billing.setImpuestos(invoice == null ? null : decimal(invoice.get("TOTAL_IMPUESTOS")));
            billing.setTotalFactura(invoice == null ? null : decimal(invoice.get("TOTAL_FACTURA")));
            billing.setTotalPagado(invoiceId == null ? java.math.BigDecimal.ZERO : payments.getOrDefault(invoiceId,
                    java.math.BigDecimal.ZERO));
            billing.setEstado(states.get(invoice == null ? number(row.get("ID_ESTADO"))
                    : number(invoice.get("ID_ESTADO"))));
            return billing;
        }).toList();
    }

    private static java.time.LocalDate toLocalDate(Object value) {
        return value instanceof Date date ? date.toLocalDate()
                : value instanceof java.time.LocalDate localDate ? localDate : null;
    }

    private static String fullName(String... values) {
        return java.util.Arrays.stream(values).filter(value -> value != null && !value.isBlank())
                .collect(java.util.stream.Collectors.joining(" "));
    }

    private Map<Long, Map<String, Object>> byId(String procedure, String idColumn) {
        return packageRows(procedure).stream().collect(java.util.stream.Collectors.toMap(
                row -> number(row.get(idColumn)), row -> row, (first, second) -> first));
    }

    private Map<Long, String> names(String procedure, String idColumn, String nameColumn) {
        return packageRows(procedure).stream().collect(java.util.stream.Collectors.toMap(
                row -> number(row.get(idColumn)), row -> text(row.get(nameColumn)), (first, second) -> first));
    }

    private List<Map<String, Object>> packageRows(String procedure) {
        return medir(procedure, () -> OraclePackageSupport.rows(OraclePackageSupport.call(jdbc, procedure,
                OraclePackageSupport.cursor()).execute(new MapSqlParameterSource())));
    }

    private List<Map<String, Object>> packageRows(String procedure, SqlParameter parameter,
                                                  MapSqlParameterSource values) {
        return medir(procedure, () -> OraclePackageSupport.rows(OraclePackageSupport.call(jdbc, procedure,
                parameter, OraclePackageSupport.cursor()).execute(values)));
    }

    private <T> T medir(String procedure, java.util.function.Supplier<T> operation) {
        long started = System.nanoTime();
        try {
            return operation.get();
        } finally {
            log.info("INCIDENTE_DB_TIMING procedure={} elapsedMs={}", procedure,
                    (System.nanoTime() - started) / 1_000_000);
        }
    }

    private String fullAddress(Map<String, Object> row, Map<Long, String> provinces,
                               Map<Long, String> cantons, Map<Long, String> districts) {
        String base = String.join(", ",
                text(provinces.get(number(row.get("ID_PROVINCIA")))),
                text(cantons.get(number(row.get("ID_CANTON")))),
                text(districts.get(number(row.get("ID_DISTRITO")))));
        String notes = text(row.get("OTRAS_SENAS"));
        return notes == null || notes.isBlank() ? base : base + " - " + notes;
    }

    private static Long number(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private static String text(Object value) {
        return value == null ? null : value.toString();
    }

    private static java.math.BigDecimal decimal(Object value) {
        return value instanceof java.math.BigDecimal decimal ? decimal
                : value instanceof Number number ? java.math.BigDecimal.valueOf(number.doubleValue()) : null;
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
        List<OpcionIncidente> options = medir(procedure, () -> cursor(call.execute(new MapSqlParameterSource())));
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
                new SqlParameter("P_ID_TIPO_INCIDENTE", Types.NUMERIC),
                new SqlParameter("P_ID_PRIORIDAD", Types.NUMERIC),
                new SqlParameter("P_ID_ESTADO", Types.NUMERIC),
                new SqlParameter("P_ID_USUARIO", Types.NUMERIC),
                new SqlParameter("P_FECHA_CIERRE", Types.DATE),
                new SqlParameter("P_HORA_CIERRE", Types.VARCHAR),
                new SqlOutParameter("P_ID_INCIDENTE", Types.NUMERIC)
        };
    }

    private static MapSqlParameterSource parameters(Incidente i, boolean withId) {
        var p = new MapSqlParameterSource();
        if (withId) p.addValue("P_ID_INCIDENTE", i.getId(), Types.NUMERIC);
        return p.addValue("P_ID_VEHICULO", i.getIdVehiculo(), Types.NUMERIC)
                .addValue("P_FECHA_REPORTE", sqlDate(i.getFechaReporte()), Types.DATE)
                .addValue("P_HORA_REPORTE", i.getHoraReporte(), Types.VARCHAR)
                .addValue("P_DETALLES_INCIDENTE", i.getDetalles(), Types.VARCHAR)
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
        Number usuarioId = (Number) rs.getObject("ID_USUARIO");
        i.setIdUsuario(usuarioId == null ? null : usuarioId.longValue());
        i.setIdCliente(usuarioId == null ? null : usuarioId.longValue());
        i.setIdVehiculo(rs.getLong("ID_VEHICULO"));
        i.setFechaReporte(rs.getDate("FECHA_REPORTE") == null ? null : rs.getDate("FECHA_REPORTE").toLocalDate());
        i.setHoraReporte(rs.getString("HORA_REPORTE"));
        i.setDetalles(rs.getString("DETALLES_INCIDENTE"));
        i.setIdProvincia(rs.getLong("ID_PROVINCIA"));
        i.setIdCanton(rs.getLong("ID_CANTON"));
        i.setIdDistrito(rs.getLong("ID_DISTRITO"));
        i.setOtrasSenas(rs.getString("OTRAS_SENAS"));
        i.setIdTipoIncidente(rs.getLong("ID_TIPO_INCIDENTE"));
        i.setIdPrioridad(rs.getLong("ID_PRIORIDAD"));
        i.setIdEstado(rs.getLong("ID_ESTADO"));
        i.setFechaCierre(rs.getDate("FECHA_CIERRE") == null ? null : rs.getDate("FECHA_CIERRE").toLocalDate());
        i.setHoraCierre(rs.getString("HORA_CIERRE"));
        return i;
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> cursor(Map<String, Object> result) {
        return (List<T>) result.get("P_RESULTADO");
    }
}
