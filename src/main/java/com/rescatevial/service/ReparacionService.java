
package com.rescatevial.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rescatevial.domain.Reparacion;
import com.rescatevial.repository.ReparacionRepository;

/**
 * Servicio para administrar las reparaciones
 * del sistema RESCATE VIAL CR.
 */
@Service
public class ReparacionService {

    private final ReparacionRepository reparacionRepository;

    // INYECCION DE DEPENDENCIAS
    public ReparacionService(ReparacionRepository reparacionRepository) {
        this.reparacionRepository = reparacionRepository;
    }

    // OBTENER TODAS LAS REPARACIONES
    @Transactional(readOnly = true)
    public List<Reparacion> getReparaciones() {
        return reparacionRepository.findAll();
    }

    // OBTENER UNA REPARACION POR ID
    @Transactional(readOnly = true)
    public Optional<Reparacion> getReparacionPorId(Long id) {
        return reparacionRepository.findById(id);
    }

    // INSERTAR O MODIFICAR UNA REPARACION
    @Transactional
    public void save(Reparacion reparacion) {

        // SI NO TIENE ID, ES UNA REPARACION NUEVA
        if (reparacion.getId() == null) {
            reparacionRepository.insertUsingProcedure(reparacion);
            return;
        }

        // VERIFICAR QUE LA REPARACION EXISTA
        reparacionRepository.findById(reparacion.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Reparacion no encontrada con ID: "
                                + reparacion.getId()));

        // MODIFICAR MEDIANTE PROCEDIMIENTO ALMACENADO
        reparacionRepository.updateUsingProcedure(reparacion);
    }

    // ELIMINAR REPARACION MEDIANTE CAMBIO DE ESTADO
    @Transactional
    public void delete(Long idReparacion) {

        // VERIFICAR QUE LA REPARACION EXISTA
        reparacionRepository.findById(idReparacion)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Reparacion con ID "
                                + idReparacion
                                + " no existe."));

        // EJECUTAR PROCEDIMIENTO ALMACENADO
        reparacionRepository.deleteById(idReparacion);
    }
}
