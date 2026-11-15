
package com.rescatevial.service;

import java.sql.SQLException;
import java.util.List;

import org.springframework.stereotype.Service;

import com.rescatevial.domain.Repuesto;
import com.rescatevial.repository.RepuestoRepository;

/*
 * PANTALLA 22: REPUESTOS
 * DESCRIPCION: SERVICIO ENCARGADO DE GESTIONAR
 * LAS OPERACIONES CRUD DE REPUESTOS.
 *
 * LAS OPERACIONES SE EJECUTAN EN ORACLE
 * MEDIANTE EL REPOSITORIO DE REPUESTOS.
 */

@Service
public class RepuestoService {

    private final RepuestoRepository repuestoRepository;

    public RepuestoService(RepuestoRepository repuestoRepository) {
        this.repuestoRepository = repuestoRepository;
    }

    // OBTENER TODOS LOS REPUESTOS
    public List<Repuesto> listar() throws SQLException {
        return repuestoRepository.listar();
    }

    // OBTENER UN REPUESTO POR SU ID
    public Repuesto buscarPorId(Long idRepuesto) throws SQLException {
        return repuestoRepository.buscarPorId(idRepuesto);
    }

    // INSERTAR UN NUEVO REPUESTO
    public void insertar(Repuesto repuesto) throws SQLException {
        validar(repuesto);
        repuestoRepository.insertar(repuesto);
    }

    // MODIFICAR UN REPUESTO EXISTENTE
    public void modificar(Repuesto repuesto) throws SQLException {

        if (repuesto == null || repuesto.getIdRepuesto() == null) {
            throw new IllegalArgumentException(
                    "Debe indicar el ID del repuesto.");
        }

        validar(repuesto);
        repuestoRepository.modificar(repuesto);
    }

    // ELIMINAR UN REPUESTO
    public void eliminar(Long idRepuesto) throws SQLException {

        if (idRepuesto == null) {
            throw new IllegalArgumentException(
                    "Debe indicar el ID del repuesto.");
        }

        repuestoRepository.eliminar(idRepuesto);
    }

    // VALIDAR LOS DATOS DEL REPUESTO
    private void validar(Repuesto repuesto) {

        if (repuesto == null) {
            throw new IllegalArgumentException(
                    "Los datos del repuesto son obligatorios.");
        }

        if (repuesto.getNombreRepuesto() == null
                || repuesto.getNombreRepuesto().isBlank()) {
            throw new IllegalArgumentException(
                    "Debe ingresar el nombre del repuesto.");
        }

        if (repuesto.getNombreRepuesto().length() > 100) {
            throw new IllegalArgumentException(
                    "El nombre no puede superar 100 caracteres.");
        }

        if (repuesto.getDescripcionRepuesto() != null
                && repuesto.getDescripcionRepuesto().length() > 500) {
            throw new IllegalArgumentException(
                    "La descripcion no puede superar 500 caracteres.");
        }

        if (repuesto.getPrecioUnitario() == null
                || repuesto.getPrecioUnitario().signum() < 0) {
            throw new IllegalArgumentException(
                    "El precio unitario debe ser mayor o igual a cero.");
        }

        if (repuesto.getIdEstado() == null) {
            throw new IllegalArgumentException(
                    "Debe seleccionar un estado.");
        }
    }
}
