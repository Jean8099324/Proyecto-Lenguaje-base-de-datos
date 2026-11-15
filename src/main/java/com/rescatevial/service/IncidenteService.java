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

    public IncidenteService(IncidenteRepository repository, EstadoService estadoService) {
        this.repository = repository;
        this.estadoService = estadoService;
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
        if (incidente.getId() == null) repository.insert(incidente); else repository.update(incidente);
    }
    @Transactional public void desactivar(Long id) { repository.deactivate(id); }
    @Transactional(readOnly = true) public List<ContactoIncidente> contactos(Long id) { return repository.findContactsByIncident(id); }
    @Transactional(readOnly = true) public Optional<VehiculoDetalleIncidente> vehiculoDetalle(Long id) { return repository.findVehicleDetail(id); }
    @Transactional(readOnly = true) public List<HistorialServicioIncidente> historialServicios(Long id) { return repository.findServiceHistory(id); }
    @Transactional(readOnly = true) public List<FacturacionIncidente> facturacion(Long id) { return repository.findBilling(id); }
    @Transactional(readOnly = true) public List<OpcionIncidente> vehiculos() { return repository.findVehicles(); }
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
        Optional<OpcionIncidente> existing = repository.findVehicleByPlate(vehicle.getPlaca());
        if (existing.isPresent()) {
            if (!Long.valueOf(1).equals(existing.get().getEstadoId())) {
                throw new IllegalArgumentException("La placa ya existe, pero el vehículo está inactivo. Consulte a un administrador.");
            }
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
