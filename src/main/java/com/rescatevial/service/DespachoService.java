package com.rescatevial.service;

import com.rescatevial.domain.UnidadDisponibleDespacho;
import com.rescatevial.repository.DespachoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DespachoService {
    private final DespachoRepository repository;

    public DespachoService(DespachoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public DespachoRepository.PaginaIncidentes pendientes(int pagina, int tamanio) {
        if (pagina < 1 || tamanio < 1) {
            throw new IllegalArgumentException("La página y su tamaño deben ser mayores que cero.");
        }
        return repository.findPendingIncidents(pagina, tamanio);
    }

    @Transactional(readOnly = true)
    public List<UnidadDisponibleDespacho> unidadesDisponibles() {
        return repository.findAvailableUnits();
    }

    @Transactional
    public void asignar(Long incidentId, Long unitId, Long responsibleId, String observations) {
        if (incidentId == null || unitId == null || responsibleId == null) {
            throw new IllegalArgumentException("Seleccione un incidente y una unidad válida.");
        }
        repository.assign(incidentId, unitId, responsibleId, observations);
    }
}
