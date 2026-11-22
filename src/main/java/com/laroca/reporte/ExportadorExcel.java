package com.laroca.reporte;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import com.laroca.modelo.FilaInventario;
import com.laroca.reporte.DisenoReporte.Columna;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

// Exporta el Reporte de Inventario a un .xlsx real (Apache POI):
// cantidades con formato numérico, precio y valor con formato de moneda
// MX ($1,234.00), totales y tarjetas con fórmulas (con su valor ya
// calculado), encabezado fijo y filtro automático.
public final class ExportadorExcel {

    // POI usa log4j-api; sin una implementación imprime "ERROR Log4j2 could
    // not find a logging implementation". Con el registrador simple de
    // log4j-api (nivel ERROR) no aparece ese aviso y no se agrega otra librería.
    static {
        if (System.getProperty("log4j2.loggerContextFactory") == null) {
            System.setProperty("log4j2.loggerContextFactory",
                    "org.apache.logging.log4j.simple.SimpleLoggerContextFactory");
        }
    }

    private static final String FORMATO_ENTERO = "#,##0;[Red]-#,##0";
    private static final String FORMATO_DINERO = "\"$\"#,##0.00;[Red]-\"$\"#,##0.00";

    private final XSSFWorkbook libro = new XSSFWorkbook();

    private final CellStyle negocio;
    private final CellStyle titulo;
    private final CellStyle meta;
    private final CellStyle tarjetaTitulo;
    private final CellStyle tarjetaEntero;
    private final CellStyle tarjetaDinero;
    private final CellStyle tarjetaAlerta;
    private final CellStyle encabezado;
    private final CellStyle encabezadoDerecha;
    private final CellStyle textoCelda;
    private final CellStyle enteroCelda;
    private final CellStyle stockCelda;
    private final CellStyle dineroCelda;
    private final CellStyle pastillaOk;
    private final CellStyle pastillaAlerta;
    private final CellStyle totalTexto;
    private final CellStyle totalEntero;
    private final CellStyle totalDinero;
    private final CellStyle nota;
    private final CellStyle notaAlerta;

    private ExportadorExcel() {
        DataFormat formatos = libro.createDataFormat();
        short entero = formatos.getFormat(FORMATO_ENTERO);
        short dinero = formatos.getFormat(FORMATO_DINERO);

        negocio = estilo(fuente(9, false, 0x5c5c5c), null);
        titulo = estilo(fuente(15, true, 0x222222), null);
        meta = estilo(fuente(9, false, 0x5c5c5c), null);

        tarjetaTitulo = estilo(fuente(9, false, 0x5c5c5c), null);
        bordeCaja(tarjetaTitulo, true);
        tarjetaEntero = estilo(fuente(14, true, 0x222222), null);
        tarjetaEntero.setDataFormat(entero);
        tarjetaEntero.setAlignment(HorizontalAlignment.LEFT);
        bordeCaja(tarjetaEntero, false);
        tarjetaDinero = estilo(fuente(14, true, 0x222222), null);
        tarjetaDinero.setDataFormat(dinero);
        tarjetaDinero.setAlignment(HorizontalAlignment.LEFT);
        bordeCaja(tarjetaDinero, false);
        tarjetaAlerta = estilo(fuente(14, true, 0xc0392b), null);
        tarjetaAlerta.setDataFormat(entero);
        tarjetaAlerta.setAlignment(HorizontalAlignment.LEFT);
        bordeCaja(tarjetaAlerta, false);

        encabezado = estilo(fuente(10, true, 0x333333), 0xdedede);
        encabezado.setBorderBottom(BorderStyle.THIN);
        encabezadoDerecha = estilo(fuente(10, true, 0x333333), 0xdedede);
        encabezadoDerecha.setBorderBottom(BorderStyle.THIN);
        encabezadoDerecha.setAlignment(HorizontalAlignment.RIGHT);

        textoCelda = estilo(fuente(10, false, 0x222222), null);
        lineaInferior(textoCelda);
        enteroCelda = estilo(fuente(10, false, 0x222222), null);
        enteroCelda.setDataFormat(entero);
        lineaInferior(enteroCelda);
        stockCelda = estilo(fuente(10, true, 0x222222), null);
        stockCelda.setDataFormat(entero);
        lineaInferior(stockCelda);
        dineroCelda = estilo(fuente(10, false, 0x222222), null);
        dineroCelda.setDataFormat(dinero);
        lineaInferior(dineroCelda);

        pastillaOk = estilo(fuente(9, true, 0x236b33), 0xdff2e1);
        pastillaOk.setAlignment(HorizontalAlignment.CENTER);
        lineaInferior(pastillaOk);
        pastillaAlerta = estilo(fuente(9, true, 0xa82a1f), 0xfbe1df);
        pastillaAlerta.setAlignment(HorizontalAlignment.CENTER);
        lineaInferior(pastillaAlerta);

        totalTexto = estilo(fuente(10, true, 0x222222), null);
        totalTexto.setBorderTop(BorderStyle.MEDIUM);
        totalEntero = estilo(fuente(10, true, 0x222222), null);
        totalEntero.setDataFormat(entero);
        totalEntero.setBorderTop(BorderStyle.MEDIUM);
        totalDinero = estilo(fuente(10, true, 0x222222), null);
        totalDinero.setDataFormat(dinero);
        totalDinero.setBorderTop(BorderStyle.MEDIUM);

        nota = estilo(fuente(8, false, 0x5c5c5c), null);
        notaAlerta = estilo(fuente(8, true, 0xc0392b), null);
    }

    public static void exportar(ReporteInventario reporte, File archivo) throws IOException {
        ExportadorExcel exportador = new ExportadorExcel();

        try (XSSFWorkbook libro = exportador.libro;
             OutputStream salida = new FileOutputStream(archivo)) {

            exportador.llenar(reporte);
            libro.write(salida);
        }
    }

    private void llenar(ReporteInventario reporte) {
        Sheet hoja = libro.createSheet("Inventario");
        hoja.setDisplayGridlines(false);

        int[] anchos = {16, 26, 20, 8, 10, 10, 9, 12, 14, 16};
        for (int i = 0; i < anchos.length; i++) {
            hoja.setColumnWidth(i, anchos[i] * 256);
        }

        int r = 0;

        // Encabezado.
        celdaTexto(hoja.createRow(r++), 0, "Abarrotes", negocio);
        Row filaTitulo = hoja.createRow(r++);
        filaTitulo.setHeightInPoints(22);
        celdaTexto(filaTitulo, 0, TextosReporte.TITULO, titulo);

        for (String linea : TextosReporte.metadatos(reporte)) {
            celdaTexto(hoja.createRow(r++), 0, linea, meta);
        }

        r++;

        // Tarjetas: títulos en una fila, valores en la siguiente.
        int filaTarjetaTitulos = r++;
        int filaTarjetaValores = r++;
        r++;

        // Tabla.
        int filaEncabezado = r++;
        Row encabezadoTabla = hoja.createRow(filaEncabezado);
        encabezadoTabla.setHeightInPoints(18);

        Columna[] columnas = Columna.values();
        for (int c = 0; c < columnas.length; c++) {
            celdaTexto(encabezadoTabla, c, columnas[c].titulo,
                    columnas[c].derecha ? encabezadoDerecha : encabezado);
        }

        List<FilaInventario> filas = reporte.getFilas();
        int primeraFila = r;

        if (filas.isEmpty()) {
            celdaTexto(hoja.createRow(r++), 0, TextosReporte.SIN_DATOS, textoCelda);
        }

        for (FilaInventario fila : filas) {
            Row row = hoja.createRow(r++);
            row.setHeightInPoints(16);

            // El código se guarda como texto para no perder ceros ni
            // convertirlo a notación científica.
            celdaTexto(row, 0, fila.getCodigo(), textoCelda);
            celdaTexto(row, 1, fila.getProducto(), textoCelda);
            celdaTexto(row, 2, fila.getCategoria(), textoCelda);
            celdaTexto(row, 3, fila.getUnidad(), textoCelda);
            celdaNumero(row, 4, fila.getEntradas(), enteroCelda);
            celdaNumero(row, 5, fila.getSalidas(), enteroCelda);
            celdaNumero(row, 6, fila.getStock(), stockCelda);

            if (fila.getPrecio() == null) {
                celdaTexto(row, 7, "", dineroCelda);
            } else {
                celdaNumero(row, 7, fila.getPrecio().doubleValue(), dineroCelda);
            }

            celdaNumero(row, 8, fila.getValor().doubleValue(), dineroCelda);
            celdaTexto(row, 9, TextosReporte.pastilla(fila.isReordenar()),
                    fila.isReordenar() ? pastillaAlerta : pastillaOk);
        }

        int ultimaFila = r - 1;
        boolean hayDatos = !filas.isEmpty();

        // Totales con fórmulas sobre la tabla.
        Row totales = hoja.createRow(r++);
        totales.setHeightInPoints(18);
        celdaTexto(totales, 0, "Totales", totalTexto);
        for (int c = 1; c < columnas.length; c++) {
            celdaTexto(totales, c, "", totalTexto);
        }
        sumaOValor(totales, 4, primeraFila, ultimaFila, hayDatos, reporte.getTotalEntradas(), totalEntero);
        sumaOValor(totales, 5, primeraFila, ultimaFila, hayDatos, reporte.getTotalSalidas(), totalEntero);
        sumaOValor(totales, 6, primeraFila, ultimaFila, hayDatos, reporte.getTotalStock(), totalEntero);
        sumaOValor(totales, 8, primeraFila, ultimaFila, hayDatos,
                reporte.getTotalValor().doubleValue(), totalDinero);

        // Tarjetas (fórmulas sobre la tabla, así cuadran siempre con ella).
        Row titulos = hoja.createRow(filaTarjetaTitulos);
        Row valores = hoja.createRow(filaTarjetaValores);
        valores.setHeightInPoints(22);

        String[] nombres = {
            "Productos", "Unidades en existencia", "Valor del inventario", "Productos a reordenar"
        };
        int[][] rangos = {{0, 1}, {2, 3}, {4, 6}, {7, 9}};

        for (int i = 0; i < 4; i++) {
            int desde = rangos[i][0];
            int hasta = rangos[i][1];

            for (int c = desde; c <= hasta; c++) {
                celdaTexto(titulos, c, c == desde ? nombres[i] : "", tarjetaTitulo);
            }
            hoja.addMergedRegion(new CellRangeAddress(filaTarjetaTitulos, filaTarjetaTitulos, desde, hasta));
            hoja.addMergedRegion(new CellRangeAddress(filaTarjetaValores, filaTarjetaValores, desde, hasta));
        }

        CellStyle estiloReorden = reporte.getProductosReordenar() > 0 ? tarjetaAlerta : tarjetaEntero;

        tarjeta(valores, 0, 1, hayDatos ? "COUNTA(" + rango(0, primeraFila, ultimaFila) + ")" : null,
                reporte.getTotalProductos(), tarjetaEntero);
        tarjeta(valores, 2, 3, hayDatos ? "SUM(" + rango(6, primeraFila, ultimaFila) + ")" : null,
                reporte.getTotalStock(), tarjetaEntero);
        tarjeta(valores, 4, 6, hayDatos ? "SUM(" + rango(8, primeraFila, ultimaFila) + ")" : null,
                reporte.getTotalValor().doubleValue(), tarjetaDinero);
        tarjeta(valores, 7, 9, hayDatos
                        ? "COUNTIF(" + rango(9, primeraFila, ultimaFila) + ",\"Reordenar\")" : null,
                reporte.getProductosReordenar(), estiloReorden);

        // Notas del pie.
        r++;
        for (String texto : TextosReporte.notas(reporte)) {
            celdaTexto(hoja.createRow(r++), 0, texto,
                    texto.startsWith(TextosReporte.PREFIJO_AVISO) ? notaAlerta : nota);
        }

        // Encabezado de la tabla siempre visible y filtro automático.
        hoja.createFreezePane(0, filaEncabezado + 1);
        if (hayDatos) {
            hoja.setAutoFilter(new CellRangeAddress(filaEncabezado, ultimaFila, 0, columnas.length - 1));
        }

        // Impresión: carta horizontal, una página de ancho, encabezado repetido.
        PrintSetup impresion = hoja.getPrintSetup();
        impresion.setLandscape(true);
        impresion.setPaperSize(PrintSetup.LETTER_PAPERSIZE);
        impresion.setFitWidth((short) 1);
        impresion.setFitHeight((short) 0);
        hoja.setFitToPage(true);
        hoja.setRepeatingRows(new CellRangeAddress(filaEncabezado, filaEncabezado, 0, columnas.length - 1));
        hoja.getFooter().setLeft("SGTA - La Roca · Reporte de Inventario");
        hoja.getFooter().setRight("Página &P de &N");

        // Guarda también el resultado de las fórmulas, para que se vean
        // aunque el programa que abra el archivo no recalcule.
        libro.getCreationHelper().createFormulaEvaluator().evaluateAll();
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private void tarjeta(Row fila, int desde, int hasta, String formula, double valor, CellStyle estilo) {
        Cell celda = fila.createCell(desde);
        celda.setCellStyle(estilo);

        if (formula == null) {
            celda.setCellValue(valor);
        } else {
            celda.setCellFormula(formula);
        }

        for (int c = desde + 1; c <= hasta; c++) {
            fila.createCell(c).setCellStyle(estilo);
        }
    }

    private void sumaOValor(Row fila, int columna, int primera, int ultima, boolean hayDatos,
            double valor, CellStyle estilo) {

        Cell celda = fila.createCell(columna);
        celda.setCellStyle(estilo);

        if (hayDatos) {
            celda.setCellFormula("SUM(" + rango(columna, primera, ultima) + ")");
        } else {
            celda.setCellValue(valor);
        }
    }

    private static String rango(int columna, int primera, int ultima) {
        String letra = CellReference.convertNumToColString(columna);
        return letra + (primera + 1) + ":" + letra + (ultima + 1);
    }

    private static void celdaTexto(Row fila, int columna, String texto, CellStyle estilo) {
        Cell celda = fila.createCell(columna);
        celda.setCellValue(texto == null ? "" : texto);
        celda.setCellStyle(estilo);
    }

    private static void celdaNumero(Row fila, int columna, double valor, CellStyle estilo) {
        Cell celda = fila.createCell(columna);
        celda.setCellValue(valor);
        celda.setCellStyle(estilo);
    }

    private XSSFFont fuente(int tamano, boolean negrita, int rgb) {
        XSSFFont fuente = libro.createFont();
        fuente.setFontName("Arial");
        fuente.setFontHeightInPoints((short) tamano);
        fuente.setBold(negrita);
        fuente.setColor(color(rgb));
        return fuente;
    }

    private CellStyle estilo(Font fuente, Integer fondo) {
        CellStyle estilo = libro.createCellStyle();
        estilo.setFont(fuente);
        estilo.setVerticalAlignment(VerticalAlignment.CENTER);

        if (fondo != null) {
            ((XSSFCellStyle) estilo).setFillForegroundColor(color(fondo));
            estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }

        return estilo;
    }

    private static void lineaInferior(CellStyle estilo) {
        estilo.setBorderBottom(BorderStyle.HAIR);
        estilo.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
    }

    private static void bordeCaja(CellStyle estilo, boolean arriba) {
        estilo.setBorderLeft(BorderStyle.THIN);
        estilo.setBorderRight(BorderStyle.THIN);
        estilo.setLeftBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        estilo.setRightBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());

        if (arriba) {
            estilo.setBorderTop(BorderStyle.THIN);
            estilo.setTopBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        } else {
            estilo.setBorderBottom(BorderStyle.THIN);
            estilo.setBottomBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        }
    }

    private static XSSFColor color(int rgb) {
        return new XSSFColor(new byte[] {
            (byte) (rgb >> 16), (byte) (rgb >> 8), (byte) rgb
        }, null);
    }
}
