package com.rescatevial.service;

import com.rescatevial.domain.Estado;
import com.rescatevial.domain.OpcionVehiculo;
import com.rescatevial.domain.Usuario;
import com.rescatevial.domain.Vehiculo;
import com.rescatevial.repository.VehiculoRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehiculoService {
    private final VehiculoRepository repository;
    private final UsuarioService usuarios;
    private final EstadoService estados;

    public VehiculoService(VehiculoRepository repository, UsuarioService usuarios, EstadoService estados) {
        this.repository = repository;
        this.usuarios = usuarios;
        this.estados = estados;
    }

    @Transactional(readOnly = true)
    public List<Vehiculo> listar() {
        var resultado = repository.listar();
        Map<Long, OpcionVehiculo> marcas = porId(repository.marcas());
        Map<Long, OpcionVehiculo> modelos = porId(repository.modelos());
        Map<Long, OpcionVehiculo> anios = porId(repository.anios());
        Map<Long, Usuario> propietarios = usuarios.getUsuarios(false).stream()
                .collect(Collectors.toMap(usuario -> usuario.getId(), usuario -> usuario, (a, b) -> a));
        Map<Long, Estado> estadosPorId = estados.listar().stream()
                .collect(Collectors.toMap(estado -> estado.getId(), estado -> estado, (a, b) -> a));
        for (Vehiculo v : resultado) {
            v.setNombreMarca(nombre(marcas.get(v.getIdMarca())));
            v.setNombreModelo(nombre(modelos.get(v.getIdModelo())));
            v.setAnioTexto(nombre(anios.get(v.getIdAnio())));
            Usuario usuario = propietarios.get(v.getIdUsuario());
            v.setNombrePropietario(usuario == null ? "Sin propietario" :
                    usuario.getNombre() + " " + usuario.getPrimerApellido());
            Estado estado = estadosPorId.get(v.getIdEstado());
            v.setNombreEstado(estado == null ? "SIN ESTADO" : estado.getNombreEstado());
        }
        return resultado;
    }

    @Transactional(readOnly = true)
    public Optional<Vehiculo> buscar(Long id) {
        return repository.buscar(id);
    }

    @Transactional(readOnly = true)
    public List<Vehiculo> listarPorCliente(Long usuarioId) {
        var marcas = porId(repository.marcas());
        var modelos = porId(repository.modelos());
        var anios = porId(repository.anios());
        return repository.listar().stream().filter(v -> usuarioId.equals(v.getIdUsuario()))
                .peek(v -> {
                    v.setNombreMarca(nombre(marcas.get(v.getIdMarca())));
                    v.setNombreModelo(nombre(modelos.get(v.getIdModelo())));
                    v.setAnioTexto(nombre(anios.get(v.getIdAnio())));
                }).toList();
    }

    @Transactional(readOnly = true)
    public List<OpcionVehiculo> marcas() { return repository.marcas(); }

    @Transactional(readOnly = true)
    public List<OpcionVehiculo> modelos() { return repository.modelos(); }

    @Transactional(readOnly = true)
    public List<OpcionVehiculo> anios() { return repository.anios(); }

    @Transactional(readOnly = true)
    public List<Usuario> propietarios() { return usuarios.getUsuarios(false); }

    private Long idEstado(String nombre) {
        return estados.listar().stream()
                .filter(e -> nombre.equalsIgnoreCase(e.getNombreEstado()))
                .map(estado -> estado.getId())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No existe el estado " + nombre + "."));
    }

    public Long estadoActivo() { return idEstado("ACTIVO"); }

    private static Map<Long, OpcionVehiculo> porId(List<OpcionVehiculo> opciones) {
        return opciones.stream().collect(Collectors.toMap(opcion -> opcion.getId(),
                opcion -> opcion, (a, b) -> a));
    }

    private static String nombre(OpcionVehiculo opcion) {
        return opcion == null ? "Sin dato" : opcion.getNombre();
    }

    private void validarRelaciones(Vehiculo v) {
        OpcionVehiculo marca = porId(marcas()).get(v.getIdMarca());
        OpcionVehiculo modelo = porId(modelos()).get(v.getIdModelo());
        OpcionVehiculo anio = porId(anios()).get(v.getIdAnio());
        if (marca == null || modelo == null || anio == null ||
                !v.getIdMarca().equals(modelo.getPadreId()) ||
                !v.getIdModelo().equals(anio.getPadreId()) ||
                !estadoActivo().equals(marca.getEstadoId()) ||
                !estadoActivo().equals(modelo.getEstadoId()) ||
                !estadoActivo().equals(anio.getEstadoId())) {
            throw new IllegalArgumentException("La marca, modelo o año no corresponden entre sí o están inactivos.");
        }
        if (v.getIdUsuario() != null) {
            boolean existePropietario = propietarios().stream()
                    .anyMatch(u -> java.util.Objects.equals(u.getId(), v.getIdUsuario()));
            if (!existePropietario) {
                throw new IllegalArgumentException("Selecciona un propietario válido.");
            }
        }
    }

    private static String normalizar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private static String placaNormalizada(String texto) {
        String placa = com.rescatevial.domain.VehiculoIncidenteForm.normalizarPlaca(texto);
        return placa == null || placa.isBlank() ? null : placa;
    }

    @Transactional(readOnly = true)
    public boolean placaRegistrada(String placa, Long excluirId) {
        String normalizada = placaNormalizada(placa);
        if (normalizada == null) return false;
        return repository.listar().stream().anyMatch(actual ->
                !java.util.Objects.equals(actual.getId(), excluirId)
                        && normalizada.equals(placaNormalizada(actual.getPlaca())));
    }

    @Transactional
    public void guardar(Vehiculo v) {
        v.setPlaca(placaNormalizada(v.getPlaca()));
        v.setCodigoChasis(normalizar(v.getCodigoChasis()));
        if (v.getPlaca() == null) {
            throw new IllegalArgumentException("La placa es obligatoria.");
        }
        validarRelaciones(v);
        var anteriores = repository.listar();
        boolean duplicado = anteriores.stream().anyMatch(actual ->
                (v.getId() == null || !v.getId().equals(actual.getId())) &&
                ((v.getPlaca() != null && v.getPlaca().equals(placaNormalizada(actual.getPlaca()))) ||
                 (v.getCodigoChasis() != null && v.getCodigoChasis().equals(normalizar(actual.getCodigoChasis())))));
        if (duplicado) throw new IllegalArgumentException("Ya existe un vehículo con esa placa o chasis.");
        if (v.getId() == null) {
            v.setIdEstado(estadoActivo());
            try {
                repository.insertar(v);
            } catch (org.springframework.dao.DuplicateKeyException ex) {
                throw new IllegalArgumentException("Ya existe un vehículo con esa placa o chasis.", ex);
            }
            // Algunos procedimientos Oracle capturan errores sin propagarlos.
            // Comprobar que el registro realmente exista antes de mostrar un exito.
            var idsAnteriores = anteriores.stream().map(Vehiculo::getId).collect(Collectors.toSet());
            boolean insertado = repository.listar().stream().anyMatch(actual ->
                    actual.getId() != null && !idsAnteriores.contains(actual.getId())
                    && java.util.Objects.equals(v.getPlaca(), placaNormalizada(actual.getPlaca()))
                    && java.util.Objects.equals(v.getCodigoChasis(), normalizar(actual.getCodigoChasis()))
                    && java.util.Objects.equals(v.getIdUsuario(), actual.getIdUsuario())
                    && java.util.Objects.equals(v.getIdMarca(), actual.getIdMarca())
                    && java.util.Objects.equals(v.getIdModelo(), actual.getIdModelo())
                    && java.util.Objects.equals(v.getIdAnio(), actual.getIdAnio()));
            if (!insertado) throw new IllegalStateException("Oracle no confirmó la inserción del vehículo.");
        } else {
            Vehiculo existente = repository.buscar(v.getId())
                    .orElseThrow(() -> new IllegalArgumentException("El vehículo no existe."));
            v.setIdEstado(existente.getIdEstado());
            try {
                repository.modificar(v);
            } catch (org.springframework.dao.DuplicateKeyException ex) {
                throw new IllegalArgumentException("Ya existe un vehículo con esa placa o chasis.", ex);
            }
            Vehiculo actualizado = repository.buscar(v.getId())
                    .orElseThrow(() -> new IllegalStateException("No se encontró el vehículo luego de modificarlo."));
            if (!v.getIdMarca().equals(actualizado.getIdMarca()) ||
                    !v.getIdModelo().equals(actualizado.getIdModelo()) ||
                    !v.getIdAnio().equals(actualizado.getIdAnio()) ||
                    !v.getIdUsuario().equals(actualizado.getIdUsuario()) ||
                    !java.util.Objects.equals(v.getPlaca(), actualizado.getPlaca()) ||
                    !java.util.Objects.equals(v.getCodigoChasis(), actualizado.getCodigoChasis())) {
                throw new IllegalStateException("Oracle no confirmó la modificación del vehículo.");
            }
        }
    }

    @Transactional
    public void actualizarPlaca(Long id, String placa) {
        Vehiculo vehiculo = buscar(id)
                .orElseThrow(() -> new IllegalArgumentException("El vehículo no existe."));
        String nuevaPlaca = placaNormalizada(placa);
        if (nuevaPlaca != null && nuevaPlaca.length() > 10)
            throw new IllegalArgumentException("La placa admite como máximo 10 caracteres.");
        if (nuevaPlaca == null && normalizar(vehiculo.getCodigoChasis()) == null)
            throw new IllegalArgumentException("Indica la placa o el código de chasis.");
        if (java.util.Objects.equals(nuevaPlaca, vehiculo.getPlaca())) return;
        if (nuevaPlaca != null && repository.listar().stream().anyMatch(actual ->
                !id.equals(actual.getId()) && nuevaPlaca.equals(placaNormalizada(actual.getPlaca()))))
            throw new IllegalArgumentException("Ya existe un vehículo con esa placa.");
        vehiculo.setPlaca(nuevaPlaca);
        repository.modificar(vehiculo);
        Vehiculo actualizado = buscar(id).orElseThrow();
        if (!java.util.Objects.equals(nuevaPlaca, actualizado.getPlaca()))
            throw new IllegalStateException("Oracle no confirmó la modificación de la placa.");
    }

    @Transactional
    public void desactivar(Long id) {
        Vehiculo v = buscar(id).orElseThrow(() -> new IllegalArgumentException("Vehículo no encontrado."));
        if (!estadoActivo().equals(v.getIdEstado())) throw new IllegalArgumentException("Ya está inactivo.");
        repository.desactivar(id);
        if (estadoActivo().equals(buscar(id).orElseThrow().getIdEstado())) {
            throw new IllegalStateException("Oracle no confirmó la desactivación.");
        }
    }

    @Transactional
    public void reactivar(Long id) {
        Vehiculo v = buscar(id).orElseThrow(() -> new IllegalArgumentException("Vehículo no encontrado."));
        validarRelaciones(v);
        v.setIdEstado(estadoActivo());
        repository.modificar(v);
        if (!estadoActivo().equals(buscar(id).orElseThrow().getIdEstado())) {
            throw new IllegalStateException("Oracle no confirmó la reactivación.");
        }
    }
}
