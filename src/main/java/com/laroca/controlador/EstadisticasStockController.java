package com.laroca.controlador;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.laroca.dao.CategoriaDAO;
import com.laroca.dao.ReporteInventarioDAO;
import com.laroca.modelo.Categoria;
import com.laroca.modelo.Estados;
import com.laroca.modelo.FilaInventario;
import com.laroca.modelo.Sesion;
import com.laroca.reporte.DisenoReporte;
import com.laroca.reporte.ExportadorExcel;
import com.laroca.reporte.ExportadorPdf;
import com.laroca.reporte.PaginasReporteFx;
import com.laroca.reporte.ReporteInventario;
import com.laroca.reporte.TextosReporte;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

public class EstadisticasStockController {

    private static final String TIPO_INVENTARIO_ACTUAL =
            "Inventario actual";

    private static final String HOJA_ESTILOS =
            "/com/laroca/vista/laroca.css";

    private static final DateTimeFormatter FECHA_ARCHIVO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private enum FiltroReorden {
        TODOS("Todos"),
        SOLO_REORDENAR("Solo a reordenar"),
        SIN_REORDENAR("Sin reordenar");

        private final String texto;

        FiltroReorden(String texto) {
            this.texto = texto;
        }

        boolean incluye(FilaInventario fila) {
            return switch (this) {
                case TODOS -> true;
                case SOLO_REORDENAR -> fila.isReordenar();
                case SIN_REORDENAR -> !fila.isReordenar();
            };
        }

        @Override
        public String toString() {
            return texto;
        }
    }

    private record OpcionCategoria(
            Integer idCategoria,
            String nombre,
            String texto) {

        @Override
        public String toString() {
            return texto;
        }
    }

    @FXML private Button btnImprimir;
    @FXML private Button btnExportarPdf;
    @FXML private Button btnExportarExcel;
    @FXML private Button btnVolverMenu;

    @FXML private ComboBox<String> cmbTipoReporte;
    @FXML private ComboBox<OpcionCategoria> cmbCategoria;
    @FXML private ComboBox<FiltroReorden> cmbReordenar;

    @FXML private Label lblFecha;
    @FXML private Label lblEstado;

    @FXML private ScrollPane scrollReporte;
    @FXML private VBox contenedorPaginas;

    private final ReporteInventarioDAO reporteDAO =
            new ReporteInventarioDAO();

    private final CategoriaDAO categoriaDAO =
            new CategoriaDAO();

    private ReporteInventario reporteActual;

    @FXML
    public void initialize() {

        if (!Navegacion.verificarPermiso(btnVolverMenu, "CONSULTAR_INVENTARIO")) {
            return;
        }

        LocalDate hoy = LocalDate.now();

        lblFecha.setText(Formato.fecha(hoy));

        cmbTipoReporte.getItems().setAll(TIPO_INVENTARIO_ACTUAL);
        cmbTipoReporte.setValue(TIPO_INVENTARIO_ACTUAL);

        cmbReordenar.getItems().setAll(FiltroReorden.values());
        cmbReordenar.setValue(FiltroReorden.TODOS);

        cargarCategorias();

        // El reporte se vuelve a generar al cambiar cualquier filtro.
        cmbCategoria.valueProperty().addListener(
                (obs, antes, ahora) -> filtroCambiado(ahora));

        cmbReordenar.valueProperty().addListener(
                (obs, antes, ahora) -> filtroCambiado(ahora));

        cmbTipoReporte.valueProperty().addListener(
                (obs, antes, ahora) -> filtroCambiado(ahora));

        generar(false);
    }

    private void cargarCategorias() {
        List<OpcionCategoria> opciones = new ArrayList<>();
        opciones.add(new OpcionCategoria(null, "Todas", "Todas"));

        List<Categoria> inactivas = new ArrayList<>();

        for (Categoria categoria : categoriaDAO.listar()) {
            if (Estados.esActivo(categoria.getEstado())) {
                opciones.add(opcion(categoria, false));
            } else {
                inactivas.add(categoria);
            }
        }

        for (Categoria categoria : inactivas) {
            opciones.add(opcion(categoria, true));
        }

        cmbCategoria.getItems().setAll(opciones);
        cmbCategoria.setValue(opciones.get(0));
    }

    private static OpcionCategoria opcion(
            Categoria categoria,
            boolean inactiva) {

        String nombre = categoria.getNombre();

        return new OpcionCategoria(
                categoria.getIdCategoria(),
                nombre,
                inactiva ? nombre + " (inactiva)" : nombre);
    }

    private void filtroCambiado(Object valor) {
        if (valor != null) {
            generar(true);
        }
    }

    private void generar(boolean conAlertas) {

        if (!Sesion.tienePermiso("CONSULTAR_INVENTARIO")) {
            return;
        }

        OpcionCategoria categoria = cmbCategoria.getValue();

        // Al volver a "Todas", el listener genera el reporte.
        if (categoria == null) {
            cmbCategoria.setValue(cmbCategoria.getItems().get(0));
            return;
        }

        FiltroReorden filtro = cmbReordenar.getValue() == null
                ? FiltroReorden.TODOS
                : cmbReordenar.getValue();

        List<FilaInventario> filas =
                reporteDAO.consultar(categoria.idCategoria());

        if (filas == null) {
            error(conAlertas, "No se pudo generar el reporte",
                    reporteDAO.getMensajeError());
            return;
        }

        List<FilaInventario> filtradas =
                filas.stream().filter(filtro::incluye).toList();

        String tipo = cmbTipoReporte.getValue() == null
                ? TIPO_INVENTARIO_ACTUAL
                : cmbTipoReporte.getValue();

        reporteActual = new ReporteInventario(
                tipo,
                categoria.nombre(),
                filtro == FiltroReorden.TODOS ? null : filtro.toString(),
                LocalDateTime.now().withSecond(0).withNano(0),
                Sesion.getNombreParaMostrar(),
                filtradas);

        mostrarReporte();
        lblEstado.setText(TextosReporte.resumen(reporteActual));
    }

    private List<VBox> construirPaginas(double altoPagina) {
        return PaginasReporteFx.construir(reporteActual, altoPagina);
    }

    private void mostrarReporte() {
        contenedorPaginas.getChildren().setAll(
                construirPaginas(DisenoReporte.ALTO_PAGINA));

        scrollReporte.setVvalue(0);

        btnImprimir.setDisable(false);
        btnExportarPdf.setDisable(false);
        btnExportarExcel.setDisable(false);
    }

    @FXML
    private void imprimir() {

        if (!Sesion.tienePermiso("CONSULTAR_INVENTARIO")) {
            return;
        }

        if (reporteActual == null) {
            return;
        }

        PrinterJob trabajo = PrinterJob.createPrinterJob();

        if (trabajo == null) {
            mostrarError("Sin impresora",
                    "No hay ninguna impresora disponible en este equipo.");
            return;
        }

        Printer impresora = trabajo.getPrinter();

        trabajo.getJobSettings().setPageLayout(
                impresora.createPageLayout(
                        Paper.NA_LETTER,
                        PageOrientation.LANDSCAPE,
                        Printer.MarginType.DEFAULT));

        if (!trabajo.showPrintDialog(ventana())) {
            trabajo.cancelJob();
            return;
        }

        PageLayout hoja = trabajo.getJobSettings().getPageLayout();

        double escala =
                hoja.getPrintableWidth() / DisenoReporte.ANCHO_PAGINA;

        double altoPagina =
                hoja.getPrintableHeight() / escala;

        List<VBox> paginas = construirPaginas(altoPagina);

        String estilos =
                getClass().getResource(HOJA_ESTILOS).toExternalForm();

        boolean impreso = true;

        for (VBox pagina : paginas) {
            Scene escena = new Scene(new Group(pagina));
            escena.getStylesheets().add(estilos);
            escena.getRoot().applyCss();
            escena.getRoot().layout();

            pagina.getTransforms().add(new Scale(escala, escala));

            if (!trabajo.printPage(hoja, pagina)) {
                impreso = false;
                break;
            }
        }

        if (impreso && trabajo.endJob()) {
            lblEstado.setText(
                    "Reporte enviado a la impresora · "
                    + paginas.size()
                    + (paginas.size() == 1 ? " página" : " páginas"));
        } else {
            trabajo.cancelJob();
            mostrarError("Error al imprimir",
                    "No se pudo imprimir el reporte.");
        }
    }

    @FXML
    private void exportarPdf() {
        if (!Sesion.tienePermiso("CONSULTAR_INVENTARIO")) {
            return;
        }

        File archivo = elegirArchivo(
                "Exportar reporte a PDF",
                "PDF (*.pdf)",
                "pdf");

        if (archivo == null) {
            return;
        }

        try {
            ExportadorPdf.exportar(reporteActual, archivo);
            lblEstado.setText(
                    "PDF guardado en " + archivo.getAbsolutePath());

        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
            mostrarError("No se pudo exportar a PDF", e.getMessage());
        }
    }

    @FXML
    private void exportarExcel() {
        if (!Sesion.tienePermiso("CONSULTAR_INVENTARIO")) {
            return;
        }

        File archivo = elegirArchivo(
                "Exportar reporte a Excel",
                "Libro de Excel (*.xlsx)",
                "xlsx");

        if (archivo == null) {
            return;
        }

        try {
            ExportadorExcel.exportar(reporteActual, archivo);
            lblEstado.setText(
                    "Excel guardado en " + archivo.getAbsolutePath());

        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
            mostrarError("No se pudo exportar a Excel", e.getMessage());
        }
    }

    private File elegirArchivo(
            String titulo,
            String descripcion,
            String extension) {

        if (reporteActual == null) {
            return null;
        }

        FileChooser selector = new FileChooser();
        selector.setTitle(titulo);

        selector.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        descripcion, "*." + extension));

        selector.setInitialFileName(
                "Reporte_Inventario_"
                + reporteActual.getFechaExistencias().format(FECHA_ARCHIVO)
                + "." + extension);

        File archivo = selector.showSaveDialog(ventana());

        if (archivo != null
                && !archivo.getName().toLowerCase().endsWith("." + extension)) {

            archivo = new File(
                    archivo.getParentFile(),
                    archivo.getName() + "." + extension);
        }

        return archivo;
    }

    @FXML
    private void volverAlMenu() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/laroca/vista/menu.fxml"));

            Parent vista = loader.load();

            Navegacion.mostrar(
                    (Stage) ventana(),
                    vista,
                    Navegacion.TITULO_MENU);

        } catch (IOException | IllegalStateException e) {
            e.printStackTrace();

            mostrarError(
                    "Error de navegación",
                    "No se pudo regresar al menú principal.\n"
                    + e.getMessage());
        }
    }

    private void error(
            boolean conAlerta,
            String titulo,
            String mensaje) {

        lblEstado.setText(titulo + ": " + mensaje);

        if (conAlerta) {
            mostrarError(titulo, mensaje);
        } else {
            Platform.runLater(() -> mostrarError(titulo, mensaje));
        }
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);

        alerta.setTitle(titulo);
        alerta.setHeaderText(null);

        alerta.setContentText(
                mensaje == null || mensaje.isBlank()
                ? "Ocurrió un error inesperado."
                : mensaje);

        Window duenio = ventana();

        if (duenio != null) {
            alerta.initOwner(duenio);
        }

        alerta.showAndWait();
    }

    private Window ventana() {
        return btnVolverMenu.getScene() == null
                ? null
                : btnVolverMenu.getScene().getWindow();
    }
}
