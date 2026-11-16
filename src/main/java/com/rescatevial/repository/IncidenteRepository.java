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
        List<OpcionIncidente> vehicles = IncidenteRepository.<OpcionIncidente>cursor(
                call.execute(new MapSqlParameterSource())).stream()
                .filter(vehicle -> Long.valueOf(1).equals(vehicle.getEstadoId())).toList();
        String sql = """
                SELECT DISTINCT i.ID_VEHICULO
                  FROM FIDE_INCIDENTES_TB i
                  JOIN FIDE_ESTADOS_TB e ON e.ID_ESTADO = i.ID_ESTADO
                 WHERE UPPER(TRIM(e.NOMBRE_ESTADO)) IN (
                     'REPORTADO', 'ASIGNADO', 'UNIDAD EN CAMINO', 'EN ATENCION', 'TRASLADO'
                 )
                   AND (? IS NULL OR i.ID_INCIDENTE <> ?)
                """;
        java.util.Set<Long> busyVehicleIds = new java.util.HashSet<>(jdbc.query(
                sql, (rs, row) -> rs.getLong("ID_VEHICULO"), incidentId, incidentId));
        return vehicles.stream().filter(vehicle -> !busyVehicleIds.contains(vehicle.getId())
                || (incidentId != null && existsVehicleForIncident(vehicle.getId(), incidentId))).toList();
    }

    private boolean existsVehicleForIncident(Long vehicleId, Long incidentId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM FIDE_INCIDENTES_TB WHERE ID_VEHICULO = ? AND ID_INCIDENTE = ?",
                Integer.class, vehicleId, incidentId);
        return count != null && count > 0;
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

    public List<OpcionIncidente> findProvinces() {
        return options("FIDE_OBTENER_PROVINCIAS_SP", "ID_PROVINCIA", "NOMBRE_PROVINCIA", null, "ID_ESTADO");
    }

    public List<OpcionIncidente> findCantons() {
        return options("FIDE_OBTENER_CANTONES_SP", "ID_CANTON", "NOMBRE_CANTON", "ID_PROVINCIA", "ID_ESTADO");
    }

    public List<OpcionIncidente> findDistricts() {
        return options("FIDE_OBTENER_DISTRITOS_SP", "ID_DISTRITO", "NOMBRE_DISTRITO", "ID_CANTON", "ID_ESTADO");
    }

    private void insertLocation(Incidente incidente) {
        locationCall("FIDE_INSERTAR_UBICACION_INCIDENTES_SP").execute(locationParameters(incidente));
        Integer locations = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM FIDE_UBICACION_INCIDENTES_TB
                WHERE ID_INCIDENTE = ?
                  AND ID_PROVINCIA = ?
                  AND ID_CANTON = ?
                  AND ID_DISTRITO = ?
                """, Integer.class, incidente.getId(), incidente.getIdProvincia(),
                incidente.getIdCanton(), incidente.getIdDistrito());
        if (locations == null || locations == 0) {
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
        String sql = """
                SELECT v.ID_USUARIO AS ID_CLIENTE, v.PLACA, v.CODIGO_CHASIS, m.NOMBRE_MARCA, mo.NOMBRE_MODELO, a.ANIO,
                       u.NOMBRE AS NOMBRE_CLIENTE, u.PRIMER_APELLIDO AS PRIMER_APELLIDO_CLIENTE,
                       u.SEGUNDO_APELLIDO AS SEGUNDO_APELLIDO_CLIENTE,
                       (SELECT LISTAGG(TRIM(
                                   NVL(p.NOMBRE_PROVINCIA, '') ||
                                   CASE WHEN p.NOMBRE_PROVINCIA IS NOT NULL THEN ', ' ELSE '' END ||
                                   NVL(c.NOMBRE_CANTON, '') ||
                                   CASE WHEN c.NOMBRE_CANTON IS NOT NULL THEN ', ' ELSE '' END ||
                                   NVL(d.NOMBRE_DISTRITO, '') ||
                                   CASE WHEN dir.OTRAS_SENAS IS NOT NULL THEN ' - ' || dir.OTRAS_SENAS ELSE '' END
                               ), ' | ') WITHIN GROUP (
                                   ORDER BY p.NOMBRE_PROVINCIA, c.NOMBRE_CANTON, d.NOMBRE_DISTRITO
                               )
                          FROM FIDE_DIRECCIONES_TB dir
                          JOIN FIDE_ESTADOS_TB ed ON ed.ID_ESTADO = dir.ID_ESTADO
                          LEFT JOIN FIDE_PROVINCIAS_TB p ON p.ID_PROVINCIA = dir.ID_PROVINCIA
                          LEFT JOIN FIDE_CANTONES_TB c ON c.ID_CANTON = dir.ID_CANTON
                          LEFT JOIN FIDE_DISTRITOS_TB d ON d.ID_DISTRITO = dir.ID_DISTRITO
                         WHERE dir.ID_USUARIO = u.ID_USUARIO
                           AND UPPER(TRIM(ed.NOMBRE_ESTADO)) = 'ACTIVO') AS DIRECCION_CLIENTE
                  FROM FIDE_VEHICULOS_TB v
                  JOIN FIDE_MARCAS_TB m ON m.ID_MARCA = v.ID_MARCA
                  JOIN FIDE_MODELOS_TB mo ON mo.ID_MARCA = v.ID_MARCA AND mo.ID_MODELO = v.ID_MODELO
                  JOIN FIDE_ANIOS_TB a ON a.ID_MODELO = v.ID_MODELO AND a.ID_ANIO = v.ID_ANIO
                  JOIN FIDE_USUARIOS_TB u ON u.ID_USUARIO = v.ID_USUARIO
                 WHERE v.ID_VEHICULO = ?
                """;
        return jdbc.query(sql, (rs, row) -> {
            VehiculoDetalleIncidente vehicle = new VehiculoDetalleIncidente();
            vehicle.setIdCliente(rs.getLong("ID_CLIENTE"));
            vehicle.setNombreCliente(fullName(
                    rs.getString("NOMBRE_CLIENTE"),
                    rs.getString("PRIMER_APELLIDO_CLIENTE"),
                    rs.getString("SEGUNDO_APELLIDO_CLIENTE")));
            vehicle.setDireccionCliente(rs.getString("DIRECCION_CLIENTE"));
            vehicle.setPlaca(rs.getString("PLACA"));
            vehicle.setCodigoChasis(rs.getString("CODIGO_CHASIS"));
            vehicle.setMarca(rs.getString("NOMBRE_MARCA"));
            vehicle.setModelo(rs.getString("NOMBRE_MODELO"));
            vehicle.setAnio(rs.getString("ANIO"));
            return vehicle;
        }, vehicleId).stream().findFirst();
    }

    public List<HistorialServicioIncidente> findServiceHistory(Long incidentId) {
        List<HistorialServicioIncidente> history = new ArrayList<>();
        String unitSql = """
                SELECT h.OBSERVACIONES, h.FECHA_SALIDA_UNIDAD, h.HORA_SALIDA_UNIDAD,
                       h.FECHA_REGRESO_UNIDAD, h.HORA_REGRESO_UNIDAD,
                       u.NOMBRE_UNIDAD, u.PLACA_UNIDAD, tu.NOMBRE_TIPO_UNIDAD,
                       us.NOMBRE, us.PRIMER_APELLIDO, us.SEGUNDO_APELLIDO, e.NOMBRE_ESTADO
                  FROM FIDE_HISTORIALES_UNIDADES_TB h
                  JOIN FIDE_UNIDADES_TB u ON u.ID_UNIDAD = h.ID_UNIDAD
                  JOIN FIDE_TIPOS_UNIDADES_TB tu ON tu.ID_TIPO_UNIDAD = u.ID_TIPO_UNIDAD
                  JOIN FIDE_USUARIOS_TB us ON us.ID_USUARIO = h.ID_RESPONSABLE
                  JOIN FIDE_ESTADOS_TB e ON e.ID_ESTADO = h.ID_ESTADO
                 WHERE h.ID_INCIDENTE = ?
                """;
        history.addAll(jdbc.query(unitSql, (rs, row) -> {
            HistorialServicioIncidente item = new HistorialServicioIncidente();
            item.setEtapa("Asistencia vial");
            item.setRecurso(rs.getString("NOMBRE_TIPO_UNIDAD") + " · " + rs.getString("NOMBRE_UNIDAD")
                    + (rs.getString("PLACA_UNIDAD") == null ? "" : " · " + rs.getString("PLACA_UNIDAD")));
            item.setResponsable(fullName(rs.getString("NOMBRE"), rs.getString("PRIMER_APELLIDO"), rs.getString("SEGUNDO_APELLIDO")));
            item.setEstado(rs.getString("NOMBRE_ESTADO"));
            item.setDetalle(rs.getString("OBSERVACIONES"));
            item.setFechaInicio(toLocalDate(rs.getDate("FECHA_SALIDA_UNIDAD")));
            item.setHoraInicio(rs.getString("HORA_SALIDA_UNIDAD"));
            item.setFechaFin(toLocalDate(rs.getDate("FECHA_REGRESO_UNIDAD")));
            item.setHoraFin(rs.getString("HORA_REGRESO_UNIDAD"));
            return item;
        }, incidentId));
        String repairSql = """
                SELECT r.ID_REPARACION, r.DESCRIPCION_REPARACION, r.FECHA_INGRESO_TALLER,
                       r.FECHA_SALIDA_TALLER, us.NOMBRE, us.PRIMER_APELLIDO,
                       us.SEGUNDO_APELLIDO, e.NOMBRE_ESTADO
                  FROM FIDE_REPARACIONES_TB r
                  JOIN FIDE_USUARIOS_TB us ON us.ID_USUARIO = r.ID_MECANICO
                  JOIN FIDE_ESTADOS_TB e ON e.ID_ESTADO = r.ID_ESTADO
                 WHERE r.ID_INCIDENTE = ?
                """;
        history.addAll(jdbc.query(repairSql, (rs, row) -> {
            HistorialServicioIncidente item = new HistorialServicioIncidente();
            item.setEtapa("Taller");
            item.setRecurso("Reparación #" + rs.getLong("ID_REPARACION"));
            item.setResponsable(fullName(rs.getString("NOMBRE"), rs.getString("PRIMER_APELLIDO"), rs.getString("SEGUNDO_APELLIDO")));
            item.setEstado(rs.getString("NOMBRE_ESTADO"));
            item.setDetalle(rs.getString("DESCRIPCION_REPARACION"));
            item.setFechaInicio(toLocalDate(rs.getDate("FECHA_INGRESO_TALLER")));
            item.setFechaFin(toLocalDate(rs.getDate("FECHA_SALIDA_TALLER")));
            return item;
        }, incidentId));
        history.sort(Comparator.comparing((HistorialServicioIncidente item) -> item.getFechaInicio(),
                Comparator.nullsLast(Comparator.naturalOrder())));
        return history;
    }

    public List<FacturacionIncidente> findBilling(Long incidentId) {
        String sql = """
                SELECT v.ID_VENTA, v.FECHA_VENTA, v.TOTAL_VENTA,
                       f.ID_FACTURA, f.FECHA_FACTURA, f.HORA_FACTURA, f.SUBTOTAL,
                       f.TOTAL_DEDUCCIONES, f.TOTAL_IMPUESTOS, f.TOTAL_FACTURA,
                       NVL(p.TOTAL_PAGADO, 0) AS TOTAL_PAGADO,
                       COALESCE(ef.NOMBRE_ESTADO, ev.NOMBRE_ESTADO) AS NOMBRE_ESTADO
                  FROM FIDE_VENTAS_TB v
                  JOIN FIDE_ESTADOS_TB ev ON ev.ID_ESTADO = v.ID_ESTADO
                  LEFT JOIN FIDE_FACTURAS_TB f ON f.ID_VENTA = v.ID_VENTA
                  LEFT JOIN FIDE_ESTADOS_TB ef ON ef.ID_ESTADO = f.ID_ESTADO
                  LEFT JOIN (SELECT ID_FACTURA, SUM(MONTO_APORTADO) TOTAL_PAGADO
                               FROM FIDE_PAGOS_TB GROUP BY ID_FACTURA) p
                    ON p.ID_FACTURA = f.ID_FACTURA
                 WHERE v.ID_INCIDENTE = ?
                 ORDER BY v.FECHA_VENTA, v.ID_VENTA
                """;
        return jdbc.query(sql, (rs, row) -> {
            FacturacionIncidente billing = new FacturacionIncidente();
            billing.setIdVenta(rs.getLong("ID_VENTA"));
            billing.setFechaVenta(toLocalDate(rs.getDate("FECHA_VENTA")));
            billing.setTotalVenta(rs.getBigDecimal("TOTAL_VENTA"));
            long invoiceId = rs.getLong("ID_FACTURA");
            billing.setIdFactura(rs.wasNull() ? null : invoiceId);
            billing.setFechaFactura(toLocalDate(rs.getDate("FECHA_FACTURA")));
            billing.setHoraFactura(rs.getString("HORA_FACTURA"));
            billing.setSubtotal(rs.getBigDecimal("SUBTOTAL"));
            billing.setDeducciones(rs.getBigDecimal("TOTAL_DEDUCCIONES"));
            billing.setImpuestos(rs.getBigDecimal("TOTAL_IMPUESTOS"));
            billing.setTotalFactura(rs.getBigDecimal("TOTAL_FACTURA"));
            billing.setTotalPagado(rs.getBigDecimal("TOTAL_PAGADO"));
            billing.setEstado(rs.getString("NOMBRE_ESTADO"));
            return billing;
        }, incidentId);
    }

    private static java.time.LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toLocalDate();
    }

    private static String fullName(String... values) {
        return java.util.Arrays.stream(values).filter(value -> value != null && !value.isBlank())
                .collect(java.util.stream.Collectors.joining(" "));
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
