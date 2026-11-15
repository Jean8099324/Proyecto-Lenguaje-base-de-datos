
package com.rescatevial.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rescatevial.domain.DetalleReparacion;
import com.rescatevial.repository.DetalleReparacionRepository;

/**
 * PROYECTO RESCATE VIAL CR
 *
 * SERVICIO PARA ADMINISTRAR LOS DETALLES
 * DE REPARACIONES MEDIANTE PROCEDIMIENTOS
 * ALMACENADOS DE ORACLE.
 */
@Service
public class DetalleReparacionService {

        private final DetalleReparacionRepository detalleRepository;

        // INYECCION DE DEPENDENCIAS
        public DetalleReparacionService(
                        DetalleReparacionRepository detalleRepository) {

                this.detalleRepository = detalleRepository;
        }

        // CONSULTAR TODOS LOS DETALLES
        @Transactional(readOnly = true)
        public List<DetalleReparacion> getDetalles() {

                return detalleRepository.findAll();
        }

        // CONSULTAR DETALLE POR REPARACION Y REPUESTO
        @Transactional(readOnly = true)
        public Optional<DetalleReparacion> getDetallePorId(
                        Long idReparacion,
                        Long idRepuesto) {

                return detalleRepository.findById(
                                idReparacion,
                                idRepuesto);
        }

        // INSERTAR UN NUEVO DETALLE
        @Transactional
        public void insertar(DetalleReparacion detalle) {

                // EVITAR REGISTRAR EL MISMO REPUESTO
                // DOS VECES EN UNA REPARACION
                if (detalleRepository.findById(
                                detalle.getIdReparacion(),
                                detalle.getIdRepuesto()).isPresent()) {

                        throw new IllegalArgumentException(
                                        "El repuesto ya está registrado "
                                                        + "en esta reparación.");
                }

                // INSERTAR MEDIANTE PROCEDIMIENTO ORACLE
                detalleRepository.insertUsingProcedure(detalle);
        }

        // MODIFICAR UN DETALLE EXISTENTE
        @Transactional
        public void modificar(DetalleReparacion detalle) {

                // VERIFICAR QUE EL DETALLE EXISTA
                detalleRepository.findById(
                                detalle.getIdReparacion(),
                                detalle.getIdRepuesto())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "No se encontró el detalle "
                                                                + "de reparación indicado."));

                // MODIFICAR MEDIANTE PROCEDIMIENTO ORACLE
                detalleRepository.updateUsingProcedure(detalle);
        }

        // ELIMINAR UN DETALLE
        @Transactional
        public void eliminar(
                        Long idReparacion,
                        Long idRepuesto) {

                // VERIFICAR QUE EL DETALLE EXISTA
                detalleRepository.findById(
                                idReparacion,
                                idRepuesto)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "No existe el detalle "
                                                                + "de reparación indicado."));

                // ELIMINAR MEDIANTE PROCEDIMIENTO ORACLE
                detalleRepository.deleteById(
                                idReparacion,
                                idRepuesto);
        }
}
