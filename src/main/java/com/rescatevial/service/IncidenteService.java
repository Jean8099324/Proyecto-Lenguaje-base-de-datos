package com.rescatevial.service;

import com.rescatevial.domain.Incidente;
import com.rescatevial.domain.ContactoIncidente;
import com.rescatevial.domain.FacturacionIncidente;
import com.rescatevial.domain.HistorialServicioIncidente;
import com.rescatevial.domain.VehiculoDetalleIncidente;
import com.rescatevial.domain.OpcionIncidente;
import com.rescatevial.domain.VehiculoIncidenteForm;
import com.rescatevial.repository.IncidenteRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncidenteService {
    private final IncidenteRepository repository;
    private final EstadoService estadoService;
    private final VehiculoService vehiculoService;
    private final ClienteService clienteService;

    public IncidenteService(IncidenteRepository repository, EstadoService estadoService, VehiculoService vehiculoService, ClienteService clienteService) {
        this.repository = repository;
        this.estadoService = estadoService;
        this.vehiculoService = vehiculoService;
        this.clienteService = clienteService;
    }

    @Transactional(readOnly = true)
    public List<Incidente> listar() {
        List<Incidente> incidentes = repository.findAll();
        Map<Long, String> vehiculos = labels(repository.findVehicles());
        Map<Long, String> provincias = labels(repository.findProvinces());
        Map<Long, String> cantones = labels(repository.findCantons());
        Map<Long, String> distritos = labels(repository.findDistricts());
        Map<Long, String> tipos = labels(repository.findIncidentTypes());
        Map<Long, String> prioridades = labels(repository.findPriorities());
        Map<Long, String> estados = estadoService.listar().stream().collect(Collectors.toMap(
                estado -> estado.getId(), estado -> estado.getNombreEstado(), (first, ignored) -> first));
        incidentes.forEach(i -> {
            i.setVehiculoEtiqueta(vehiculos.getOrDefault(i.getIdVehiculo(), "Vehículo " + i.getIdVehiculo()));
            String ubicacion = provincias.getOrDefault(i.getIdProvincia(), "Provincia " + i.getIdProvincia())
                    + ", " + cantones.getOrDefault(i.getIdCanton(), "Cantón " + i.getIdCanton())
                    + ", " + distritos.getOrDefault(i.getIdDistrito(), "Distrito " + i.getIdDistrito());
            if (i.getOtrasSenas() != null && !i.getOtrasSenas().isBlank()) {
                ubicacion += " — " + i.getOtrasSenas();
            }
            i.setUbicacionEtiqueta(ubicacion);
            i.setTipoEtiqueta(tipos.getOrDefault(i.getIdTipoIncidente(), "Tipo " + i.getIdTipoIncidente()));
            i.setPrioridadEtiqueta(prioridades.getOrDefault(i.getIdPrioridad(), "Prioridad " + i.getIdPrioridad()));
            i.setEstadoEtiqueta(estados.getOrDefault(i.getIdEstado(), "Estado " + i.getIdEstado()));
        });
        return incidentes;
    }
    @Transactional(readOnly = true) public Optional<Incidente> buscar(Long id) { return repository.findById(id); }
    @Transactional(readOnly = true)
    public List<Incidente> listarPorCliente(Long usuarioId) {
        var tipos = labels(repository.findIncidentTypes());
        var estados = estadoService.listar().stream().collect(Collectors.toMap(
                e -> e.getId(), e -> e.getNombreEstado(), (a, b) -> a));
        return repository.findAll().stream().filter(i -> usuarioId.equals(i.getIdUsuario()))
                .peek(i -> {
                    i.setTipoEtiqueta(tipos.getOrDefault(i.getIdTipoIncidente(), "Sin tipo"));
                    i.setEstadoEtiqueta(estados.getOrDefault(i.getIdEstado(), "Sin estado"));
                }).toList();
    }
    @Transactional(readOnly = true)
    public Optional<Incidente> buscarDetalle(Long id) {
        return repository.findById(id).map(i -> {
            Map<Long, String> vehiculos = labels(repository.findVehicles());
            Map<Long, String> provincias = labels(repository.findProvinces());
            Map<Long, String> cantones = labels(repository.findCantons());
            Map<Long, String> distritos = labels(repository.findDistricts());
            Map<Long, String> tipos = labels(repository.findIncidentTypes());
            Map<Long, String> prioridades = labels(repository.findPriorities());
            Map<Long, String> estados = estadoService.listar().stream().collect(Collectors.toMap(
                    estado -> estado.getId(), estado -> estado.getNombreEstado(), (first, ignored) -> first));
            i.setVehiculoEtiqueta(vehiculos.getOrDefault(i.getIdVehiculo(), "Vehículo " + i.getIdVehiculo()));
            String ubicacion = provincias.getOrDefault(i.getIdProvincia(), "Provincia " + i.getIdProvincia())
                    + ", " + cantones.getOrDefault(i.getIdCanton(), "Cantón " + i.getIdCanton())
                    + ", " + distritos.getOrDefault(i.getIdDistrito(), "Distrito " + i.getIdDistrito());
            if (i.getOtrasSenas() != null && !i.getOtrasSenas().isBlank()) ubicacion += " — " + i.getOtrasSenas();
            i.setUbicacionEtiqueta(ubicacion);
            i.setTipoEtiqueta(tipos.getOrDefault(i.getIdTipoIncidente(), "Tipo " + i.getIdTipoIncidente()));
            i.setPrioridadEtiqueta(prioridades.getOrDefault(i.getIdPrioridad(), "Prioridad " + i.getIdPrioridad()));
            i.setEstadoEtiqueta(estados.getOrDefault(i.getIdEstado(), "Estado " + i.getIdEstado()));
            return i;
        });
    }
    @Transactional public void guardar(Incidente incidente) {
        if (incidente.getId() != null) {
        }
        // ID_USUARIO representa el cliente opcional vinculado al incidente.
        incidente.setIdUsuario(incidente.getIdCliente());
        if (repository.existsActiveIncidentForVehicle(incidente.getIdVehiculo(), incidente.getId())) {
            throw new IllegalStateException("El vehículo seleccionado ya tiene un incidente en proceso.");
        }
        if (incidente.getId() == null) repository.insert(incidente); else repository.update(incidente);
    }
    @Transactional
    public void crearContactoDesdeIncidente(Incidente incidente) {
        if (!repository.findContactsByIncident(incidente.getId()).isEmpty())
            throw new IllegalArgumentException("El incidente ya tiene un contacto. Recargue el formulario antes de editarlo.");
        Incidente datos = new Incidente();
        datos.setId(incidente.getId());
        datos.setNombreReportante(incidente.getNombreContactoIncidente());
        datos.setPrimerApellidoReportante(incidente.getPrimerApellidoContactoIncidente());
        datos.setSegundoApellidoReportante(incidente.getSegundoApellidoContactoIncidente());
        datos.setTelefonoReportante(incidente.getTelefonoContactoIncidente());
        repository.insertReporter(datos);
    }
    @Transactional public void desactivar(Long id) { repository.deactivate(id); }
    @Transactional(readOnly = true) public List<ContactoIncidente> contactos(Long id) { return repository.findContactsByIncident(id); }
    @Transactional(readOnly = true) public Optional<VehiculoDetalleIncidente> vehiculoDetalle(Long id) { return repository.findVehicleDetail(id); }
    @Transactional(readOnly = true) public List<HistorialServicioIncidente> historialServicios(Long id) { return repository.findServiceHistory(id); }
    @Transactional(readOnly = true) public List<FacturacionIncidente> facturacion(Long id) { return repository.findBilling(id); }
    @Transactional(readOnly = true) public List<OpcionIncidente> vehiculos() { return repository.findVehicles(); }
    @Transactional(readOnly = true)
    public Optional<com.rescatevial.domain.Vehiculo> vehiculoParaEdicion(Long incidenteId) {
        return repository.findById(incidenteId).flatMap(incidente -> vehiculoService.buscar(incidente.getIdVehiculo()));
    }

    @Transactional
    public void actualizarVehiculoDesdeIncidente(Long incidenteId, com.rescatevial.domain.Vehiculo formulario) {
        Incidente incidente = repository.findById(incidenteId)
                .orElseThrow(() -> new IllegalArgumentException("El incidente no existe."));
        if (!java.util.Objects.equals(incidente.getIdVehiculo(), formulario.getId()))
            throw new IllegalArgumentException("El vehículo indicado no corresponde al incidente.");
        var actual = vehiculoService.buscar(formulario.getId())
                .orElseThrow(() -> new IllegalArgumentException("El vehículo no existe."));
        // La placa identifica el vehículo y no se modifica desde un incidente.
        formulario.setPlaca(actual.getPlaca());
        formulario.setIdUsuario(actual.getIdUsuario());
        formulario.setIdEstado(actual.getIdEstado());
        vehiculoService.guardar(formulario);
    }
    @Transactional(readOnly = true) public List<OpcionIncidente> vehiculos(Long incidentId) { return repository.findVehicles(incidentId); }
    @Transactional(readOnly = true) public List<OpcionIncidente> tipos() { return repository.findIncidentTypes(); }
    @Transactional(readOnly = true) public List<OpcionIncidente> prioridades() { return repository.findPriorities(); }
    @Transactional(readOnly = true) public List<OpcionIncidente> provincias() { return repository.findProvinces(); }
    @Transactional(readOnly = true) public List<OpcionIncidente> cantones() { return repository.findCantons(); }
    @Transactional(readOnly = true) public List<OpcionIncidente> distritos() { return repository.findDistricts(); }
    @Transactional(readOnly = true) public List<OpcionIncidente> marcasVehiculo() { return repository.findBrands(); }
    @Transactional(readOnly = true) public List<OpcionIncidente> modelosVehiculo() { return repository.findModels(); }
    @Transactional(readOnly = true) public List<OpcionIncidente> aniosVehiculo() { return repository.findYears(); }

    @Transactional
    public OpcionIncidente registrarVehiculo(VehiculoIncidenteForm vehicle) {
        if (vehicle.getIdUsuario() == null)
            throw new IllegalArgumentException("No se identificó al usuario que registra el vehículo.");
        clienteService.buscar(vehicle.getIdUsuario())
                .filter(u -> clienteService.estadoActivoId().equals(u.getEstado()))
                .orElseThrow(() -> new IllegalArgumentException("Seleccione un cliente activo como propietario."));
        Optional<OpcionIncidente> existing = repository.findVehicleByPlate(vehicle.getPlaca());
        if (existing.isPresent()) {
            if (!Long.valueOf(1).equals(existing.get().getEstadoId())) {
                throw new IllegalArgumentException("La placa ya existe, pero el vehículo está inactivo. Consulte a un administrador.");
            }
            var propietario = repository.findVehicleDetail(existing.get().getId())
                    .orElseThrow(() -> new IllegalStateException("No se pudo comprobar el propietario del veh?culo."));
            if (!java.util.Objects.equals(propietario.getIdCliente(), vehicle.getIdUsuario()))
                throw new IllegalArgumentException("La placa ya pertenece a otro cliente. Seleccione el veh?culo existente o corrija su propietario desde el detalle.");
            return existing.get();
        }
        repository.insertVehicle(vehicle);
        return repository.findVehicleByPlate(vehicle.getPlaca())
                .orElseThrow(() -> new IllegalStateException("No se pudo recuperar el vehículo recién registrado."));
    }

    private static Map<Long, String> labels(List<OpcionIncidente> options) {
        return options.stream().collect(Collectors.toMap(
                option -> option.getId(), option -> option.getEtiqueta(), (first, ignored) -> first));
    }
}
