package com.rescatevial.service;

import com.rescatevial.domain.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortalClienteService {
    private final UsuarioService usuarios;
    private final VehiculoService vehiculos;
    private final IncidenteService incidentes;
    private final DatoContactoService datos;
    private final EstadoService estados;
    public PortalClienteService(UsuarioService usuarios, VehiculoService vehiculos, IncidenteService incidentes,
                                DatoContactoService datos, EstadoService estados) {
        this.usuarios=usuarios; this.vehiculos=vehiculos; this.incidentes=incidentes; this.datos=datos; this.estados=estados;
    }
    public Usuario actual(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth.getAuthorities().stream()
                .noneMatch(a -> "ROLE_CLIENT".equals(a.getAuthority()) || "ROLE_ADMIN".equals(a.getAuthority())))
            throw new AccessDeniedException("Acceso restringido");
        return usuarios.getUsuarioPorUsername(auth.getName()).orElseThrow(() -> new AccessDeniedException("Acceso restringido"));
    }
    public Vehiculo vehiculoPropio(Authentication auth, Long id) {
        Long usuarioId=actual(auth).getId();
        return vehiculos.buscar(id).filter(v -> usuarioId.equals(v.getIdUsuario()))
                .orElseThrow(() -> new AccessDeniedException("No tiene acceso a este vehículo."));
    }
    public Incidente incidentePropio(Authentication auth, Long id) {
        Long usuarioId=actual(auth).getId();
        return incidentes.buscar(id).filter(i -> usuarioId.equals(i.getIdUsuario()))
                .orElseThrow(() -> new AccessDeniedException("No tiene acceso a este incidente."));
    }
    @Transactional
    public void agregarVehiculo(Authentication auth, Vehiculo formulario) {
        Usuario usuario = actual(auth);
        // Solo se copian los datos del vehículo; identidad y propietario se fijan en servidor.
        Vehiculo nuevo = new Vehiculo();
        nuevo.setPlaca(formulario.getPlaca());
        nuevo.setCodigoChasis(formulario.getCodigoChasis());
        nuevo.setIdMarca(formulario.getIdMarca());
        nuevo.setIdModelo(formulario.getIdModelo());
        nuevo.setIdAnio(formulario.getIdAnio());
        nuevo.setIdUsuario(usuario.getId());
        vehiculos.guardar(nuevo);
    }
    @Transactional
    public void guardarDato(Authentication auth, String tipo, DatoContacto dato, boolean editar) {
        dato.setIdUsuario(actual(auth).getId());
        datos.guardar(tipo, dato, editar);
    }
    @Transactional
    public void estadoDato(Authentication auth, String tipo, DatoContacto dato, boolean reactivar) {
        dato.setIdUsuario(actual(auth).getId());
        datos.cambiarEstado(tipo, dato, reactivar);
    }
    @Transactional
    public void actualizarPerfil(Authentication auth, String nombre, String apellido, String segundo) {
        Usuario usuario=actual(auth);
        if (nombre==null || nombre.isBlank() || nombre.length()>100 || apellido==null || apellido.isBlank()
                || apellido.length()>100 || (segundo!=null && segundo.length()>100))
            throw new IllegalArgumentException("Revise el nombre y los apellidos (máximo 100 caracteres por campo).");
        usuarios.actualizarNombreDesdeIncidente(usuario.getId(), nombre, apellido, segundo);
    }
    @Transactional
    public void reportar(Authentication auth, Long vehiculoId, Long tipo, Long provincia, Long canton, Long distrito,
                         String senas, String detalles, String nombreContacto, String primerApellidoContacto,
                         String segundoApellidoContacto, String telefono, String correo) {
        Usuario usuario=actual(auth);
        if (nombreContacto == null || nombreContacto.isBlank() || nombreContacto.trim().length() > 100)
            throw new IllegalArgumentException("Indique el nombre del contacto (máximo 100 caracteres).");
        if ((primerApellidoContacto != null && primerApellidoContacto.trim().length() > 100)
                || (segundoApellidoContacto != null && segundoApellidoContacto.trim().length() > 100))
            throw new IllegalArgumentException("Los apellidos del contacto admiten un máximo de 100 caracteres cada uno.");
        Vehiculo vehiculo=vehiculoPropio(auth, vehiculoId);
        if (!vehiculos.estadoActivo().equals(vehiculo.getIdEstado())) throw new IllegalArgumentException("El vehículo está inactivo.");
        if (detalles==null || detalles.isBlank() || detalles.length()>500)
            throw new IllegalArgumentException("Describa el incidente en un máximo de 500 caracteres.");
        if (senas==null || senas.isBlank() || senas.trim().length()>200)
            throw new IllegalArgumentException("Indique las otras señas en un máximo de 200 caracteres.");
        if (telefono==null || !telefono.matches("\\d{8}")) throw new IllegalArgumentException("Indique un teléfono de 8 dígitos.");
        if (correo!=null && !correo.isBlank() && (correo.length()>150 || !correo.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")))
            throw new IllegalArgumentException("Indique un correo válido.");
        if (incidentes.tipos().stream().noneMatch(t -> t.getId().equals(tipo))) throw new IllegalArgumentException("Seleccione un tipo de incidente válido.");
        boolean ubicacion=datos.provincias().stream().anyMatch(p -> p.getId().equals(provincia))
                && datos.cantones().stream().anyMatch(c -> c.getId().equals(canton) && Objects.equals(c.getIdPadre(),provincia))
                && datos.distritos().stream().anyMatch(d -> d.getId().equals(distrito) && Objects.equals(d.getIdPadre(),canton));
        if (!ubicacion) throw new IllegalArgumentException("La provincia, el cantón y el distrito no corresponden.");
        Incidente i=new Incidente();
        i.setIdCliente(usuario.getId()); i.setIdUsuario(usuario.getId()); i.setIdVehiculo(vehiculoId);
        i.setIdTipoIncidente(tipo); i.setIdProvincia(provincia); i.setIdCanton(canton); i.setIdDistrito(distrito);
        i.setDetalles(detalles.trim()); i.setOtrasSenas(senas.trim());
        var ahora=ZonedDateTime.now(ZoneId.of("America/Costa_Rica"));
        i.setFechaReporte(ahora.toLocalDate()); i.setHoraReporte(ahora.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        i.setIdEstado(estados.listar().stream().filter(e -> "REPORTADO".equalsIgnoreCase(e.getNombreEstado().trim()))
                .map(e -> e.getId()).findFirst().orElseThrow(() -> new IllegalStateException("No existe el estado REPORTADO.")));
        i.setIdPrioridad(incidentes.prioridades().stream().filter(p -> "MEDIA".equalsIgnoreCase(p.getEtiqueta().trim()))
                .map(p -> p.getId()).findFirst().orElseThrow(() -> new IllegalStateException("No existe la prioridad MEDIA.")));
        i.setNombreReportante(nombreContacto.trim());
        i.setPrimerApellidoReportante(primerApellidoContacto == null ? null : primerApellidoContacto.trim());
        i.setSegundoApellidoReportante(segundoApellidoContacto == null ? null : segundoApellidoContacto.trim());
        i.setTelefonoReportante(telefono); i.setCorreoReportante(correo);
        incidentes.guardar(i);
    }
}
