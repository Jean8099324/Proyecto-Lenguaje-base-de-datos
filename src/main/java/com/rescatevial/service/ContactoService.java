package com.rescatevial.service;

import com.rescatevial.domain.Contacto;
import com.rescatevial.domain.ContactoIncidente;
import com.rescatevial.domain.Estado;
import com.rescatevial.domain.IncidenteOpcion;
import com.rescatevial.repository.ContactoRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactoService {
    private final ContactoRepository repository;
    private final EstadoService estados;

    public ContactoService(ContactoRepository repository, EstadoService estados) {
        this.repository = repository;
        this.estados = estados;
    }

    private Long idEstado(String nombre) {
        return estados.listar().stream()
                .filter(e -> nombre.equalsIgnoreCase(e.getNombreEstado()))
                .map(Estado::getId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No existe el estado " + nombre + " en Oracle."));
    }

    public Long estadoActivo() { return idEstado("ACTIVO"); }
    private Long estadoInactivo() { return idEstado("INACTIVO"); }

    @Transactional(readOnly = true)
    public List<Contacto> listar() {
        Map<Long, Estado> nombres = estados.listar().stream()
                .collect(Collectors.toMap(Estado::getId, Function.identity(), (a, b) -> a));
        List<Contacto> contactos = repository.listar();
        contactos.forEach(c -> {
            Estado estado = nombres.get(c.getIdEstado());
            c.setNombreEstado(estado == null ? "SIN ESTADO" : estado.getNombreEstado());
        });
        return contactos;
    }

    @Transactional(readOnly = true)
    public Optional<Contacto> buscar(Long id) {
        return repository.buscar(id);
    }

    private static String limpiar(String dato) {
        return dato == null || dato.isBlank() ? null : dato.trim();
    }

    private static boolean coincide(Contacto uno, Contacto otro) {
        return Objects.equals(uno.getNombreContacto(), otro.getNombreContacto())
                && Objects.equals(uno.getPrimerApellido(), otro.getPrimerApellido())
                && Objects.equals(uno.getSegundoApellido(), otro.getSegundoApellido())
                && Objects.equals(uno.getTelefonoContacto(), otro.getTelefonoContacto())
                && Objects.equals(uno.getCorreoContacto(), otro.getCorreoContacto())
                && Objects.equals(uno.getIdEstado(), otro.getIdEstado());
    }

    @Transactional
    public void guardar(Contacto c) {
        c.setNombreContacto(limpiar(c.getNombreContacto()));
        c.setPrimerApellido(limpiar(c.getPrimerApellido()));
        c.setSegundoApellido(limpiar(c.getSegundoApellido()));
        c.setTelefonoContacto(limpiar(c.getTelefonoContacto()));
        c.setCorreoContacto(limpiar(c.getCorreoContacto()));
        if (c.getNombreContacto() == null || c.getPrimerApellido() == null) {
            throw new IllegalArgumentException("Nombre y primer apellido son obligatorios.");
        }
        if (c.getTelefonoContacto() == null && c.getCorreoContacto() == null) {
            throw new IllegalArgumentException("Indica un teléfono o un correo para localizar al contacto.");
        }
        if (c.getId() == null) {
            c.setIdEstado(estadoActivo());
            long antes = repository.listar().stream().filter(reg -> coincide(reg, c)).count();
            repository.insertar(c);
            long despues = repository.listar().stream().filter(reg -> coincide(reg, c)).count();
            if (despues <= antes) {
                throw new IllegalStateException("Oracle no confirmó el registro del contacto.");
            }
        } else {
            Contacto anterior = repository.buscar(c.getId())
                    .orElseThrow(() -> new IllegalArgumentException("El contacto no existe."));
            // El estado se gestiona solo con los botones Desactivar y Reactivar.
            c.setIdEstado(anterior.getIdEstado());
            repository.modificar(c);
            Contacto actualizado = repository.buscar(c.getId())
                    .orElseThrow(() -> new IllegalStateException("Oracle no devolvió el contacto actualizado."));
            if (!coincide(c, actualizado)) {
                throw new IllegalStateException("Oracle no confirmó los cambios del contacto.");
            }
        }
    }

    @Transactional
    public void desactivar(Long id) {
        Contacto contacto = repository.buscar(id)
                .orElseThrow(() -> new IllegalArgumentException("Contacto no encontrado."));
        if (estadoInactivo().equals(contacto.getIdEstado())) {
            throw new IllegalArgumentException("El contacto ya está inactivo.");
        }
        repository.desactivar(id);
        Contacto actualizado = repository.buscar(id).orElseThrow();
        if (!estadoInactivo().equals(actualizado.getIdEstado())) {
            throw new IllegalStateException("Oracle no confirmó la desactivación.");
        }
    }

    @Transactional
    public void reactivar(Long id) {
        Contacto contacto = repository.buscar(id)
                .orElseThrow(() -> new IllegalArgumentException("Contacto no encontrado."));
        if (estadoActivo().equals(contacto.getIdEstado())) {
            throw new IllegalArgumentException("El contacto ya está activo.");
        }
        contacto.setIdEstado(estadoActivo());
        repository.modificar(contacto);
        Contacto actualizado = repository.buscar(id).orElseThrow();
        if (!estadoActivo().equals(actualizado.getIdEstado())) {
            throw new IllegalStateException("Oracle no confirmó la reactivación.");
        }
    }

    @Transactional(readOnly = true)
    public List<IncidenteOpcion> incidentes() {
        return repository.incidentes();
    }

    @Transactional(readOnly = true)
    public List<ContactoIncidente> relaciones(Long idContacto) {
        Map<Long, Estado> estadosPorId = estados.listar().stream()
                .collect(Collectors.toMap(Estado::getId, Function.identity(), (a, b) -> a));
        return repository.relaciones().stream()
                .filter(r -> r.getIdContacto().equals(idContacto))
                .peek(r -> {
                    Estado e = estadosPorId.get(r.getIdEstado());
                    r.setNombreEstado(e == null ? "SIN ESTADO" : e.getNombreEstado());
                }).toList();
    }

    private boolean existeIncidente(Long idIncidente) {
        return incidentes().stream().anyMatch(i -> idIncidente.equals(i.getId()));
    }

    @Transactional
    public void asociarIncidente(Long idContacto, Long idIncidente) {
        Contacto contacto = repository.buscar(idContacto)
                .orElseThrow(() -> new IllegalArgumentException("Contacto no encontrado."));
        if (!estadoActivo().equals(contacto.getIdEstado())) {
            throw new IllegalArgumentException("Reactiva el contacto antes de asociarlo.");
        }
        if (!existeIncidente(idIncidente)) {
            throw new IllegalArgumentException("Selecciona un incidente existente.");
        }
        Optional<ContactoIncidente> relacionActual = repository.relaciones().stream()
                .filter(r -> idContacto.equals(r.getIdContacto()) && idIncidente.equals(r.getIdIncidente()))
                .findFirst();
        if (relacionActual.isPresent()) {
            if (estadoActivo().equals(relacionActual.get().getIdEstado())) {
                throw new IllegalArgumentException("El contacto ya está asociado a este incidente.");
            }
            repository.modificarRelacion(idIncidente, idContacto, estadoActivo());
        } else {
            repository.asociar(idIncidente, idContacto, estadoActivo());
        }
        boolean activo = repository.relaciones().stream()
                .anyMatch(r -> idContacto.equals(r.getIdContacto()) && idIncidente.equals(r.getIdIncidente())
                        && estadoActivo().equals(r.getIdEstado()));
        if (!activo) throw new IllegalStateException("Oracle no confirmó la asociación al incidente.");
    }

    @Transactional
    public void quitarIncidente(Long idContacto, Long idIncidente) {
        boolean asociado = repository.relaciones().stream()
                .anyMatch(r -> idContacto.equals(r.getIdContacto()) && idIncidente.equals(r.getIdIncidente())
                        && estadoActivo().equals(r.getIdEstado()));
        if (!asociado) throw new IllegalArgumentException("No existe una relación activa con ese incidente.");
        repository.desactivarRelacion(idIncidente, idContacto);
        boolean sigueActivo = repository.relaciones().stream()
                .anyMatch(r -> idContacto.equals(r.getIdContacto()) && idIncidente.equals(r.getIdIncidente())
                        && estadoActivo().equals(r.getIdEstado()));
        if (sigueActivo) throw new IllegalStateException("Oracle no confirmó la desasociación.");
    }
}
