package com.laroca.reporte;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** Exportador XLSX independiente para Estadísticas Generales. */
public final class ExportadorEstadisticasExcel {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ExportadorEstadisticasExcel() {
    }

    public static void exportar(DatosReporteEstadisticas datos, File archivo) throws IOException {
        try (XSSFWorkbook libro = new XSSFWorkbook();
             FileOutputStream salida = new FileOutputStream(archivo)) {

            Sheet hoja = libro.createSheet("Estadísticas");
            hoja.setDisplayGridlines(false);
            hoja.setColumnWidth(0, 38 * 256);
            hoja.setColumnWidth(1, 24 * 256);
            hoja.setColumnWidth(2, 42 * 256);

            Font fuenteTitulo = libro.createFont();
            fuenteTitulo.setFontName("Arial");
            fuenteTitulo.setFontHeightInPoints((short) 16);
            fuenteTitulo.setBold(true);

            Font fuenteSeccion = libro.createFont();
            fuenteSeccion.setFontName("Arial");
            fuenteSeccion.setFontHeightInPoints((short) 11);
            fuenteSeccion.setBold(true);
            fuenteSeccion.setColor(IndexedColors.DARK_RED.getIndex());

            Font fuenteNormal = libro.createFont();
            fuenteNormal.setFontName("Arial");
            fuenteNormal.setFontHeightInPoints((short) 10);

            CellStyle titulo = libro.createCellStyle();
            titulo.setFont(fuenteTitulo);
            titulo.setAlignment(HorizontalAlignment.CENTER);

            CellStyle seccion = libro.createCellStyle();
            seccion.setFont(fuenteSeccion);
            seccion.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            seccion.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            seccion.setBorderBottom(BorderStyle.THIN);

            CellStyle normal = libro.createCellStyle();
            normal.setFont(fuenteNormal);
            normal.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
            normal.setBorderBottom(BorderStyle.HAIR);

            CellStyle encabezadoTabla = libro.createCellStyle();
            encabezadoTabla.cloneStyleFrom(seccion);
            encabezadoTabla.setAlignment(HorizontalAlignment.CENTER);

            int fila = 0;
            Row marca = hoja.createRow(fila++);
            celda(marca, 0, "ABARROTES LA ROCA", normal);

            Row tituloRow = hoja.createRow(fila++);
            celda(tituloRow, 0, "REPORTE DE ESTADÍSTICAS GENERALES", titulo);
            hoja.addMergedRegion(new CellRangeAddress(1, 1, 0, 2));

            Row periodo = hoja.createRow(fila++);
            celda(periodo, 0, "Periodo: " + datos.desde().format(FECHA)
                    + " al " + datos.hasta().format(FECHA), normal);
            celda(periodo, 1, "Categoría: " + datos.categoria(), normal);
            celda(periodo, 2, "Generado: " + datos.generado().format(FECHA), normal);
            fila++;

            fila = escribirSeccion(hoja, fila, "RESUMEN GENERAL",
                    datos.indicadores(), seccion, encabezadoTabla, normal);

            for (DatosReporteEstadisticas.Seccion bloque : datos.secciones()) {
                fila = escribirSeccion(hoja, fila, bloque.titulo().toUpperCase(),
                        bloque.datos(), seccion, encabezadoTabla, normal);
            }

            hoja.createFreezePane(0, 5);
            hoja.setFitToPage(true);
            PrintSetup impresion = hoja.getPrintSetup();
            impresion.setLandscape(true);
            impresion.setPaperSize(PrintSetup.LETTER_PAPERSIZE);
            impresion.setFitWidth((short) 1);
            impresion.setFitHeight((short) 0);
            hoja.getFooter().setLeft("SGTA - La Roca · Estadísticas Generales");
            hoja.getFooter().setRight("Página &P de &N");

            for (int i = 0; i < 3; i++) {
                hoja.autoSizeColumn(i);
                int ancho = Math.min(hoja.getColumnWidth(i), 55 * 256);
                hoja.setColumnWidth(i, Math.max(ancho, 18 * 256));
            }

            libro.write(salida);
        }
    }

    private static int escribirSeccion(
            Sheet hoja, int fila, String titulo,
            java.util.List<DatosReporteEstadisticas.Dato> datos,
            CellStyle estiloSeccion, CellStyle estiloEncabezado, CellStyle estiloNormal) {

        Row encabezado = hoja.createRow(fila++);
        celda(encabezado, 0, titulo, estiloSeccion);
        hoja.addMergedRegion(new CellRangeAddress(encabezado.getRowNum(),
                encabezado.getRowNum(), 0, 2));

        Row nombres = hoja.createRow(fila++);
        celda(nombres, 0, "Concepto", estiloEncabezado);
        celda(nombres, 1, "Resultado", estiloEncabezado);
        celda(nombres, 2, "Detalle", estiloEncabezado);

        if (datos.isEmpty()) {
            Row vacio = hoja.createRow(fila++);
            celda(vacio, 0, "Sin registros para el periodo seleccionado.", estiloNormal);
            hoja.addMergedRegion(new CellRangeAddress(vacio.getRowNum(), vacio.getRowNum(), 0, 2));
        } else {
            for (DatosReporteEstadisticas.Dato dato : datos) {
                Row row = hoja.createRow(fila++);
                celda(row, 0, dato.nombre(), estiloNormal);
                celda(row, 1, dato.valor(), estiloNormal);
                celda(row, 2, dato.detalle(), estiloNormal);
            }
        }
        return fila + 1;
    }

    private static void celda(Row fila, int columna, String valor, CellStyle estilo) {
        Cell celda = fila.createCell(columna);
        celda.setCellValue(valor == null ? "" : valor);
        celda.setCellStyle(estilo);
    }
}
