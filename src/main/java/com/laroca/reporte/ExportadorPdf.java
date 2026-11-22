package com.laroca.reporte;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import com.laroca.controlador.Formato;
import com.laroca.modelo.FilaInventario;
import com.laroca.reporte.DisenoReporte.Columna;
import com.laroca.reporte.DisenoReporte.Pagina;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;

// Exporta el Reporte de Inventario a PDF (OpenPDF) con el mismo diseño
// y las mismas páginas que la pantalla: carta horizontal, márgenes de
// 0.5", y todas las medidas de DisenoReporte escaladas de px a puntos.
public final class ExportadorPdf {

    private static final float MARGEN = 36f;

    // 816 px de pantalla = 720 pt imprimibles.
    private static final float ESCALA = (float) ((PageSize.LETTER.getHeight() - 2 * MARGEN)
            / DisenoReporte.ANCHO_PAGINA);

    private static final Color TEXTO = new Color(0x22, 0x22, 0x22);
    private static final Color TEXTO_SUAVE = new Color(0x5c, 0x5c, 0x5c);
    private static final Color ROJO = new Color(0xc0, 0x39, 0x2b);
    private static final Color BORDE = new Color(0xb5, 0xb5, 0xb5);
    private static final Color LINEA_FILA = new Color(0xe3, 0xe3, 0xe3);
    private static final Color VERDE_FONDO = new Color(0xdf, 0xf2, 0xe1);
    private static final Color VERDE_TEXTO = new Color(0x23, 0x6b, 0x33);
    private static final Color ROJO_FONDO = new Color(0xfb, 0xe1, 0xdf);
    private static final Color ROJO_TEXTO = new Color(0xa8, 0x2a, 0x1f);

    private final BaseFont normal;
    private final BaseFont negrita;

    private PdfContentByte cb;
    private float altoPaginaPt;

    private ExportadorPdf() throws IOException {
        normal = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        negrita = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
    }

    public static void exportar(ReporteInventario reporte, File archivo) throws IOException {
        try (OutputStream salida = new FileOutputStream(archivo)) {
            new ExportadorPdf().escribir(reporte, salida);
        }
    }

    private void escribir(ReporteInventario reporte, OutputStream salida) throws IOException {
        Rectangle hoja = PageSize.LETTER.rotate();
        altoPaginaPt = hoja.getHeight();

        Document documento = new Document(hoja, MARGEN, MARGEN, MARGEN, MARGEN);

        try {
            PdfWriter escritor = PdfWriter.getInstance(documento, salida);
            documento.addTitle("Reporte de Inventario");
            documento.addAuthor(reporte.getGeneradoPor() == null ? "" : reporte.getGeneradoPor());
            documento.addCreator("SGTA - La Roca");
            documento.open();

            cb = escritor.getDirectContent();

            // Mismo alto lógico que en pantalla: 540 pt / 0.882 = 612 px.
            double altoLogico = (altoPaginaPt - 2 * MARGEN) / ESCALA;
            List<Pagina> plan = DisenoReporte.paginar(reporte.getFilas().size(), altoLogico);

            for (Pagina pagina : plan) {
                if (pagina.numero() > 1) {
                    documento.newPage();
                }
                dibujarPagina(reporte, pagina, plan.size(), altoLogico);
            }

            documento.close();

        } catch (DocumentException e) {
            throw new IOException("No se pudo generar el PDF: " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------
    // Página
    // ------------------------------------------------------------------

    private void dibujarPagina(ReporteInventario reporte, Pagina pagina, int totalPaginas,
            double altoLogico) {

        double x0 = DisenoReporte.MARGEN_LATERAL;
        double ancho = DisenoReporte.ANCHO_CONTENIDO;
        double y = DisenoReporte.MARGEN_SUPERIOR;

        if (pagina.primera()) {
            dibujarEncabezado(reporte, x0, y, ancho);
            y += DisenoReporte.ENCABEZADO_ALTO;
        } else {
            double base = y + DisenoReporte.ENCABEZADO_CONTINUACION_ALTO - 6;
            texto(TextosReporte.TITULO + " (continuación)", negrita, 12, TEXTO, x0, base, Alineacion.IZQUIERDA);
            texto(TextosReporte.periodo(reporte), normal, 10, TEXTO_SUAVE, x0 + ancho, base, Alineacion.DERECHA);
            y += DisenoReporte.ENCABEZADO_CONTINUACION_ALTO;
        }

        // Regla bajo el encabezado.
        rectanguloRelleno(x0, y, ancho, DisenoReporte.REGLA_ALTO, TEXTO);
        y += DisenoReporte.REGLA_ALTO + DisenoReporte.REGLA_MARGEN;

        if (pagina.primera()) {
            dibujarTarjetas(reporte, x0, y, ancho);
            y += DisenoReporte.TARJETAS_ALTO + DisenoReporte.TARJETAS_MARGEN;
        }

        // Encabezado de la tabla.
        double x = x0;
        double base = centroVertical(y, DisenoReporte.TABLA_ENCABEZADO_ALTO, 10);
        for (Columna columna : Columna.values()) {
            textoCelda(columna.titulo, negrita, 10, TEXTO, x, columna, base);
            x += columna.ancho;
        }
        y += DisenoReporte.TABLA_ENCABEZADO_ALTO;
        linea(x0, y, ancho, 1, new Color(0x8c, 0x8c, 0x8c));

        List<FilaInventario> filas = reporte.getFilas();

        if (filas.isEmpty()) {
            texto(TextosReporte.SIN_DATOS, normal, 10.5, TEXTO_SUAVE, x0 + 6,
                    centroVertical(y, DisenoReporte.FILA_ALTO, 10.5), Alineacion.IZQUIERDA);
            y += DisenoReporte.FILA_ALTO;
            linea(x0, y, ancho, 1, LINEA_FILA);
        }

        for (int i = pagina.desde(); i < pagina.hasta(); i++) {
            dibujarFila(filas.get(i), x0, y);
            y += DisenoReporte.FILA_ALTO;
            linea(x0, y, ancho, 1, LINEA_FILA);
        }

        if (pagina.totales()) {
            dibujarTotales(reporte, x0, y, ancho);
        }

        dibujarPie(reporte, pagina.numero(), totalPaginas, x0, ancho, altoLogico);
    }

    private void dibujarEncabezado(ReporteInventario reporte, double x0, double y, double ancho) {
        double fondo = y + DisenoReporte.ENCABEZADO_ALTO - 6;

        texto("Abarrotes", normal, 10, TEXTO_SUAVE, x0, fondo - 22, Alineacion.IZQUIERDA);
        texto(TextosReporte.TITULO, negrita, 17, TEXTO, x0, fondo - 2, Alineacion.IZQUIERDA);

        double base = y + 10;
        for (String linea : TextosReporte.metadatos(reporte)) {
            texto(linea, normal, 10, TEXTO_SUAVE, x0 + ancho, base, Alineacion.DERECHA);
            base += 14;
        }
    }

    private void dibujarTarjetas(ReporteInventario reporte, double x0, double y, double ancho) {
        double espacio = DisenoReporte.TARJETAS_ESPACIO;
        double anchoTarjeta = (ancho - 3 * espacio) / 4;

        String[] titulos = {
            "Productos", "Unidades en existencia", "Valor del inventario", "Productos a reordenar"
        };
        String[] valores = {
            Formato.entero(reporte.getTotalProductos()),
            Formato.entero(reporte.getTotalStock()),
            Formato.dinero(reporte.getTotalValor()),
            Formato.entero(reporte.getProductosReordenar())
        };
        boolean[] alertas = {
            false,
            reporte.getTotalStock() < 0,
            reporte.getTotalValor().signum() < 0,
            reporte.getProductosReordenar() > 0
        };

        for (int i = 0; i < 4; i++) {
            double x = x0 + i * (anchoTarjeta + espacio);
            rectanguloBorde(x, y, anchoTarjeta, DisenoReporte.TARJETAS_ALTO, BORDE);
            texto(titulos[i], normal, 9.5, TEXTO_SUAVE, x + 10, y + 16, Alineacion.IZQUIERDA);
            texto(valores[i], negrita, 16, alertas[i] ? ROJO : TEXTO, x + 10, y + 38, Alineacion.IZQUIERDA);
        }
    }

    private void dibujarFila(FilaInventario fila, double x0, double y) {
        double x = x0;
        double base = centroVertical(y, DisenoReporte.FILA_ALTO, 10.5);

        for (Columna columna : Columna.values()) {
            if (columna == Columna.REORDEN) {
                dibujarPastilla(fila.isReordenar(), x + 6, y);
            } else {
                boolean stock = columna == Columna.STOCK;
                boolean negativo = (stock && fila.isStockNegativo())
                        || (columna == Columna.VALOR && fila.getValor().signum() < 0);

                textoCelda(TextosReporte.valor(fila, columna), stock ? negrita : normal, 10.5,
                        negativo ? ROJO : TEXTO, x, columna, base);
            }
            x += columna.ancho;
        }
    }

    private void dibujarPastilla(boolean reordenar, double x, double yFila) {
        String texto = TextosReporte.pastilla(reordenar);
        double tamano = 9;
        double anchoTexto = negrita.getWidthPoint(texto, (float) tamano);
        double ancho = anchoTexto + 14;
        double alto = 15;
        double y = yFila + (DisenoReporte.FILA_ALTO - alto) / 2;

        cb.saveState();
        cb.setColorFill(reordenar ? ROJO_FONDO : VERDE_FONDO);
        cb.roundRectangle(px(x), py(y + alto), (float) (ancho * ESCALA), (float) (alto * ESCALA),
                (float) (alto * ESCALA / 2));
        cb.fill();
        cb.restoreState();

        texto(texto, negrita, tamano, reordenar ? ROJO_TEXTO : VERDE_TEXTO, x + 7,
                centroVertical(y, alto, tamano), Alineacion.IZQUIERDA);
    }

    private void dibujarTotales(ReporteInventario reporte, double x0, double y, double ancho) {
        rectanguloRelleno(x0, y, ancho, 1.5, new Color(0x33, 0x33, 0x33));

        double x = x0;
        double base = centroVertical(y, DisenoReporte.TOTALES_ALTO, 10.5);

        for (Columna columna : Columna.values()) {
            boolean negativo = (columna == Columna.STOCK && reporte.getTotalStock() < 0)
                    || (columna == Columna.VALOR && reporte.getTotalValor().signum() < 0);

            textoCelda(TextosReporte.total(reporte, columna), negrita, 10.5,
                    negativo ? ROJO : TEXTO, x, columna, base);
            x += columna.ancho;
        }
    }

    private void dibujarPie(ReporteInventario reporte, int numero, int total, double x0,
            double ancho, double altoLogico) {

        List<String> notas = TextosReporte.notas(reporte);
        double fondo = altoLogico - DisenoReporte.MARGEN_INFERIOR - 3;
        double base = fondo - (notas.size() - 1) * 12;

        for (String nota : notas) {
            boolean aviso = nota.startsWith(TextosReporte.PREFIJO_AVISO);
            texto(nota, aviso ? negrita : normal, 9, aviso ? ROJO : TEXTO_SUAVE, x0, base,
                    Alineacion.IZQUIERDA);
            base += 12;
        }

        texto(TextosReporte.pagina(numero, total), normal, 9, TEXTO_SUAVE, x0 + ancho, fondo,
                Alineacion.DERECHA);
    }

    // ------------------------------------------------------------------
    // Primitivas (coordenadas en px de pantalla desde la esquina
    // superior izquierda de la página; se convierten a puntos PDF)
    // ------------------------------------------------------------------

    private enum Alineacion { IZQUIERDA, DERECHA }

    private float px(double x) {
        return (float) (MARGEN + x * ESCALA);
    }

    private float py(double y) {
        return (float) (altoPaginaPt - MARGEN - y * ESCALA);
    }

    // Línea base que centra verticalmente un texto en una franja.
    private static double centroVertical(double y, double alto, double tamano) {
        return y + alto / 2 + tamano * 0.36;
    }

    private void textoCelda(String texto, BaseFont fuente, double tamano, Color color,
            double xColumna, Columna columna, double base) {

        double relleno = 6;
        double maximo = columna.ancho - 2 * relleno;
        String ajustado = ajustar(texto, fuente, tamano, maximo);

        if (columna.derecha) {
            texto(ajustado, fuente, tamano, color, xColumna + columna.ancho - relleno, base,
                    Alineacion.DERECHA);
        } else {
            texto(ajustado, fuente, tamano, color, xColumna + relleno, base, Alineacion.IZQUIERDA);
        }
    }

    private void texto(String texto, BaseFont fuente, double tamanoPx, Color color,
            double x, double base, Alineacion alineacion) {

        if (texto == null || texto.isEmpty()) {
            return;
        }

        float tamano = (float) (tamanoPx * ESCALA);
        float ancho = fuente.getWidthPoint(texto, tamano);
        float xPdf = alineacion == Alineacion.DERECHA ? px(x) - ancho : px(x);

        cb.beginText();
        cb.setFontAndSize(fuente, tamano);
        cb.setColorFill(color);
        cb.setTextMatrix(xPdf, py(base));
        cb.showText(texto);
        cb.endText();
    }

    // Recorta con "…" lo que no cabe en el ancho (en px).
    private static String ajustar(String texto, BaseFont fuente, double tamanoPx, double maximoPx) {
        if (texto == null) {
            return "";
        }

        float tamano = (float) (tamanoPx * ESCALA);
        float maximo = (float) (maximoPx * ESCALA);

        if (fuente.getWidthPoint(texto, tamano) <= maximo) {
            return texto;
        }

        String recorte = texto;
        while (!recorte.isEmpty() && fuente.getWidthPoint(recorte + "…", tamano) > maximo) {
            recorte = recorte.substring(0, recorte.length() - 1);
        }
        return recorte.stripTrailing() + "…";
    }

    private void linea(double x, double y, double ancho, double grosor, Color color) {
        cb.saveState();
        cb.setColorStroke(color);
        cb.setLineWidth((float) (grosor * ESCALA));
        cb.moveTo(px(x), py(y));
        cb.lineTo(px(x + ancho), py(y));
        cb.stroke();
        cb.restoreState();
    }

    private void rectanguloRelleno(double x, double y, double ancho, double alto, Color color) {
        cb.saveState();
        cb.setColorFill(color);
        cb.rectangle(px(x), py(y + alto), (float) (ancho * ESCALA), (float) (alto * ESCALA));
        cb.fill();
        cb.restoreState();
    }

    private void rectanguloBorde(double x, double y, double ancho, double alto, Color color) {
        cb.saveState();
        cb.setColorStroke(color);
        cb.setLineWidth(0.75f);
        cb.rectangle(px(x), py(y + alto), (float) (ancho * ESCALA), (float) (alto * ESCALA));
        cb.stroke();
        cb.restoreState();
    }
}
