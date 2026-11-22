package com.laroca.reporte;

import java.util.ArrayList;
import java.util.List;

import com.laroca.controlador.Formato;
import com.laroca.modelo.FilaInventario;
import com.laroca.reporte.DisenoReporte.Columna;

// Textos del Reporte de Inventario, iguales en pantalla, impresión, PDF y Excel.
public final class TextosReporte {

    public static final String TITULO = "\"LA ROCA\" — Reporte de Inventario";

    public static final String SIN_DATOS =
            "No hay productos activos con los filtros seleccionados.";

    public static final String PREFIJO_AVISO = "Atención:";

    private TextosReporte() {

    }

    public static String periodo(ReporteInventario reporte) {
        return "Periodo: " + Formato.fecha(reporte.getDesde())
                + " – " + Formato.fecha(reporte.getHasta());
    }

    public static String generado(ReporteInventario reporte) {
        String texto = "Generado: " + Formato.fechaHora(reporte.getGeneradoEn());
        String autor = reporte.getGeneradoPor();

        return autor == null || autor.isBlank() ? texto : texto + " por " + autor;
    }

    // Líneas de la esquina superior derecha.
    public static List<String> metadatos(ReporteInventario reporte) {
        List<String> lineas = new ArrayList<>();
        lineas.add(periodo(reporte));
        lineas.add("Categoría: " + reporte.getCategoria());

        if (reporte.getFiltroReorden() != null) {
            lineas.add("Reordenar: " + reporte.getFiltroReorden());
        }

        lineas.add(generado(reporte));
        return lineas;
    }

    // Notas del pie de cada página (hasta 3 líneas).
    public static List<String> notas(ReporteInventario reporte) {
        List<String> notas = new ArrayList<>();
        notas.add(ReporteInventario.NOTA_FORMULA);
        notas.add(ReporteInventario.NOTA_STOCK);

        int negativos = reporte.getProductosNegativos();

        if (negativos > 0) {
            notas.add(PREFIJO_AVISO + " " + negativos
                    + (negativos == 1 ? " producto con stock negativo" : " productos con stock negativo")
                    + " (en rojo): revisar sus existencias.");
        }

        return notas;
    }

    public static String pagina(int numero, int total) {
        return "Página " + numero + " de " + total;
    }

    public static String pastilla(boolean reordenar) {
        return reordenar ? "Reordenar" : "No";
    }

    public static String valor(FilaInventario fila, Columna columna) {
        return switch (columna) {
            case CODIGO -> texto(fila.getCodigo());
            case PRODUCTO -> texto(fila.getProducto());
            case CATEGORIA -> texto(fila.getCategoria());
            case UNIDAD -> texto(fila.getUnidad());
            case ENTRADAS -> Formato.entero(fila.getEntradas());
            case SALIDAS -> Formato.entero(fila.getSalidas());
            case STOCK -> Formato.entero(fila.getStock());
            case PRECIO -> fila.getPrecio() == null ? "—" : Formato.dinero(fila.getPrecio());
            case VALOR -> Formato.dinero(fila.getValor());
            case REORDEN -> pastilla(fila.isReordenar());
        };
    }

    public static String total(ReporteInventario reporte, Columna columna) {
        return switch (columna) {
            case CODIGO -> "Totales";
            case ENTRADAS -> Formato.entero(reporte.getTotalEntradas());
            case SALIDAS -> Formato.entero(reporte.getTotalSalidas());
            case STOCK -> Formato.entero(reporte.getTotalStock());
            case VALOR -> Formato.dinero(reporte.getTotalValor());
            default -> "";
        };
    }

    // Línea de la barra de estado.
    public static String resumen(ReporteInventario reporte) {
        int productos = reporte.getTotalProductos();
        String texto = "Reporte generado · " + productos
                + (productos == 1 ? " producto" : " productos")
                + " · " + reporte.getProductosReordenar() + " por reordenar";

        if (reporte.getProductosNegativos() > 0) {
            texto += " · " + reporte.getProductosNegativos() + " con stock negativo";
        }

        return texto;
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor;
    }
}
