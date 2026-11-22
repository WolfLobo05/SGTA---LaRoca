package com.laroca.reporte;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Exporta Estadísticas Generales a PDF utilizando la misma API de OpenPDF
 * y el mismo enfoque de dibujo directo que ExportadorPdf del Inventario.
 *
 * No requiere cambios en pom.xml ni agrega dependencias.
 */
public final class ExportadorEstadisticasPdf {

    private static final float MARGEN = 36f;
    private static final float ALTO_LINEA = 18f;

    private static final Color TEXTO = new Color(0x22, 0x22, 0x22);
    private static final Color TEXTO_SUAVE = new Color(0x5c, 0x5c, 0x5c);
    private static final Color ROJO = new Color(0xc0, 0x39, 0x2b);
    private static final Color BORDE = new Color(0xb5, 0xb5, 0xb5);
    private static final Color FONDO = new Color(0xf2, 0xf2, 0xf2);

    private final BaseFont normal;
    private final BaseFont negrita;

    private PdfContentByte cb;
    private Document documento;
    private float anchoPagina;
    private float altoPagina;
    private float y;

    private ExportadorEstadisticasPdf() throws IOException {
        normal = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        negrita = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
    }

    public static void exportar(DatosReporteEstadisticas datos, File archivo) throws IOException {
        try (OutputStream salida = new FileOutputStream(archivo)) {
            new ExportadorEstadisticasPdf().escribir(datos, salida);
        }
    }

    private void escribir(DatosReporteEstadisticas datos, OutputStream salida) throws IOException {
        Rectangle hoja = PageSize.LETTER;
        anchoPagina = hoja.getWidth();
        altoPagina = hoja.getHeight();

        documento = new Document(hoja, MARGEN, MARGEN, MARGEN, MARGEN);

        try {
            PdfWriter escritor = PdfWriter.getInstance(documento, salida);
            documento.addTitle("Estadísticas Generales");
            documento.addAuthor("SGTA - La Roca");
            documento.addCreator("SGTA - La Roca");
            documento.open();

            cb = escritor.getDirectContent();
            y = altoPagina - MARGEN;

            dibujarEncabezado(datos);
            dibujarSeccion("RESUMEN GENERAL", datos.indicadores());

            for (DatosReporteEstadisticas.Seccion seccion : datos.secciones()) {
                dibujarSeccion(seccion.titulo().toUpperCase(), seccion.datos());
            }

            documento.close();

        } catch (DocumentException e) {
            if (documento.isOpen()) {
                documento.close();
            }
            throw new IOException("No se pudo generar el PDF: " + e.getMessage(), e);
        }
    }

    private void dibujarEncabezado(DatosReporteEstadisticas datos) {
        texto("ABARROTES LA ROCA", negrita, 10, TEXTO_SUAVE, MARGEN, y, false);
        y -= 22;
        texto("REPORTE DE ESTADÍSTICAS GENERALES", negrita, 17, TEXTO, MARGEN, y, false);
        y -= 20;

        String periodo = "Periodo: " + datos.desde() + " al " + datos.hasta();
        texto(periodo, normal, 9, TEXTO_SUAVE, MARGEN, y, false);
        y -= 14;
        texto("Categoría: " + datos.categoria(), normal, 9, TEXTO_SUAVE, MARGEN, y, false);
        y -= 14;
        texto("Generado: " + datos.generado(), normal, 9, TEXTO_SUAVE, MARGEN, y, false);
        y -= 18;
        linea(MARGEN, y, anchoPagina - 2 * MARGEN, BORDE);
        y -= 18;
    }

    private void dibujarSeccion(String titulo, List<DatosReporteEstadisticas.Dato> datos) {
        comprobarEspacio(65);
        rectangulo(MARGEN, y - 5, anchoPagina - 2 * MARGEN, 20, FONDO);
        texto(titulo, negrita, 10, ROJO, MARGEN + 7, y, false);
        y -= 27;

        float xNombre = MARGEN + 7;
        float xValor = MARGEN + 290;
        float xDetalle = MARGEN + 390;

        texto("CONCEPTO", negrita, 8, TEXTO_SUAVE, xNombre, y, false);
        texto("RESULTADO", negrita, 8, TEXTO_SUAVE, xValor, y, false);
        texto("DETALLE", negrita, 8, TEXTO_SUAVE, xDetalle, y, false);
        y -= 8;
        linea(MARGEN, y, anchoPagina - 2 * MARGEN, BORDE);
        y -= 15;

        if (datos == null || datos.isEmpty()) {
            comprobarEspacio(ALTO_LINEA);
            texto("Sin registros para el periodo seleccionado.", normal, 9, TEXTO_SUAVE,
                    xNombre, y, false);
            y -= ALTO_LINEA;
        } else {
            for (DatosReporteEstadisticas.Dato dato : datos) {
                comprobarEspacio(ALTO_LINEA + 5);

                texto(ajustar(dato.nombre(), normal, 9, 270), normal, 9, TEXTO,
                        xNombre, y, false);
                texto(ajustar(dato.valor(), negrita, 9, 90), negrita, 9, TEXTO,
                        xValor, y, false);
                texto(ajustar(dato.detalle(), normal, 8, 145), normal, 8, TEXTO_SUAVE,
                        xDetalle, y, false);

                y -= ALTO_LINEA;
                linea(MARGEN, y + 5, anchoPagina - 2 * MARGEN, new Color(0xe3, 0xe3, 0xe3));
            }
        }

        y -= 15;
    }

    private void comprobarEspacio(float requerido) {
        if (y - requerido < MARGEN + 20) {
            documento.newPage();
            y = altoPagina - MARGEN;
            texto("REPORTE DE ESTADÍSTICAS GENERALES (CONTINUACIÓN)",
                    negrita, 11, TEXTO, MARGEN, y, false);
            y -= 24;
        }
    }

    private void texto(String valor, BaseFont fuente, float tamano, Color color,
            float x, float yBase, boolean derecha) {
        if (valor == null || valor.isEmpty()) {
            return;
        }

        float ancho = fuente.getWidthPoint(valor, tamano);
        float posicionX = derecha ? x - ancho : x;

        cb.beginText();
        cb.setFontAndSize(fuente, tamano);
        cb.setColorFill(color);
        cb.setTextMatrix(posicionX, yBase);
        cb.showText(valor);
        cb.endText();
    }

    private static String ajustar(String valor, BaseFont fuente, float tamano, float maximo) {
        if (valor == null) {
            return "";
        }
        if (fuente.getWidthPoint(valor, tamano) <= maximo) {
            return valor;
        }

        String recorte = valor;
        while (!recorte.isEmpty()
                && fuente.getWidthPoint(recorte + "...", tamano) > maximo) {
            recorte = recorte.substring(0, recorte.length() - 1);
        }
        return recorte.stripTrailing() + "...";
    }

    private void linea(float x, float yLinea, float ancho, Color color) {
        cb.saveState();
        cb.setColorStroke(color);
        cb.setLineWidth(0.6f);
        cb.moveTo(x, yLinea);
        cb.lineTo(x + ancho, yLinea);
        cb.stroke();
        cb.restoreState();
    }

    private void rectangulo(float x, float ySuperior, float ancho, float alto, Color color) {
        cb.saveState();
        cb.setColorFill(color);
        cb.rectangle(x, ySuperior - alto, ancho, alto);
        cb.fill();
        cb.restoreState();
    }
}
