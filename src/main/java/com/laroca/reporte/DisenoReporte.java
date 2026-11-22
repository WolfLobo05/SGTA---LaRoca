package com.laroca.reporte;

import java.util.ArrayList;
import java.util.List;

// Medidas del "papel" del Reporte de Inventario y reparto de filas por
// página. Las usan la pantalla, la impresión y el PDF, para que los tres
// tengan las mismas páginas.
//
// Las medidas son píxeles de pantalla. La hoja es carta horizontal con
// márgenes de 0.5": 720 x 540 pt imprimibles = 816 x 612 px a escala 0.882.
public final class DisenoReporte {

    public static final double ANCHO_PAGINA = 816;
    public static final double ALTO_PAGINA = 612;

    public static final double MARGEN_LATERAL = 30;
    public static final double MARGEN_SUPERIOR = 28;
    public static final double MARGEN_INFERIOR = 22;

    public static final double ANCHO_CONTENIDO = ANCHO_PAGINA - 2 * MARGEN_LATERAL;

    public static final double ENCABEZADO_ALTO = 66;
    public static final double ENCABEZADO_CONTINUACION_ALTO = 22;
    public static final double REGLA_ALTO = 2;
    public static final double REGLA_MARGEN = 12;
    public static final double TARJETAS_ALTO = 50;
    public static final double TARJETAS_MARGEN = 14;
    public static final double TARJETAS_ESPACIO = 10;
    public static final double TABLA_ENCABEZADO_ALTO = 26;
    public static final double FILA_ALTO = 25;
    public static final double TOTALES_ALTO = 28;
    public static final double PIE_MARGEN = 8;
    public static final double PIE_ALTO = 40;

    // Columnas de la tabla. Los anchos suman ANCHO_CONTENIDO (756).
    public enum Columna {
        CODIGO("Código", 94, false),
        PRODUCTO("Producto", 126, false),
        CATEGORIA("Categoría", 100, false),
        UNIDAD("Unidad", 46, false),
        ENTRADAS("Entradas", 58, true),
        SALIDAS("Salidas", 52, true),
        STOCK("Stock", 48, true),
        PRECIO("Precio", 60, true),
        VALOR("Valor", 72, true),
        REORDEN("Punto de reorden", 100, false);

        public final String titulo;
        public final double ancho;
        public final boolean derecha;

        Columna(String titulo, double ancho, boolean derecha) {
            this.titulo = titulo;
            this.ancho = ancho;
            this.derecha = derecha;
        }
    }

    // Filas [desde, hasta) de una página; la última lleva los totales.
    public record Pagina(int numero, int desde, int hasta, boolean primera, boolean totales) {

        public int filas() {
            return hasta - desde;
        }
    }

    private DisenoReporte() {

    }

    // Reparte las filas en páginas de la altura dada.
    // Con 0 filas hay una sola página (con el aviso "sin productos").
    public static List<Pagina> paginar(int totalFilas, double altoPagina) {
        List<Pagina> paginas = new ArrayList<>();

        // Sin productos: una página con un renglón de aviso.
        if (totalFilas == 0) {
            paginas.add(new Pagina(1, 0, 0, true, true));
            return paginas;
        }

        int inicio = 0;

        while (inicio < totalFilas) {
            boolean primera = paginas.isEmpty();
            int restantes = totalFilas - inicio;

            int conTotales = Math.max(1, capacidad(altoPagina, primera, true));
            int sinTotales = Math.max(1, capacidad(altoPagina, primera, false));

            if (restantes <= conTotales) {
                paginas.add(new Pagina(paginas.size() + 1, inicio, totalFilas, primera, true));
                break;
            }

            // Si las filas restantes caben, pero sin espacio para los totales,
            // se deja la última fila para la página siguiente junto con los totales.
            int tomar = restantes <= sinTotales ? restantes - 1 : sinTotales;
            tomar = Math.max(1, tomar);

            paginas.add(new Pagina(paginas.size() + 1, inicio, inicio + tomar, primera, false));
            inicio += tomar;
        }

        return paginas;
    }

    // Altura ocupada por todo lo que no son filas de datos.
    public static double altoFijo(boolean primera, boolean totales) {
        double alto = MARGEN_SUPERIOR + MARGEN_INFERIOR
                + REGLA_ALTO + REGLA_MARGEN
                + TABLA_ENCABEZADO_ALTO
                + PIE_MARGEN + PIE_ALTO;

        alto += primera
                ? ENCABEZADO_ALTO + TARJETAS_ALTO + TARJETAS_MARGEN
                : ENCABEZADO_CONTINUACION_ALTO;

        if (totales) {
            alto += TOTALES_ALTO;
        }

        return alto;
    }

    private static int capacidad(double altoPagina, boolean primera, boolean totales) {
        return (int) Math.floor((altoPagina - altoFijo(primera, totales)) / FILA_ALTO);
    }
}
