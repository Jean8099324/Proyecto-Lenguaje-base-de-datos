package com.rescatevial.controller;

import com.rescatevial.domain.Cliente;
import com.rescatevial.service.ClienteService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/clientes")
public class ClienteController {
    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    @GetMapping
    public String listado(Model model) {
        completarModelo(model);
        if (!model.containsAttribute("clienteForm")) model.addAttribute("clienteForm", new Cliente());
        return "clientes/listado";
    }

    @PostMapping
    public String registrar(@Valid Cliente clienteForm, BindingResult bindingResult,
                            RedirectAttributes redirectAttributes, Model model) {
        if (bindingResult.hasErrors()) {
            completarModelo(model);
            model.addAttribute("modalAbierto", true);
            return "clientes/listado";
        }
        try {
            service.registrar(clienteForm);
            redirectAttributes.addFlashAttribute("mensajeExito", "Cliente registrado correctamente.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/clientes";
    }

    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable long id, @RequestParam boolean activo,
                                RedirectAttributes redirectAttributes) {
        service.cambiarEstado(id, activo);
        redirectAttributes.addFlashAttribute("mensajeExito", "Estado del cliente actualizado.");
        return "redirect:/clientes";
    }

    @GetMapping("/{id}/detalle")
    @ResponseBody
    public Map<String, Object> detalle(@PathVariable long id) {
        Cliente cliente = service.buscar(id);
        return Map.of("nombre", cliente.nombreCompleto(), "username", cliente.username(),
                "telefono", cliente.telefono(), "correo", cliente.correo(),
                "vehiculos", cliente.vehiculos(), "estado", cliente.estado());
    }

    private void completarModelo(Model model) {
        List<Cliente> clientes = service.listar();
        model.addAttribute("clientes", clientes);
        model.addAttribute("totalClientes", clientes.size());
        model.addAttribute("clientesActivos", service.contarActivos());
        model.addAttribute("vehiculosAsociados", service.contarVehiculos());
    }
}
