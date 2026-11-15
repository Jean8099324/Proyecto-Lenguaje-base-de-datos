package com.rescatevial.controller;

import com.rescatevial.domain.Unidad;
import com.rescatevial.service.UnidadService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequestMapping("/unidades")
public class UnidadController {
    private final UnidadService service;
    public UnidadController(UnidadService service){this.service=service;}
    @GetMapping public String listar(Model model){var unidades=service.listar();model.addAttribute("unidades",unidades);model.addAttribute("totalUnidades",unidades.size());model.addAttribute("tipos",service.tipos());model.addAttribute("estados",estadosUnidad());return "unidades/listado";}
    @GetMapping("/nuevo") public String nuevo(Model model){model.addAttribute("unidadForm",new Unidad());opciones(model);return "unidades/formulario";}
    @GetMapping("/editar/{id}") public String editar(@PathVariable Long id,Model model,RedirectAttributes r){var u=service.buscar(id);if(u.isEmpty()){r.addFlashAttribute("mensajeError","Unidad no encontrada.");return "redirect:/unidades";}model.addAttribute("unidadForm",u.get());opciones(model);return "unidades/formulario";}
    @PostMapping("/guardar") public String guardar(@Valid @ModelAttribute("unidadForm") Unidad u,BindingResult errors,Model model,RedirectAttributes r){if(errors.hasErrors()){model.addAttribute("mensajeError","Complete los campos obligatorios.");opciones(model);return "unidades/formulario";}service.guardar(u);r.addFlashAttribute("mensajeExito",u.getId()==null?"Unidad registrada correctamente.":"Unidad actualizada correctamente.");return "redirect:/unidades";}
    @PostMapping("/{id}/desactivar") public String desactivar(@PathVariable Long id,RedirectAttributes r){service.desactivar(id);r.addFlashAttribute("mensajeExito","Unidad desactivada correctamente.");return "redirect:/unidades";}
    @GetMapping("/{id}/historial") public String historial(@PathVariable Long id,Model model,RedirectAttributes r){var u=service.buscar(id);if(u.isEmpty()){r.addFlashAttribute("mensajeError","Unidad no encontrada.");return "redirect:/unidades";}model.addAttribute("unidad",u.get());model.addAttribute("historial",service.historial(id));return "unidades/historial";}
    private void opciones(Model m){m.addAttribute("tipos",service.tipos());m.addAttribute("estados",estadosUnidad());}
    private java.util.List<com.rescatevial.domain.Estado> estadosUnidad(){return new java.util.ArrayList<>(service.estados().stream().filter(e->{String n=e.getNombreEstado();return !"ACTIVO".equalsIgnoreCase(n)&&!"ASIGNADO".equalsIgnoreCase(n)&&!"EN ATENCION".equalsIgnoreCase(n)&&!"REPORTADO".equalsIgnoreCase(n)&&!"TRASLADO".equalsIgnoreCase(n)&&!"FINALIZADO".equalsIgnoreCase(n);}).collect(java.util.stream.Collectors.toMap(e->e.getNombreEstado().trim().toUpperCase(java.util.Locale.ROOT),e->e,(a,b)->a,java.util.LinkedHashMap::new)).values());}
}
