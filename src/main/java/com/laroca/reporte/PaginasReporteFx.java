package com.laroca.reporte;

import java.util.ArrayList;
import java.util.List;

import com.laroca.controlador.Formato;
import com.laroca.modelo.FilaInventario;
import com.laroca.reporte.DisenoReporte.Columna;
import com.laroca.reporte.DisenoReporte.Pagina;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

// Construye las páginas ("papel" blanco) del Reporte de Inventario.
// Las mismas páginas se muestran en pantalla y se mandan a la impresora.
// Los estilos están en laroca.css (clases reporte-*, tarjeta-*, pastilla-*).
public final class PaginasReporteFx {

    private PaginasReporteFx() {

    }

    public static List<VBox> construir(ReporteInventario reporte, double altoPagina) {
        List<Pagina> plan = DisenoReporte.paginar(reporte.getFilas().size(), altoPagina);
        List<VBox> paginas = new ArrayList<>();

        for (Pagina pagina : plan) {
            paginas.add(construirPagina(reporte, pagina, plan.size(), altoPagina));
        }

        return paginas;
    }

    private static VBox construirPagina(ReporteInventario reporte, Pagina pagina,
            int totalPaginas, double altoPagina) {

        VBox papel = new VBox();
        papel.getStyleClass().add("reporte-papel");
        papel.setPadding(new Insets(DisenoReporte.MARGEN_SUPERIOR, DisenoReporte.MARGEN_LATERAL,
                DisenoReporte.MARGEN_INFERIOR, DisenoReporte.MARGEN_LATERAL));
        fijarTamano(papel, DisenoReporte.ANCHO_PAGINA, altoPagina);

        if (pagina.primera()) {
            papel.getChildren().add(encabezado(reporte));
        } else {
            papel.getChildren().add(encabezadoContinuacion(reporte));
        }

        Region regla = new Region();
        regla.getStyleClass().add("reporte-regla");
        fijarAlto(regla, DisenoReporte.REGLA_ALTO);
        VBox.setMargin(regla, new Insets(0, 0, DisenoReporte.REGLA_MARGEN, 0));
        papel.getChildren().add(regla);

        if (pagina.primera()) {
            HBox tarjetas = tarjetas(reporte);
            VBox.setMargin(tarjetas, new Insets(0, 0, DisenoReporte.TARJETAS_MARGEN, 0));
            papel.getChildren().add(tarjetas);
        }

        papel.getChildren().add(filaEncabezadoTabla());

        List<FilaInventario> filas = reporte.getFilas();

        if (filas.isEmpty()) {
            papel.getChildren().add(filaSinDatos());
        }

        for (int i = pagina.desde(); i < pagina.hasta(); i++) {
            papel.getChildren().add(filaDatos(filas.get(i)));
        }

        if (pagina.totales()) {
            papel.getChildren().add(filaTotales(reporte));
        }

        Region relleno = new Region();
        VBox.setVgrow(relleno, Priority.ALWAYS);
        relleno.setMinHeight(DisenoReporte.PIE_MARGEN);
        papel.getChildren().add(relleno);

        papel.getChildren().add(pie(reporte, pagina.numero(), totalPaginas));

        return papel;
    }

    // ------------------------------------------------------------------
    // Encabezado
    // ------------------------------------------------------------------

    private static HBox encabezado(ReporteInventario reporte) {
        Label negocio = etiqueta("Abarrotes", "reporte-negocio");
        Label titulo = etiqueta(TextosReporte.TITULO, "reporte-titulo");

        VBox izquierda = new VBox(negocio, titulo);
        izquierda.setAlignment(Pos.BOTTOM_LEFT);

        VBox derecha = new VBox();
        derecha.setAlignment(Pos.TOP_RIGHT);
        derecha.getStyleClass().add("reporte-metadatos");

        for (String linea : TextosReporte.metadatos(reporte)) {
            Label meta = etiqueta(linea, "reporte-meta");
            meta.setMaxWidth(Double.MAX_VALUE);
            meta.setAlignment(Pos.CENTER_RIGHT);
            derecha.getChildren().add(meta);
        }

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);

        HBox encabezado = new HBox(izquierda, espacio, derecha);
        encabezado.setAlignment(Pos.BOTTOM_LEFT);
        encabezado.setPadding(new Insets(0, 0, 6, 0));
        fijarAlto(encabezado, DisenoReporte.ENCABEZADO_ALTO);
        return encabezado;
    }

    private static HBox encabezadoContinuacion(ReporteInventario reporte) {
        Label titulo = etiqueta(TextosReporte.TITULO + " (continuación)", "reporte-titulo-chico");
        Label existencias = etiqueta(TextosReporte.existencias(reporte), "reporte-meta");

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);

        HBox encabezado = new HBox(titulo, espacio, existencias);
        encabezado.setAlignment(Pos.BOTTOM_LEFT);
        encabezado.setPadding(new Insets(0, 0, 4, 0));
        fijarAlto(encabezado, DisenoReporte.ENCABEZADO_CONTINUACION_ALTO);
        return encabezado;
    }

    // ------------------------------------------------------------------
    // Tarjetas de resumen
    // ------------------------------------------------------------------

    private static HBox tarjetas(ReporteInventario reporte) {
        HBox tarjetas = new HBox(DisenoReporte.TARJETAS_ESPACIO,
                tarjeta("Productos", Formato.entero(reporte.getTotalProductos()), false),
                tarjeta("Unidades en existencia", Formato.entero(reporte.getTotalStock()),
                        reporte.getTotalStock() < 0),
                tarjeta("Valor del inventario", Formato.dinero(reporte.getTotalValor()),
                        reporte.getTotalValor().signum() < 0),
                tarjeta("Productos a reordenar", Formato.entero(reporte.getProductosReordenar()),
                        reporte.getProductosReordenar() > 0));

        fijarAlto(tarjetas, DisenoReporte.TARJETAS_ALTO);
        return tarjetas;
    }

    private static VBox tarjeta(String titulo, String valor, boolean alerta) {
        Label lblTitulo = etiqueta(titulo, "tarjeta-titulo");
        Label lblValor = etiqueta(valor, "tarjeta-valor");

        if (alerta) {
            lblValor.getStyleClass().add("tarjeta-valor-alerta");
        }

        VBox tarjeta = new VBox(lblTitulo, lblValor);
        tarjeta.getStyleClass().add("tarjeta-resumen");
        tarjeta.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tarjeta, Priority.ALWAYS);
        return tarjeta;
    }

    // ------------------------------------------------------------------
    // Tabla
    // ------------------------------------------------------------------

    private static HBox filaEncabezadoTabla() {
        HBox fila = new HBox();
        fila.getStyleClass().add("reporte-encabezado-tabla");
        fijarAlto(fila, DisenoReporte.TABLA_ENCABEZADO_ALTO);

        for (Columna columna : Columna.values()) {
            fila.getChildren().add(celda(columna.titulo, columna, "reporte-celda-encabezado"));
        }

        return fila;
    }

    private static HBox filaDatos(FilaInventario dato) {
        HBox fila = new HBox();
        fila.getStyleClass().add("reporte-fila");
        fijarAlto(fila, DisenoReporte.FILA_ALTO);

        for (Columna columna : Columna.values()) {
            if (columna == Columna.REORDEN) {
                fila.getChildren().add(celdaPastilla(dato.isReordenar()));
                continue;
            }

            Label celda = celda(TextosReporte.valor(dato, columna), columna, "reporte-celda");

            if (columna == Columna.STOCK) {
                celda.getStyleClass().add("reporte-celda-stock");

                if (dato.isStockNegativo()) {
                    celda.getStyleClass().add("reporte-celda-negativa");
                }
            }

            if (columna == Columna.VALOR && dato.getValor().signum() < 0) {
                celda.getStyleClass().add("reporte-celda-negativa");
            }

            fila.getChildren().add(celda);
        }

        return fila;
    }

    private static HBox filaSinDatos() {
        Label aviso = etiqueta(TextosReporte.SIN_DATOS, "reporte-celda");
        aviso.getStyleClass().add("reporte-sin-datos");
        aviso.setPadding(new Insets(0, 6, 0, 6));

        HBox fila = new HBox(aviso);
        fila.getStyleClass().add("reporte-fila");
        fila.setAlignment(Pos.CENTER_LEFT);
        fijarAlto(fila, DisenoReporte.FILA_ALTO);
        return fila;
    }

    private static HBox filaTotales(ReporteInventario reporte) {
        HBox fila = new HBox();
        fila.getStyleClass().add("fila-totales");
        fijarAlto(fila, DisenoReporte.TOTALES_ALTO);

        for (Columna columna : Columna.values()) {
            Label celda = celda(TextosReporte.total(reporte, columna), columna, "reporte-celda");

            boolean negativo = (columna == Columna.STOCK && reporte.getTotalStock() < 0)
                    || (columna == Columna.VALOR && reporte.getTotalValor().signum() < 0);

            if (negativo) {
                celda.getStyleClass().add("reporte-celda-negativa");
            }

            fila.getChildren().add(celda);
        }

        return fila;
    }

    private static Label celda(String texto, Columna columna, String clase) {
        Label celda = etiqueta(texto, clase);
        celda.setTextOverrun(OverrunStyle.ELLIPSIS);
        celda.setAlignment(columna.derecha ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        celda.setPadding(new Insets(0, 6, 0, 6));
        fijarAncho(celda, columna.ancho);
        celda.setMaxHeight(Double.MAX_VALUE);
        return celda;
    }

    private static HBox celdaPastilla(boolean reordenar) {
        Label pastilla = etiqueta(TextosReporte.pastilla(reordenar), "pastilla");
        pastilla.getStyleClass().add(reordenar ? "pastilla-alerta" : "pastilla-ok");

        HBox celda = new HBox(pastilla);
        celda.setAlignment(Pos.CENTER_LEFT);
        celda.setPadding(new Insets(0, 6, 0, 6));
        fijarAncho(celda, Columna.REORDEN.ancho);
        return celda;
    }

    // ------------------------------------------------------------------
    // Pie
    // ------------------------------------------------------------------

    private static HBox pie(ReporteInventario reporte, int numero, int total) {
        VBox notas = new VBox(1);
        notas.setAlignment(Pos.BOTTOM_LEFT);

        for (String nota : TextosReporte.notas(reporte)) {
            Label lbl = etiqueta(nota, "reporte-nota");

            if (nota.startsWith(TextosReporte.PREFIJO_AVISO)) {
                lbl.getStyleClass().add("reporte-nota-alerta");
            }

            notas.getChildren().add(lbl);
        }

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);

        Label pagina = etiqueta(TextosReporte.pagina(numero, total), "reporte-nota");

        HBox pie = new HBox(notas, espacio, pagina);
        pie.setAlignment(Pos.BOTTOM_LEFT);
        fijarAlto(pie, DisenoReporte.PIE_ALTO);
        return pie;
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private static Label etiqueta(String texto, String clase) {
        Label etiqueta = new Label(texto);
        etiqueta.getStyleClass().add(clase);
        return etiqueta;
    }

    private static void fijarTamano(Region region, double ancho, double alto) {
        fijarAncho(region, ancho);
        fijarAlto(region, alto);
    }

    private static void fijarAncho(Region region, double ancho) {
        region.setMinWidth(ancho);
        region.setPrefWidth(ancho);
        region.setMaxWidth(ancho);
    }

    private static void fijarAlto(Region region, double alto) {
        region.setMinHeight(alto);
        region.setPrefHeight(alto);
        region.setMaxHeight(alto);
    }
}
