
package com.rescatevial.controller;

import java.io.IOException;
import java.sql.SQLException;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.rescatevial.domain.Factura;
import com.rescatevial.service.FacturaPdfService;
import com.rescatevial.service.FacturaService;

/*
 * PANTALLA 24: FACTURACION
 *
 * DESCRIPCION:
 * CONTROLADOR ENCARGADO DE GESTIONAR
 * EL LISTADO, REGISTRO, MODIFICACION,
 * ELIMINACION Y DESCARGA DE FACTURAS PDF.
 *
 * PROYECTO: RESCATE VIAL CR
 */

@Controller
@RequestMapping("/facturas")
public class FacturaController {

    private final FacturaService facturaService;
    private final FacturaPdfService facturaPdfService;

    public FacturaController(
            FacturaService facturaService,
            FacturaPdfService facturaPdfService) {

        this.facturaService = facturaService;
        this.facturaPdfService = facturaPdfService;
    }

    // MOSTRAR LISTADO DE FACTURAS
    @GetMapping("/listado")
    public String listado(Model model) throws SQLException {

        model.addAttribute(
                "facturas",
                facturaService.listar());

        return "facturas/listado";
    }

    // MOSTRAR FORMULARIO DE REGISTRO
    @GetMapping("/nuevo")
    public String nuevo(Model model) {

        model.addAttribute("factura", new Factura());
        model.addAttribute("modoEdicion", false);

        return "facturas/modifica";
    }

    // MOSTRAR FORMULARIO DE MODIFICACION
    @GetMapping("/modificar")
    public String modificar(
            @RequestParam("idFactura") Long idFactura,
            Model model) throws SQLException {

        Factura factura = facturaService.buscarPorId(idFactura);

        if (factura == null) {
            return "redirect:/facturas/listado";
        }

        model.addAttribute("factura", factura);
        model.addAttribute("modoEdicion", true);

        return "facturas/modifica";
    }

    // REGISTRAR UNA NUEVA FACTURA
    @PostMapping("/guardar")
    public String guardar(
            @ModelAttribute Factura factura,
            RedirectAttributes redirectAttributes) {

        try {

            facturaService.insertar(factura);

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Factura registrada correctamente.");

        } catch (SQLException | IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "No se pudo registrar la factura: "
                            + e.getMessage());
        }

        return "redirect:/facturas/listado";
    }

    // ACTUALIZAR FACTURA
    @PostMapping("/actualizar")
    public String actualizar(
            @ModelAttribute Factura factura,
            RedirectAttributes redirectAttributes) {

        try {

            facturaService.modificar(factura);

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Factura actualizada correctamente.");

        } catch (SQLException | IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "No se pudo actualizar la factura: "
                            + e.getMessage());
        }

        return "redirect:/facturas/listado";
    }

    // ELIMINAR FACTURA
    @PostMapping("/eliminar")
    public String eliminar(
            @RequestParam("idFactura") Long idFactura,
            RedirectAttributes redirectAttributes) {

        try {

            facturaService.eliminar(idFactura);

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Operacion de eliminacion realizada correctamente.");

        } catch (SQLException | IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "No se pudo eliminar la factura: "
                            + e.getMessage());
        }

        return "redirect:/facturas/listado";
    }

    /*
     * DESCARGAR FACTURA EN FORMATO PDF
     *
     * DESCRIPCION:
     * CONSULTAR UNA FACTURA REGISTRADA
     * EN ORACLE Y GENERAR SU DOCUMENTO PDF
     * UTILIZANDO APACHE PDFBOX.
     */

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> descargarPdf(
            @RequestParam("idFactura") Long idFactura) {

        try {

            // MOSTRAR ID SOLICITADO EN LA TERMINAL
            System.out.println(
                    "INICIANDO DESCARGA PDF DE FACTURA: "
                            + idFactura);

            // GENERAR PDF CON LOS DATOS DE ORACLE
            byte[] pdf = facturaPdfService.generarPdf(idFactura);

            // MOSTRAR CONFIRMACION DE GENERACION
            System.out.println(
                    "PDF GENERADO CORRECTAMENTE: "
                            + idFactura);

            // CONFIGURAR NOMBRE DEL ARCHIVO
            String nombreArchivo = "Factura_" + idFactura + ".pdf";

            // CONFIGURAR CABECERAS HTTP
            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(
                    MediaType.APPLICATION_PDF);

            headers.setContentDisposition(
                    ContentDisposition.attachment()
                            .filename(nombreArchivo)
                            .build());

            headers.setContentLength(pdf.length);

            // DEVOLVER ARCHIVO PARA DESCARGA
            return new ResponseEntity<>(
                    pdf,
                    headers,
                    HttpStatus.OK);

        } catch (IllegalArgumentException e) {

            /*
             * ERROR DE VALIDACION O FACTURA
             * NO ENCONTRADA.
             */

            System.err.println(
                    "ERROR AL BUSCAR O GENERAR FACTURA PDF: "
                            + e.getMessage());

            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .build();

        } catch (SQLException | IOException e) {

            /*
             * ERROR DE CONSULTA A ORACLE
             * O GENERACION DEL DOCUMENTO PDF.
             */

            System.err.println(
                    "ERROR DE ORACLE O PDFBOX: "
                            + e.getMessage());

            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }
}
