package com.rescatevial.service;

import com.rescatevial.domain.Incidente;
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
        Map<Long, String> ubicaciones = labels(repository.findLocations());
        Map<Long, String> tipos = labels(repository.findIncidentTypes());
        Map<Long, String> prioridades = labels(repository.findPriorities());
        Map<Long, String> estados = estadoService.listar().stream().collect(Collectors.toMap(
                estado -> estado.getId(), estado -> estado.getNombreEstado(), (first, ignored) -> first));
        incidentes.forEach(i -> {
            i.setVehiculoEtiqueta(vehiculos.getOrDefault(i.getIdVehiculo(), "Vehículo " + i.getIdVehiculo()));
            i.setUbicacionEtiqueta(ubicaciones.getOrDefault(i.getIdUbicacion(), "Ubicación " + i.getIdUbicacion()));
            i.setTipoEtiqueta(tipos.getOrDefault(i.getIdTipoIncidente(), "Tipo " + i.getIdTipoIncidente()));
            i.setPrioridadEtiqueta(prioridades.getOrDefault(i.getIdPrioridad(), "Prioridad " + i.getIdPrioridad()));
            i.setEstadoEtiqueta(estados.getOrDefault(i.getIdEstado(), "Estado " + i.getIdEstado()));
        });
        return incidentes;
    }
    @Transactional(readOnly = true) public Optional<Incidente> buscar(Long id) { return repository.findById(id); }
    @Transactional public void guardar(Incidente incidente) {
        if (incidente.getId() == null) repository.insert(incidente); else repository.update(incidente);
    }
    @Transactional public void desactivar(Long id) { repository.deactivate(id); }
    @Transactional(readOnly = true) public List<OpcionIncidente> vehiculos() { return repository.findVehicles(); }
    @Transactional(readOnly = true) public List<OpcionIncidente> tipos() { return repository.findIncidentTypes(); }
    @Transactional(readOnly = true) public List<OpcionIncidente> prioridades() { return repository.findPriorities(); }
    @Transactional(readOnly = true) public List<OpcionIncidente> ubicaciones() { return repository.findLocations(); }
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
