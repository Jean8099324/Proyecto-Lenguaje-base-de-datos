package com.rescatevial.controller;

import com.rescatevial.domain.Usuario;
import com.rescatevial.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/clientes")
public class ClienteController {
    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public String listado(Model model) {
        var clientes = clienteService.listar();
        model.addAttribute("clientes", clientes);
        model.addAttribute("totalClientes", clientes.size());
        return "clientes/listado";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        Usuario cliente = new Usuario();
        cliente.setTipoUsuario(clienteService.tipoClienteId());
        cliente.setEstado(clienteService.estadoActivoId());
        model.addAttribute("clienteForm", cliente);
        model.addAttribute("esNuevo", true);
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
        return "clientes/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("clienteForm") Usuario cliente,
                          BindingResult errores, Model model, RedirectAttributes redireccion) {
        boolean nuevo = cliente.getId() == null;
        if (errores.hasErrors()) {
            cliente.setPassword(null);
            model.addAttribute("esNuevo", nuevo);
            model.addAttribute("mensajeError", "Revisa los campos obligatorios y sus formatos.");
            return "clientes/formulario";
        }
        try {
            clienteService.guardar(cliente);
            redireccion.addFlashAttribute("mensajeExito", nuevo
                    ? "Cliente registrado correctamente." : "Cliente actualizado correctamente.");
            return "redirect:/clientes";
        } catch (Exception ex) {
            cliente.setPassword(null);
            model.addAttribute("esNuevo", nuevo);
            model.addAttribute("mensajeError", "No se pudo guardar el cliente. Revisa sus datos.");
            return "clientes/formulario";
        }
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
