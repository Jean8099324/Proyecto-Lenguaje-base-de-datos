package com.rescatevial.service;

import com.rescatevial.domain.ConfiguracionRegistro;
import com.rescatevial.domain.ConfiguracionTipo;
import com.rescatevial.repository.ConfiguracionRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfiguracionService {
    private final ConfiguracionRepository repository;
    public ConfiguracionService(ConfiguracionRepository repository) { this.repository = repository; }

    public List<ConfiguracionRegistro> listar(ConfiguracionTipo tipo) {
        var datos = repository.listar(tipo);
        if (tipo.isTienePadre()) {
            var nombres = new HashMap<Long, String>();
            for (var padre : repository.listar(ConfiguracionTipo.desdeRuta(tipo.getPadre())))
                nombres.put(padre.getId(), padre.getNombre());
            for (var d : datos) d.setNombrePadre(nombres.getOrDefault(d.getIdPadre(), "No encontrado"));
        }
        return datos;
    }

    public ConfiguracionRegistro buscar(ConfiguracionTipo tipo, Long id) {
        return listar(tipo).stream().filter(x -> x.getId().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Registro no encontrado."));
    }

    public List<ConfiguracionRegistro> opcionesPadre(ConfiguracionTipo tipo) {
        if (!tipo.isTienePadre()) return List.of();
        return listar(ConfiguracionTipo.desdeRuta(tipo.getPadre()));
    }

    private void validar(ConfiguracionTipo tipo, ConfiguracionRegistro r, boolean nuevo) {
        if (r.getNombre() == null || r.getNombre().isBlank())
            throw new IllegalArgumentException("El nombre es obligatorio.");
        r.setNombre(r.getNombre().trim());
        if (r.getNombre().length() > 100)
            throw new IllegalArgumentException("El nombre no puede superar 100 caracteres.");
        if (nuevo && tipo.isIdManual() && (r.getId() == null || r.getId() <= 0))
            throw new IllegalArgumentException("Debes indicar un ID positivo para este catálogo.");
        if (tipo.isTienePadre()) {
            if (r.getIdPadre() == null) throw new IllegalArgumentException("Selecciona el registro superior.");
            var padreTipo = ConfiguracionTipo.desdeRuta(tipo.getPadre());
            var padre = buscar(padreTipo, r.getIdPadre());
            if (nuevo && !padre.isActivo())
                throw new IllegalArgumentException("No puedes asociar un registro nuevo a un padre inactivo.");
        }
        if (!nuevo && (r.getId() == null || r.getId() <= 0))
            throw new IllegalArgumentException("El ID del registro no es válido.");
    }

    @Transactional
    public void guardar(ConfiguracionTipo tipo, ConfiguracionRegistro entrada, boolean nuevo) {
        validar(tipo, entrada, nuevo);
        if (nuevo) {
            if (tipo.isIdManual() && repository.listar(tipo).stream()
                    .anyMatch(d -> d.getId().equals(entrada.getId())))
                throw new IllegalArgumentException("Ya existe un registro con ese ID.");
            entrada.setIdEstado(1L);
            repository.insertar(tipo, entrada);
            // Algunos SP del proyecto capturan errores con DBMS_OUTPUT sin RAISE.
            // Consultar después permite detectar la mayoría de inserciones no efectuadas.
            boolean presente = repository.listar(tipo).stream().anyMatch(d ->
                d.getNombre() != null && d.getNombre().equalsIgnoreCase(entrada.getNombre()) &&
                (!tipo.isIdManual() || d.getId().equals(entrada.getId())));
            if (!presente) throw new IllegalStateException("Oracle no confirmó la creación del registro.");
        } else {
            ConfiguracionRegistro anterior = buscar(tipo, entrada.getId());
            if (tipo.isEstadoSistema() && (entrada.getId() == 1L || entrada.getId() == 2L))
                throw new IllegalArgumentException("Los estados ACTIVO e INACTIVO son esenciales y no deben renombrarse.");
            entrada.setIdEstado(anterior.getIdEstado());
            repository.modificar(tipo, entrada);
            ConfiguracionRegistro comprobado = buscar(tipo, entrada.getId());
            if (!entrada.getNombre().equals(comprobado.getNombre()) ||
                (tipo.isTienePadre() && !entrada.getIdPadre().equals(comprobado.getIdPadre())))
                throw new IllegalStateException("Oracle no confirmó la modificación del registro.");
        }
    }

    @Transactional
    public void cambiarEstado(ConfiguracionTipo tipo, Long id, boolean reactivar) {
        if (tipo.isEstadoSistema())
            throw new IllegalArgumentException("Los estados del sistema no admiten desactivación.");
        var actual = buscar(tipo, id);
        if (reactivar) {
            actual.setIdEstado(1L);
            repository.modificar(tipo, actual);
        } else {
            repository.desactivar(tipo, id);
        }
        var comprobado = buscar(tipo, id);
        if (comprobado.isActivo() != reactivar)
            throw new IllegalStateException("Oracle no confirmó el cambio de estado.");
    }
}
