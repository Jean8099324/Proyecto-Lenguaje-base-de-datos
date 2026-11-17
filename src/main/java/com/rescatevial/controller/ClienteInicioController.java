package com.rescatevial.controller;

import com.rescatevial.domain.DatoContacto;
import com.rescatevial.domain.Vehiculo;
import com.rescatevial.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;

@Controller
@RequestMapping("/cliente")
public class ClienteInicioController {
    private final PortalClienteService portal;
    private final UsuarioService usuarios;
    private final DatoContactoService datos;
    private final VehiculoService vehiculos;
    private final IncidenteService incidentes;
    public ClienteInicioController(PortalClienteService portal, UsuarioService usuarios, DatoContactoService datos,
                                    VehiculoService vehiculos, IncidenteService incidentes) {
        this.portal=portal; this.usuarios=usuarios; this.datos=datos; this.vehiculos=vehiculos; this.incidentes=incidentes;
    }
    @GetMapping({"", "/inicio"})
    public String inicio(Authentication auth, Model model) {
        var cliente=portal.actual(auth);
        model.addAttribute("cliente",cliente);
        model.addAttribute("vehiculos",vehiculos.listarPorCliente(cliente.getId()));
        model.addAttribute("incidentes",incidentes.listarPorCliente(cliente.getId()));
        model.addAttribute("provincias",datos.provincias());
        model.addAttribute("cantones",datos.cantones());
        model.addAttribute("distritos",datos.distritos());
        model.addAttribute("tipos",incidentes.tipos());
        return "cliente/inicio";
    }
    @GetMapping("/cuenta")
    public String cuenta(Authentication auth, Model model) {
        var cliente=portal.actual(auth);
        model.addAttribute("cliente",cliente);
        model.addAttribute("correos",datos.correos(cliente.getId()));
        model.addAttribute("telefonos",datos.telefonos(cliente.getId()));
        model.addAttribute("direcciones",datos.direcciones(cliente.getId()));
        model.addAttribute("provincias",datos.provincias());
        model.addAttribute("cantones",datos.cantones());
        model.addAttribute("distritos",datos.distritos());
        return "cliente/cuenta";
    }

    @InitBinder("vehiculoForm")
    public void camposVehiculo(WebDataBinder binder) {
        binder.setAllowedFields("placa", "codigoChasis", "idMarca", "idModelo", "idAnio");
    }

    @GetMapping("/vehiculos/nuevo")
    public String nuevoVehiculo(Authentication auth, Model model) {
        portal.actual(auth);
        model.addAttribute("vehiculoForm", new Vehiculo());
        return formularioVehiculo(model);
    }

    @GetMapping("/vehiculos/validar-placa")
    @ResponseBody
    public java.util.Map<String, Boolean> validarPlacaVehiculo(Authentication auth, @RequestParam String placa) {
        portal.actual(auth);
        return java.util.Map.of("duplicada", vehiculos.placaRegistrada(placa, null));
    }

    @PostMapping("/vehiculos/guardar")
    public String guardarVehiculo(Authentication auth, @Valid @ModelAttribute("vehiculoForm") Vehiculo formulario,
                                  BindingResult errores, Model model, RedirectAttributes redirect) {
        portal.actual(auth);
        if (errores.hasErrors()) return formularioVehiculo(model);
        try {
            portal.agregarVehiculo(auth, formulario);
            redirect.addFlashAttribute("mensajeExito", "Vehículo agregado a tu cuenta correctamente.");
            return "redirect:/cliente/inicio#mis-vehiculos";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("mensajeError", ex.getMessage());
        } catch (org.springframework.dao.DataAccessException ex) {
            model.addAttribute("mensajeError", "No fue posible guardar el vehículo. Inténtalo nuevamente.");
        }
        return formularioVehiculo(model);
    }

    private String formularioVehiculo(Model model) {
        model.addAttribute("portalCliente", true);
        model.addAttribute("esNuevo", true);
        model.addAttribute("marcas", vehiculos.marcas());
        model.addAttribute("modelos", vehiculos.modelos());
        model.addAttribute("anios", vehiculos.anios());
        model.addAttribute("estadoActivo", vehiculos.estadoActivo());
        return "vehiculos/formulario";
    }
    @GetMapping("/incidentes/{id}")
    public String incidente(@PathVariable Long id, Authentication auth, Model model) {
        var i=portal.incidentePropio(auth,id);
        model.addAttribute("incidente",i);
        model.addAttribute("estado",incidentes.listarPorCliente(portal.actual(auth).getId()).stream()
                .filter(item -> id.equals(item.getId())).map(item -> item.getEstadoEtiqueta()).findFirst().orElse("Sin estado"));
        return "cliente/incidente";
    }
    @PostMapping("/perfil")
    public String perfil(Authentication auth, @RequestParam String nombre, @RequestParam String primerApellido,
                         @RequestParam(defaultValue="") String segundoApellido, HttpServletRequest request, RedirectAttributes redirect) {
        portal.actualizarPerfil(auth,nombre,primerApellido,segundoApellido);
        request.getSession().setAttribute("nombreUsuario",(nombre+" "+primerApellido+" "+segundoApellido).trim());
        return exito(redirect,"Datos personales actualizados.");
    }
    @PostMapping("/password")
    public String password(Authentication auth, @RequestParam String actual, @RequestParam String nueva,
                           @RequestParam String confirmacion, HttpServletRequest request) {
        portal.actual(auth);
        usuarios.cambiarPasswordPropia(auth.getName(),actual,nueva,confirmacion);
        request.getSession().invalidate();
        SecurityContextHolder.clearContext();
        return "redirect:/login?passwordChanged=true";
    }
    // Lista explícita de campos: nunca se aceptan propietario, roles, estado ni ID de usuario del navegador.
    @PostMapping("/datos/{tipo}")
    public String dato(@PathVariable String tipo, Authentication auth, @RequestParam(defaultValue="") String valor,
                       @RequestParam(defaultValue="") String valorAnterior, @RequestParam(required=false) Long idProvincia,
                       @RequestParam(required=false) Long idCanton, @RequestParam(required=false) Long idDistrito,
                       @RequestParam(defaultValue="") String otrasSenas, @RequestParam(defaultValue="false") boolean editar,
                       RedirectAttributes redirect) {
        DatoContacto d=dato(valor,idProvincia,idCanton,idDistrito); d.setValorAnterior(valorAnterior); d.setOtrasSenas(otrasSenas);
        portal.guardarDato(auth,tipo,d,editar);
        return exito(redirect,"Dato de contacto guardado.");
    }
    @PostMapping("/datos/{tipo}/estado")
    public String estado(@PathVariable String tipo, Authentication auth, @RequestParam(defaultValue="") String valor,
                         @RequestParam(required=false) Long idProvincia, @RequestParam(required=false) Long idCanton,
                         @RequestParam(required=false) Long idDistrito, @RequestParam(defaultValue="false") boolean reactivar,
                         RedirectAttributes redirect) {
        portal.estadoDato(auth,tipo,dato(valor,idProvincia,idCanton,idDistrito),reactivar);
        return exito(redirect,reactivar ? "Dato reactivado." : "Dato desactivado.");
    }
    private DatoContacto dato(String valor,Long provincia,Long canton,Long distrito) {
        var d=new DatoContacto(); d.setValor(valor); d.setIdProvincia(provincia); d.setIdCanton(canton); d.setIdDistrito(distrito); return d;
    }
    @PostMapping("/incidentes")
    public String reportar(Authentication auth, @RequestParam Long idVehiculo, @RequestParam Long idTipoIncidente,
                           @RequestParam Long idProvincia, @RequestParam Long idCanton, @RequestParam Long idDistrito,
                           @RequestParam String detalles, @RequestParam(defaultValue="") String otrasSenas,
                           @RequestParam(defaultValue="") String nombreContacto,
                           @RequestParam(defaultValue="") String primerApellidoContacto,
                           @RequestParam(defaultValue="") String segundoApellidoContacto,
                           @RequestParam String telefono, @RequestParam(defaultValue="") String correo, RedirectAttributes redirect) {
        portal.reportar(auth,idVehiculo,idTipoIncidente,idProvincia,idCanton,idDistrito,otrasSenas,detalles,
                nombreContacto,primerApellidoContacto,segundoApellidoContacto,telefono,correo);
        return exito(redirect,"Incidente reportado correctamente.");
    }
    private String exito(RedirectAttributes r,String mensaje) { r.addFlashAttribute("mensajeExito",mensaje); return "redirect:/cliente/inicio"; }
    @ExceptionHandler(IllegalArgumentException.class)
    public String validacion(IllegalArgumentException ex, RedirectAttributes r) { r.addFlashAttribute("mensajeError",ex.getMessage()); return "redirect:/cliente/inicio"; }
    @ExceptionHandler({IllegalStateException.class, org.springframework.dao.DataAccessException.class})
    public String fallo(RedirectAttributes r) { r.addFlashAttribute("mensajeError","No fue posible completar la operación. Verifica los datos e inténtalo nuevamente."); return "redirect:/cliente/inicio"; }
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String denegado(Authentication auth, Model model) {
        var menu = new MenuAdvice();
        model.addAttribute("esCliente", menu.esCliente(auth));
        model.addAttribute("esAdministrador", menu.esAdministrador(auth));
        model.addAttribute("sesionAutenticada", menu.sesionAutenticada(auth));
        return "error/403";
    }
}
