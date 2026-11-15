
package com.rescatevial.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import com.rescatevial.domain.Factura;

/*
 * PANTALLA 24: FACTURACION
 *
 * DESCRIPCION:
 * SERVICIO ENCARGADO DE GENERAR
 * FACTURAS EN FORMATO PDF UTILIZANDO
 * APACHE PDFBOX Y LOS DATOS DE ORACLE.
 *
 * PROYECTO: RESCATE VIAL CR
 */

@Service
public class FacturaPdfService {

        private final FacturaService facturaService;

        public FacturaPdfService(FacturaService facturaService) {
                this.facturaService = facturaService;
        }

        /*
         * GENERAR FACTURA PDF POR ID
         *
         * DESCRIPCION:
         * CONSULTAR LA FACTURA EN ORACLE
         * Y GENERAR EL DOCUMENTO PDF.
         */

        public byte[] generarPdf(Long idFactura)
                        throws SQLException, IOException {

                // MOSTRAR INICIO DEL PROCESO
                System.out.println(
                                "PDF SERVICE - INICIANDO GENERACION");

                System.out.println(
                                "PDF SERVICE - BUSCANDO FACTURA: "
                                                + idFactura);

                // CONSULTAR FACTURA EN ORACLE
                Factura factura = facturaService.buscarPorId(idFactura);

                // VALIDAR QUE EXISTA LA FACTURA
                if (factura == null) {

                        System.err.println(
                                        "PDF SERVICE - FACTURA NO ENCONTRADA: "
                                                        + idFactura);

                        throw new IllegalArgumentException(
                                        "No se encontro la factura solicitada.");
                }

                System.out.println(
                                "PDF SERVICE - FACTURA ENCONTRADA: "
                                                + factura.getIdFactura());

                // CREAR DOCUMENTO PDF
                try (
                                PDDocument documento = new PDDocument();
                                ByteArrayOutputStream salida = new ByteArrayOutputStream()) {

                        // CREAR PAGINA TAMAÑO A4
                        PDPage pagina = new PDPage(PDRectangle.A4);

                        documento.addPage(pagina);

                        // CONFIGURAR FUENTES
                        PDType1Font fuenteNormal = new PDType1Font(
                                        Standard14Fonts.FontName.HELVETICA);

                        PDType1Font fuenteNegrita = new PDType1Font(
                                        Standard14Fonts.FontName.HELVETICA_BOLD);

                        try (PDPageContentStream contenido = new PDPageContentStream(documento, pagina)) {

                                float anchoPagina = pagina.getMediaBox().getWidth();

                                /*
                                 * ENCABEZADO
                                 */

                                // COLOR AZUL OSCURO
                                contenido.setNonStrokingColor(
                                                new Color(11, 31, 51));

                                contenido.addRect(
                                                0,
                                                730,
                                                anchoPagina,
                                                112);

                                contenido.fill();

                                escribirTexto(
                                                contenido,
                                                fuenteNegrita,
                                                23,
                                                45,
                                                790,
                                                "RESCATE VIAL CR",
                                                true);

                                escribirTexto(
                                                contenido,
                                                fuenteNormal,
                                                12,
                                                45,
                                                765,
                                                "COMPROBANTE DE FACTURACION",
                                                true);

                                /*
                                 * NUMERO DE FACTURA
                                 */

                                escribirTexto(
                                                contenido,
                                                fuenteNegrita,
                                                17,
                                                45,
                                                695,
                                                "FACTURA No. "
                                                                + factura.getIdFactura(),
                                                false);

                                /*
                                 * INFORMACION GENERAL
                                 */

                                linea(
                                                contenido,
                                                45,
                                                675,
                                                550,
                                                675);

                                escribirTexto(
                                                contenido,
                                                fuenteNegrita,
                                                12,
                                                45,
                                                650,
                                                "INFORMACION DE LA FACTURA",
                                                false);

                                // FORMATEAR FECHA
                                String fecha = factura.getFechaFactura() != null
                                                ? factura.getFechaFactura().format(
                                                                DateTimeFormatter.ofPattern(
                                                                                "dd/MM/yyyy"))
                                                : "No registrada";

                                // FECHA
                                escribirTexto(
                                                contenido,
                                                fuenteNormal,
                                                11,
                                                45,
                                                625,
                                                "Fecha: " + fecha,
                                                false);

                                // HORA
                                escribirTexto(
                                                contenido,
                                                fuenteNormal,
                                                11,
                                                45,
                                                605,
                                                "Hora: "
                                                                + valorTexto(
                                                                                factura.getHoraFactura()),
                                                false);

                                // VENTA ASOCIADA
                                escribirTexto(
                                                contenido,
                                                fuenteNormal,
                                                11,
                                                45,
                                                585,
                                                "Venta asociada: "
                                                                + factura.getIdVenta(),
                                                false);

                                // ESTADO
                                escribirTexto(
                                                contenido,
                                                fuenteNormal,
                                                11,
                                                45,
                                                565,
                                                "Estado ID: "
                                                                + factura.getIdEstado(),
                                                false);

                                /*
                                 * DETALLE DE IMPORTES
                                 */

                                linea(
                                                contenido,
                                                45,
                                                540,
                                                550,
                                                540);

                                escribirTexto(
                                                contenido,
                                                fuenteNegrita,
                                                12,
                                                45,
                                                515,
                                                "DETALLE DE FACTURACION",
                                                false);

                                // SUBTOTAL
                                escribirTexto(
                                                contenido,
                                                fuenteNormal,
                                                11,
                                                45,
                                                485,
                                                "Subtotal:",
                                                false);

                                escribirTexto(
                                                contenido,
                                                fuenteNormal,
                                                11,
                                                390,
                                                485,
                                                formatoMonto(
                                                                factura.getSubtotal()),
                                                false);

                                // DEDUCCIONES
                                escribirTexto(
                                                contenido,
                                                fuenteNormal,
                                                11,
                                                45,
                                                455,
                                                "Total deducciones:",
                                                false);

                                escribirTexto(
                                                contenido,
                                                fuenteNormal,
                                                11,
                                                390,
                                                455,
                                                formatoMonto(
                                                                factura.getTotalDeducciones()),
                                                false);

                                // IMPUESTOS
                                escribirTexto(
                                                contenido,
                                                fuenteNormal,
                                                11,
                                                45,
                                                425,
                                                "Total impuestos:",
                                                false);

                                escribirTexto(
                                                contenido,
                                                fuenteNormal,
                                                11,
                                                390,
                                                425,
                                                formatoMonto(
                                                                factura.getTotalImpuestos()),
                                                false);

                                /*
                                 * TOTAL DE LA FACTURA
                                 */

                                // COLOR GRIS CLARO
                                contenido.setNonStrokingColor(
                                                new Color(244, 247, 250));

                                contenido.addRect(
                                                45,
                                                355,
                                                505,
                                                48);

                                contenido.fill();

                                escribirTexto(
                                                contenido,
                                                fuenteNegrita,
                                                14,
                                                60,
                                                373,
                                                "TOTAL FACTURA:",
                                                false);

                                escribirTexto(
                                                contenido,
                                                fuenteNegrita,
                                                14,
                                                365,
                                                373,
                                                formatoMonto(
                                                                factura.getTotalFactura()),
                                                false);

                                /*
                                 * PIE DEL DOCUMENTO
                                 */

                                linea(
                                                contenido,
                                                45,
                                                110,
                                                550,
                                                110);

                                escribirTexto(
                                                contenido,
                                                fuenteNormal,
                                                10,
                                                45,
                                                90,
                                                "Documento generado por RESCATE VIAL CR",
                                                false);

                                escribirTexto(
                                                contenido,
                                                fuenteNormal,
                                                9,
                                                45,
                                                73,
                                                "Informacion obtenida del sistema de facturacion.",
                                                false);
                        }

                        /*
                         * GUARDAR PDF EN MEMORIA
                         */

                        System.out.println(
                                        "PDF SERVICE - GUARDANDO DOCUMENTO");

                        documento.save(salida);

                        byte[] pdf = salida.toByteArray();

                        System.out.println(
                                        "PDF SERVICE - DOCUMENTO GENERADO CORRECTAMENTE");

                        System.out.println(
                                        "PDF SERVICE - TAMANO DEL ARCHIVO: "
                                                        + pdf.length
                                                        + " BYTES");

                        return pdf;

                } catch (IOException e) {

                        System.err.println(
                                        "PDF SERVICE - ERROR AL CREAR EL DOCUMENTO: "
                                                        + e.getMessage());

                        e.printStackTrace();

                        throw e;
                }
        }

        /*
         * METODO PARA ESCRIBIR TEXTO EN EL PDF
         */

        private void escribirTexto(
                        PDPageContentStream contenido,
                        PDType1Font fuente,
                        int tamano,
                        float x,
                        float y,
                        String texto,
                        boolean blanco) throws IOException {

                // CONFIGURAR COLOR DEL TEXTO
                if (blanco) {

                        contenido.setNonStrokingColor(
                                        Color.WHITE);

                } else {

                        contenido.setNonStrokingColor(
                                        new Color(11, 31, 51));
                }

                // ESCRIBIR TEXTO
                contenido.beginText();

                contenido.setFont(
                                fuente,
                                tamano);

                contenido.newLineAtOffset(
                                x,
                                y);

                contenido.showText(texto);

                contenido.endText();
        }

        /*
         * METODO PARA DIBUJAR LINEAS
         */

        private void linea(
                        PDPageContentStream contenido,
                        float x1,
                        float y1,
                        float x2,
                        float y2) throws IOException {

                // COLOR GRIS PARA LINEAS
                contenido.setStrokingColor(
                                new Color(220, 225, 230));

                contenido.setLineWidth(1);

                contenido.moveTo(
                                x1,
                                y1);

                contenido.lineTo(
                                x2,
                                y2);

                contenido.stroke();
        }

        /*
         * METODO PARA FORMATEAR MONTOS
         */

        private String formatoMonto(BigDecimal monto) {

                if (monto == null) {
                        monto = BigDecimal.ZERO;
                }

                return "CRC " + String.format(
                                Locale.US,
                                "%,.2f",
                                monto);
        }

        /*
         * METODO PARA VALIDAR TEXTOS NULOS
         */

        private String valorTexto(String valor) {

                if (valor == null || valor.isBlank()) {
                        return "No registrada";
                }

                return valor;
        }
}
