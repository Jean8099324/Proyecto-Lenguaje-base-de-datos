package com.rescatevial.service;

import com.rescatevial.domain.DatoContacto;
import com.rescatevial.domain.OpcionUbicacion;
import com.rescatevial.domain.Usuario;
import com.rescatevial.repository.DatoContactoRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatoContactoService {
    private final DatoContactoRepository repo;
    private final UsuarioService usuarios;
    private final EstadoService estados;
    public DatoContactoService(DatoContactoRepository repo, UsuarioService usuarios, EstadoService estados) {
        this.repo = repo;
        this.usuarios = usuarios;
        this.estados = estados;
    }
    private Long estado(String nombre) {
        return estados.listar().stream().filter(e -> nombre.equalsIgnoreCase(e.getNombreEstado()))
            .map(estado -> estado.getId()).findFirst()
            .orElseThrow(() -> new IllegalStateException("No existe el estado " + nombre + " en Oracle."));
    }
    private Long activo() { return estado("ACTIVO"); }
    private Long inactivo() { return estado("INACTIVO"); }

    @Transactional(readOnly = true)
    public List<Usuario> usuarios() { return usuarios.getUsuarios(false); }
    @Transactional(readOnly = true)
    public Usuario usuario(Long id) {
        if (id == null) throw new IllegalArgumentException("Selecciona un usuario.");
        return usuarios.getUsuarioPorId(id)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
    }
    private static String limpiar(String valor) { return valor == null ? null : valor.trim(); }
    private static String requerido(String valor, String nombre) {
        valor = limpiar(valor);
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException(nombre + " es obligatorio.");
        return valor;
    }
    private void nombres(List<DatoContacto> datos) {
        Map<Long,String> etiquetas = estados.listar().stream()
            .collect(Collectors.toMap(estado -> estado.getId(), estado -> estado.getNombreEstado(), (a,b) -> a));
        datos.forEach(d -> d.setNombreEstado(etiquetas.getOrDefault(d.getIdEstado(), "SIN ESTADO")));
    }
    @Transactional(readOnly = true)
    public List<DatoContacto> telefonos(Long id) { usuario(id); var datos = repo.telefonos(id); nombres(datos); return datos; }
    @Transactional(readOnly = true)
    public List<DatoContacto> correos(Long id) { usuario(id); var datos = repo.correos(id); nombres(datos); return datos; }
    @Transactional(readOnly = true)
    public List<DatoContacto> direcciones(Long id) {
        usuario(id);
        var datos = repo.direcciones(id);
        nombres(datos);
        var p = repo.provincias().stream().collect(Collectors.toMap(opcion -> opcion.getId(), opcion -> opcion.getNombre(), (a,b)->a));
        var c = repo.cantones().stream().collect(Collectors.toMap(opcion -> opcion.getId(), opcion -> opcion.getNombre(), (a,b)->a));
        var d = repo.distritos().stream().collect(Collectors.toMap(opcion -> opcion.getId(), opcion -> opcion.getNombre(), (a,b)->a));
        datos.forEach(dato -> {
            dato.setNombreProvincia(p.getOrDefault(dato.getIdProvincia(), "Sin provincia"));
            dato.setNombreCanton(c.getOrDefault(dato.getIdCanton(), "Sin cantón"));
            dato.setNombreDistrito(d.getOrDefault(dato.getIdDistrito(), "Sin distrito"));
        });
        return datos;
    }
    public List<OpcionUbicacion> provincias() { Long estadoActivo = activo(); return repo.provincias().stream().filter(o -> estadoActivo.equals(o.getIdEstado())).toList(); }
    public List<OpcionUbicacion> cantones() { Long estadoActivo = activo(); return repo.cantones().stream().filter(o -> estadoActivo.equals(o.getIdEstado())).toList(); }
    public List<OpcionUbicacion> distritos() { Long estadoActivo = activo(); return repo.distritos().stream().filter(o -> estadoActivo.equals(o.getIdEstado())).toList(); }

    public DatoContacto buscar(String tipo, Long id, String valor, Long provincia, Long canton, Long distrito) {
        usuario(id);
        if ("telefono".equals(tipo) || "correo".equals(tipo)) {
            var todos = "telefono".equals(tipo) ? repo.telefonos(id) : repo.correos(id);
            return todos.stream().filter(d -> Objects.equals(d.getValor(), valor)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El dato de contacto no existe."));
        }
        if ("direccion".equals(tipo)) {
            return repo.direcciones(id).stream()
                .filter(d -> Objects.equals(d.getIdProvincia(), provincia)
                          && Objects.equals(d.getIdCanton(), canton)
                          && Objects.equals(d.getIdDistrito(), distrito))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("La dirección no existe."));
        }
        throw new IllegalArgumentException("Tipo de dato no válido.");
    }
    private void validarDireccion(DatoContacto dato) {
        if (dato.getIdProvincia() == null || dato.getIdCanton() == null || dato.getIdDistrito() == null)
            throw new IllegalArgumentException("Selecciona provincia, cantón y distrito.");
        var provincia = repo.provincias().stream().filter(o -> o.getId().equals(dato.getIdProvincia())).findFirst();
        var canton = repo.cantones().stream().filter(o -> o.getId().equals(dato.getIdCanton())).findFirst();
        var distrito = repo.distritos().stream().filter(o -> o.getId().equals(dato.getIdDistrito())).findFirst();
        if (provincia.isEmpty() || canton.isEmpty() || distrito.isEmpty()
            || !Objects.equals(canton.get().getIdPadre(), dato.getIdProvincia())
            || !Objects.equals(distrito.get().getIdPadre(), dato.getIdCanton())
            || !activo().equals(provincia.get().getIdEstado())
            || !activo().equals(canton.get().getIdEstado())
            || !activo().equals(distrito.get().getIdEstado()))
            throw new IllegalArgumentException("La provincia, el cantón y el distrito no corresponden entre sí o están inactivos.");
        dato.setOtrasSenas(normalizarSenas(dato.getOtrasSenas()));
        if (dato.getOtrasSenas() != null && dato.getOtrasSenas().length() > 200)
            throw new IllegalArgumentException("Las otras señas deben tener 200 caracteres como máximo.");
    }
    private boolean existeValor(String tipo, Long id, String valor, Long estado) {
        var datos = "telefono".equals(tipo) ? repo.telefonos(id) : repo.correos(id);
        return datos.stream().anyMatch(d -> Objects.equals(limpiar(d.getValor()), limpiar(valor))
                && Objects.equals(d.getIdEstado(), estado));
    }
    private static String normalizarSenas(String valor) {
        String limpio = limpiar(valor);
        return limpio == null || limpio.isEmpty() ? null : limpio;
    }

    private void insertarOReactivarDireccion(DatoContacto nueva) {
        var existente = repo.direcciones(nueva.getIdUsuario()).stream().filter(d ->
                Objects.equals(d.getIdProvincia(), nueva.getIdProvincia())
                && Objects.equals(d.getIdCanton(), nueva.getIdCanton())
                && Objects.equals(d.getIdDistrito(), nueva.getIdDistrito())).findFirst();
        nueva.setIdEstado(activo());
        if (existente.isPresent()) {
            if (activo().equals(existente.get().getIdEstado()))
                throw new IllegalArgumentException("El cliente ya tiene esa dirección registrada.");
            repo.modificarDireccion(nueva);
        } else {
            repo.insertarDireccion(nueva);
        }
        if (!existeDireccion(nueva, nueva.getIdEstado()))
            throw new IllegalStateException("Oracle no confirmó la dirección del cliente.");
    }

    private boolean existeDireccion(DatoContacto dato, Long estado) {
        return repo.direcciones(dato.getIdUsuario()).stream().anyMatch(d ->
            Objects.equals(d.getIdProvincia(), dato.getIdProvincia())
            && Objects.equals(d.getIdCanton(), dato.getIdCanton())
            && Objects.equals(d.getIdDistrito(), dato.getIdDistrito())
            && Objects.equals(d.getIdEstado(), estado)
            && Objects.equals(normalizarSenas(d.getOtrasSenas()), normalizarSenas(dato.getOtrasSenas())));
    }

    @Transactional
    public void guardar(String tipo, DatoContacto dato, boolean editar) {
        usuario(dato.getIdUsuario());
        if ("telefono".equals(tipo) || "correo".equals(tipo)) {
            String valor = requerido(dato.getValor(), "El dato");
            dato.setValor(valor);
            int maximo = "telefono".equals(tipo) ? 8 : 150;
            if (valor.length() > maximo) throw new IllegalArgumentException("El valor supera la longitud permitida.");
            if ("telefono".equals(tipo) && !valor.matches("\\d{8}"))
                throw new IllegalArgumentException("El teléfono debe contener exactamente 8 dígitos numéricos.");
            if ("correo".equals(tipo) && !valor.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
                throw new IllegalArgumentException("Indica un correo válido.");
            if (editar) {
                var anterior = buscar(tipo, dato.getIdUsuario(), dato.getValorAnterior(), null, null, null);
                if ("telefono".equals(tipo))
                    repo.modificarTelefono(dato.getIdUsuario(), anterior.getValor(), valor, anterior.getIdEstado());
                else repo.modificarCorreo(dato.getIdUsuario(), anterior.getValor(), valor, anterior.getIdEstado());
                if (!existeValor(tipo, dato.getIdUsuario(), valor, anterior.getIdEstado()))
                    throw new IllegalStateException("Oracle no confirmó la modificación.");
            } else {
                if (("telefono".equals(tipo) ? repo.telefonos(dato.getIdUsuario()) : repo.correos(dato.getIdUsuario()))
                    .stream().anyMatch(d -> Objects.equals(limpiar(d.getValor()), limpiar(valor))))
                    throw new IllegalArgumentException("Este " + ("telefono".equals(tipo) ? "teléfono" : "correo")
                        + " ya existe para este usuario. Reactívalo si está inactivo.");
                dato.setValor(valor); dato.setIdEstado(activo());
                if ("telefono".equals(tipo)) repo.insertarTelefono(dato); else repo.insertarCorreo(dato);
                if (!existeValor(tipo, dato.getIdUsuario(), valor, activo()))
                    throw new IllegalStateException("Oracle no confirmó el registro.");
            }
        } else if ("direccion".equals(tipo)) {
            validarDireccion(dato);
            if (editar) {
                var anterior = buscar(tipo, dato.getIdUsuario(), null, dato.getIdProvincia(), dato.getIdCanton(), dato.getIdDistrito());
                dato.setIdEstado(anterior.getIdEstado());
                repo.modificarDireccion(dato);
            } else {
                if (repo.direcciones(dato.getIdUsuario()).stream().anyMatch(d ->
                    Objects.equals(d.getIdProvincia(), dato.getIdProvincia())
                    && Objects.equals(d.getIdCanton(), dato.getIdCanton())
                    && Objects.equals(d.getIdDistrito(), dato.getIdDistrito())))
                    throw new IllegalArgumentException("Esta ubicación ya existe para el usuario. Edita o reactiva la dirección existente.");
                dato.setIdEstado(activo()); repo.insertarDireccion(dato);
            }
            if (!existeDireccion(dato, dato.getIdEstado()))
                throw new IllegalStateException("Oracle no confirmó los cambios de la dirección.");
        } else throw new IllegalArgumentException("Tipo de dato no válido.");
    }

    @Transactional
    public void actualizarDireccionDesdeIncidente(Long usuarioId, Long provinciaOriginal,
                                                   Long cantonOriginal, Long distritoOriginal,
                                                   DatoContacto nueva) {
        usuario(usuarioId);
        nueva.setIdUsuario(usuarioId);
        validarDireccion(nueva);
        DatoContacto anterior = buscar("direccion", usuarioId, null,
                provinciaOriginal, cantonOriginal, distritoOriginal);
        boolean mismaClave = Objects.equals(provinciaOriginal, nueva.getIdProvincia())
                && Objects.equals(cantonOriginal, nueva.getIdCanton())
                && Objects.equals(distritoOriginal, nueva.getIdDistrito());
        if (mismaClave) {
            nueva.setIdEstado(anterior.getIdEstado());
            repo.modificarDireccion(nueva);
            if (!existeDireccion(nueva, nueva.getIdEstado()))
                throw new IllegalStateException("Oracle no confirmo la direccion actualizada.");
            return;
        }
        insertarOReactivarDireccion(nueva);
        anterior.setIdEstado(inactivo());
        repo.eliminarDireccion(anterior);
        boolean anteriorActiva = repo.direcciones(usuarioId).stream().anyMatch(d ->
                Objects.equals(d.getIdProvincia(), provinciaOriginal)
                && Objects.equals(d.getIdCanton(), cantonOriginal)
                && Objects.equals(d.getIdDistrito(), distritoOriginal)
                && !inactivo().equals(d.getIdEstado()));
        if (anteriorActiva)
            throw new IllegalStateException("Oracle no confirmó la desactivación de la dirección anterior.");
    }

    @Transactional
    public void crearDireccionDesdeIncidente(Long usuarioId, DatoContacto nueva) {
        usuario(usuarioId);
        nueva.setIdUsuario(usuarioId);
        validarDireccion(nueva);
        insertarOReactivarDireccion(nueva);
    }

    @Transactional
    public void cambiarEstado(String tipo, DatoContacto identificador, boolean reactivar) {
        var anterior = buscar(tipo, identificador.getIdUsuario(), identificador.getValor(),
                              identificador.getIdProvincia(), identificador.getIdCanton(), identificador.getIdDistrito());
        Long nuevoEstado = reactivar ? activo() : inactivo();
        if (Objects.equals(anterior.getIdEstado(), nuevoEstado))
            throw new IllegalArgumentException("El registro ya tiene ese estado.");
        if ("telefono".equals(tipo)) {
            if (reactivar) repo.modificarTelefono(anterior.getIdUsuario(), anterior.getValor(), anterior.getValor(), nuevoEstado);
            else repo.eliminarTelefono(anterior.getIdUsuario(), anterior.getValor());
            if (!existeValor(tipo, anterior.getIdUsuario(), anterior.getValor(), nuevoEstado))
                throw new IllegalStateException("Oracle no confirmó el cambio de estado.");
        } else if ("correo".equals(tipo)) {
            if (reactivar) repo.modificarCorreo(anterior.getIdUsuario(), anterior.getValor(), anterior.getValor(), nuevoEstado);
            else repo.eliminarCorreo(anterior.getIdUsuario(), anterior.getValor());
            if (!existeValor(tipo, anterior.getIdUsuario(), anterior.getValor(), nuevoEstado))
                throw new IllegalStateException("Oracle no confirmó el cambio de estado.");
        } else if ("direccion".equals(tipo)) {
            anterior.setIdEstado(nuevoEstado);
            if (reactivar) repo.modificarDireccion(anterior); else repo.eliminarDireccion(anterior);
            if (!existeDireccion(anterior, nuevoEstado))
                throw new IllegalStateException("Oracle no confirmó el cambio de estado de la dirección.");
        } else throw new IllegalArgumentException("Tipo de dato no válido.");
    }
}
