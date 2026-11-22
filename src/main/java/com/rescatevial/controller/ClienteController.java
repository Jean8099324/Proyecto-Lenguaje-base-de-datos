package com.rescatevial.controller;

import com.rescatevial.domain.Usuario;
import com.rescatevial.domain.DatoContacto;
import com.rescatevial.service.ClienteService;
import com.rescatevial.service.CorreoService;
import com.rescatevial.service.DatoContactoService;
import com.rescatevial.service.VehiculoService;
import com.rescatevial.service.IncidenteService;
import jakarta.validation.Valid;
import java.util.List;
import java.security.SecureRandom;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/clientes")
public class ClienteController {
    private final ClienteService clienteService;
    private final CorreoService correoService;
    private final DatoContactoService datoContactoService;
    private final VehiculoService vehiculoService;
    private final IncidenteService incidenteService;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CARACTERES_PASSWORD = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

    public ClienteController(ClienteService clienteService, CorreoService correoService,
                             DatoContactoService datoContactoService, VehiculoService vehiculoService,
                             IncidenteService incidenteService) {
        this.clienteService = clienteService;
        this.correoService = correoService;
        this.datoContactoService = datoContactoService;
        this.vehiculoService = vehiculoService;
        this.incidenteService = incidenteService;
    }

    @GetMapping
    public String listado(@RequestParam(defaultValue = "1") int pagina, Model model) {
        int tamanioPagina = 10;
        int paginaSolicitada = Math.max(1, pagina);
        var resultado = clienteService.listarPaginado(paginaSolicitada, tamanioPagina);
        long totalClientes = resultado.total();
        int totalPaginas = (int) Math.max(1, (totalClientes + tamanioPagina - 1) / tamanioPagina);
        int paginaActual = Math.min(paginaSolicitada, totalPaginas);
        if (paginaActual != paginaSolicitada) {
            resultado = clienteService.listarPaginado(paginaActual, tamanioPagina);
        }
        model.addAttribute("clientes", resultado.usuarios());
        model.addAttribute("totalClientes", totalClientes);
        model.addAttribute("paginaActual", paginaActual);
        model.addAttribute("totalPaginas", totalPaginas);
        model.addAttribute("inicioRegistro", totalClientes == 0 ? 0 : (paginaActual - 1) * tamanioPagina + 1);
        model.addAttribute("finRegistro", Math.min((long) paginaActual * tamanioPagina, totalClientes));
        return "clientes/listado";
    }

    @GetMapping("/nuevo")
    public String nuevo(@RequestParam(required = false) String retorno, Model model) {
        Usuario cliente = new Usuario();
        cliente.setTipoUsuario(clienteService.tipoClienteId());
        cliente.setEstado(clienteService.estadoActivoId());
        model.addAttribute("clienteForm", cliente);
        model.addAttribute("esNuevo", true);
        prepararContactos(model, cliente);
        model.addAttribute("retorno", retornoValido(retorno) ? retorno : null);
        return "clientes/formulario";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes redireccion) {
        var cliente = clienteService.buscar(id);
        if (cliente.isEmpty()) {
            redireccion.addFlashAttribute("mensajeError", "No se encontró el cliente.");
            return "redirect:/clientes";
        }
        Usuario formulario = cliente.get();
        formulario.setPassword(null); // Nunca mostrar el hash de Oracle.
        model.addAttribute("clienteForm", formulario);
        model.addAttribute("esNuevo", false);
        prepararContactos(model, formulario);
        return "clientes/formulario";
    }

    @GetMapping("/detalle/{id}")
    public String detalle(@PathVariable Long id, Model model, RedirectAttributes redireccion) {
        var cliente = clienteService.buscar(id);
        if (cliente.isEmpty()) {
            redireccion.addFlashAttribute("mensajeError", "No se encontró el cliente.");
            return "redirect:/clientes";
        }
        model.addAttribute("cliente", cliente.get());
        model.addAttribute("direcciones", datoContactoService.direcciones(id));
        model.addAttribute("telefonos", datoContactoService.telefonos(id));
        model.addAttribute("correos", datoContactoService.correos(id));
        cargarRelaciones(model, id);
        return "clientes/detalle";
    }

    @GetMapping("/username-disponible")
    @ResponseBody
    public java.util.Map<String, Object> usernameDisponible(
            @RequestParam String username,
            @RequestParam(required = false) Long id) {
        String valor = username == null ? "" : username.trim();
        if (valor.isBlank()) {
            return java.util.Map.of("disponible", false, "mensaje", "Ingrese un username.");
        }
        var existente = clienteService.buscarPorUsername(valor);
        boolean disponible = existente.isEmpty() || (id != null && id.equals(existente.get().getId()));
        return java.util.Map.of("disponible", disponible,
                "mensaje", disponible ? "Username disponible." : "Username no disponible.");
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("clienteForm") Usuario cliente,
                          BindingResult errores, Model model, RedirectAttributes redireccion,
                          @RequestParam(required = false) String retorno,
                          @RequestParam(required = false) List<Long> idProvincia,
                          @RequestParam(required = false) List<Long> idCanton,
                          @RequestParam(required = false) List<Long> idDistrito,
                          @RequestParam(required = false) List<String> otrasSenas,
                          @RequestParam(name = "contactoCorreo", required = false) List<String> correos,
                          @RequestParam(name = "telefono", required = false) List<String> telefonos) {
        boolean nuevo = cliente.getId() == null;
        if (errores.hasErrors()) {
            model.addAttribute("esNuevo", nuevo);
            prepararContactos(model, cliente);
            model.addAttribute("retorno", retornoValido(retorno) ? retorno : null);
            model.addAttribute("mensajeError", "Revisa los campos obligatorios y sus formatos.");
            return "clientes/formulario";
        }
        try {
            List<String> destinatarios = correos == null ? List.of() : correos.stream()
                    .filter(correo -> correo != null && !correo.isBlank())
                    .map(correo -> correo.trim())
                    .collect(Collectors.toList());
            if (nuevo) {
                if (destinatarios.isEmpty()) {
                    throw new IllegalArgumentException("Ingrese al menos un correo para enviar las credenciales.");
                }
                cliente.setPassword(generarPasswordTemporal());
            }
            clienteService.guardar(cliente);
            guardarDirecciones(cliente.getId(), idProvincia, idCanton, idDistrito, otrasSenas);
            guardarContactos(cliente.getId(), "correo", correos);
            guardarContactos(cliente.getId(), "telefono", telefonos);
            if (nuevo) {
                for (String destinatario : destinatarios) {
                    correoService.enviarCredenciales(destinatario, cliente.getUsername(), cliente.getPassword());
                }
            }
            if (nuevo && retornoValido(retorno)) {
                return "redirect:" + retorno + "?clienteId=" + cliente.getId();
            }
            redireccion.addFlashAttribute("mensajeExito", nuevo
                    ? "Cliente registrado correctamente." : "Cliente actualizado correctamente.");
            return "redirect:/clientes";
        } catch (Exception ex) {
            model.addAttribute("esNuevo", nuevo);
            prepararContactos(model, cliente);
            model.addAttribute("retorno", retornoValido(retorno) ? retorno : null);
            String detalle = ex.getMessage();
            String mensaje = ex instanceof IllegalArgumentException || ex instanceof IllegalStateException
                    ? detalle
                    : "No se pudo guardar el cliente. Revisa sus datos."
                        + (detalle == null || detalle.isBlank() ? "" : " Detalle: " + detalle);
            model.addAttribute("mensajeError", mensaje);
            return "clientes/formulario";
        }
    }

    private static boolean retornoValido(String retorno) {
        return retorno != null && retorno.matches("/incidentes/modificar/\\d+");
    }

    private void prepararContactos(Model model, Usuario cliente) {
        cargarRelaciones(model, cliente.getId());
        if (cliente.getId() != null) {
            model.addAttribute("clientesDisponibles", clienteService.listar());
        }
        model.addAttribute("provincias", datoContactoService.provincias());
        model.addAttribute("cantones", datoContactoService.cantones());
        model.addAttribute("distritos", datoContactoService.distritos());
        if (cliente.getId() == null) {
            model.addAttribute("direcciones", java.util.List.of());
            model.addAttribute("telefonos", java.util.List.of());
            model.addAttribute("correos", java.util.List.of());
            return;
        }
        model.addAttribute("direcciones", datoContactoService.direcciones(cliente.getId()));
        model.addAttribute("telefonos", datoContactoService.telefonos(cliente.getId()));
        model.addAttribute("correos", datoContactoService.correos(cliente.getId()));
    }

    private void cargarRelaciones(Model model, Long clienteId) {
        if (clienteId == null) {
            model.addAttribute("vehiculos", List.of());
            model.addAttribute("incidentes", List.of());
            model.addAttribute("incidentesActivos", List.of());
            return;
        }
        model.addAttribute("vehiculos", vehiculoService.listarPorCliente(clienteId));
        // Los incidentes guardan el cliente en ID_USUARIO; cuando esta relación
        // falta, el propietario se obtiene del vehículo asociado.
        var vehiculosCliente = vehiculoService.listarPorCliente(clienteId);
        var idsVehiculosCliente = vehiculosCliente.stream()
                .map(vehiculo -> vehiculo.getId())
                .collect(java.util.stream.Collectors.toSet());
        var incidentes = incidenteService.listar().stream()
                .filter(incidente -> clienteId.equals(incidente.getIdUsuario())
                        || idsVehiculosCliente.contains(incidente.getIdVehiculo()))
                .toList();
        model.addAttribute("incidentes", incidentes);
        model.addAttribute("incidentesActivos", incidentes.stream()
                .filter(incidente -> java.util.Set.of("REPORTADO", "ASIGNADO", "UNIDAD EN CAMINO",
                                "EN ATENCION", "TRASLADO")
                        .contains(incidente.getEstadoEtiqueta() == null ? ""
                                : incidente.getEstadoEtiqueta().trim().toUpperCase(java.util.Locale.ROOT)))
                .toList());
    }

    private void guardarContactos(Long clienteId, String tipo, List<String> valores) {
        if (valores == null) {
            return;
        }
        for (String valor : valores) {
            if (valor == null || valor.isBlank()) {
                continue;
            }
            DatoContacto contacto = new DatoContacto();
            contacto.setIdUsuario(clienteId);
            contacto.setValor(valor);
            datoContactoService.guardar(tipo, contacto, false);
        }
    }

    private static String generarPasswordTemporal() {
        StringBuilder password = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            password.append(CARACTERES_PASSWORD.charAt(RANDOM.nextInt(CARACTERES_PASSWORD.length())));
        }
        return password.toString();
    }

    private void guardarDirecciones(Long clienteId, List<Long> provincias, List<Long> cantones,
                                    List<Long> distritos, List<String> senas) {
        int cantidad = Math.max(Math.max(tamano(provincias), tamano(cantones)),
                Math.max(tamano(distritos), tamano(senas)));
        for (int i = 0; i < cantidad; i++) {
            Long provincia = valor(provincias, i);
            Long canton = valor(cantones, i);
            Long distrito = valor(distritos, i);
            String otrasSenas = valor(senas, i);
            boolean vacia = provincia == null && canton == null && distrito == null
                    && (otrasSenas == null || otrasSenas.isBlank());
            if (vacia) {
                continue;
            }
            if (provincia == null || canton == null || distrito == null) {
                throw new IllegalArgumentException("Completa provincia, cantón y distrito en cada dirección.");
            }
            DatoContacto direccion = new DatoContacto();
            direccion.setIdProvincia(provincia);
            direccion.setIdCanton(canton);
            direccion.setIdDistrito(distrito);
            direccion.setOtrasSenas(otrasSenas);
            datoContactoService.crearDireccionDesdeIncidente(clienteId, direccion);
        }
    }

    private static int tamano(List<?> valores) {
        return valores == null ? 0 : valores.size();
    }

    private static <T> T valor(List<T> valores, int indice) {
        return valores != null && indice < valores.size() ? valores.get(indice) : null;
    }

    @PostMapping("/{id}/desactivar")
    public String desactivar(@PathVariable Long id, RedirectAttributes redireccion) {
        try {
            clienteService.desactivar(id);
            redireccion.addFlashAttribute("mensajeExito", "Cliente desactivado.");
        } catch (Exception ex) {
            redireccion.addFlashAttribute("mensajeError", "No se pudo desactivar el cliente.");
        }
        return "redirect:/clientes";
    }

    @PostMapping("/{id}/reactivar")
    public String reactivar(@PathVariable Long id, RedirectAttributes redireccion) {
        try {
            clienteService.reactivar(id);
            redireccion.addFlashAttribute("mensajeExito", "Cliente reactivado.");
        } catch (Exception ex) {
            redireccion.addFlashAttribute("mensajeError", "No se pudo reactivar el cliente.");
        }
        return "redirect:/clientes";
    }
}
