package com.laroca.controlador;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
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
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;

// Reporte de Inventario (pantalla "Stock").
//
// Stock = Productos.existencia, la existencia real que actualizan en una
// transacción las compras, ventas, mermas, devoluciones y cancelaciones.
// Entradas y salidas son los movimientos activos del periodo Desde–Hasta
// (días completos). Por eso, si el periodo no cubre todo el historial o se
// capturó inventario inicial, Stock no es igual a Entradas − Salidas; el
// reporte lo indica en su pie.
public class EstadisticasStockController {

    private static final String TIPO_INVENTARIO_ACTUAL = "Inventario actual";

    private static final String HOJA_ESTILOS = "/com/laroca/vista/laroca.css";

    // Acepta 1/9/2026 y 01/09/2026; rechaza fechas imposibles (31/02).
    private static final DateTimeFormatter FECHA_CAPTURA =
            DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private static final DateTimeFormatter FECHA_ARCHIVO = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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

    // idCategoria null = "Todas".
    private record OpcionCategoria(Integer idCategoria, String nombre, String texto) {

        @Override
        public String toString() {
            return texto;
        }
    }

    @FXML private Button btnGenerar;
    @FXML private Button btnImprimir;
    @FXML private Button btnExportarPdf;
    @FXML private Button btnExportarExcel;
    @FXML private Button btnVolverMenu;

    @FXML private ComboBox<String> cmbTipoReporte;
    @FXML private ComboBox<OpcionCategoria> cmbCategoria;
    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private ComboBox<FiltroReorden> cmbReordenar;

    @FXML private Label lblFecha;
    @FXML private Label lblEstado;

    @FXML private ScrollPane scrollReporte;
    @FXML private VBox contenedorPaginas;

    private final ReporteInventarioDAO reporteDAO = new ReporteInventarioDAO();
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();

    // Último reporte generado: es el que se imprime y se exporta.
    private ReporteInventario reporteActual;

    @FXML
    public void initialize() {
        lblFecha.setText(Formato.fecha(LocalDate.now()));

        // "Movimientos del periodo" no está implementado; solo se ofrece
        // el inventario actual.
        cmbTipoReporte.getItems().setAll(TIPO_INVENTARIO_ACTUAL);
        cmbTipoReporte.setValue(TIPO_INVENTARIO_ACTUAL);

        cmbReordenar.getItems().setAll(FiltroReorden.values());
        cmbReordenar.setValue(FiltroReorden.TODOS);

        configurarFecha(dpDesde);
        configurarFecha(dpHasta);

        // Del día 1 del mes actual a hoy.
        LocalDate hoy = LocalDate.now();
        dpDesde.setValue(hoy.withDayOfMonth(1));
        dpHasta.setValue(hoy);

        cargarCategorias();

        // Avisar que el reporte mostrado ya no corresponde a los filtros.
        cmbCategoria.valueProperty().addListener((obs, antes, ahora) -> filtrosCambiados());
        cmbReordenar.valueProperty().addListener((obs, antes, ahora) -> filtrosCambiados());
        dpDesde.valueProperty().addListener((obs, antes, ahora) -> filtrosCambiados());
        dpHasta.valueProperty().addListener((obs, antes, ahora) -> filtrosCambiados());

        // Primer reporte con los filtros por omisión.
        generar(false);
    }

    private void configurarFecha(DatePicker selector) {
        selector.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate fecha) {
                return Formato.fecha(fecha);
            }

            @Override
            public LocalDate fromString(String texto) {
                return interpretarFecha(texto);
            }
        });
    }

    // null si el texto está vacío o no es una fecha dd/MM/yyyy válida.
    private static LocalDate interpretarFecha(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(texto.trim(), FECHA_CAPTURA);
        } catch (DateTimeParseException e) {
            return null;
        }
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

        // Las inactivas al final: pueden tener productos activos todavía.
        for (Categoria categoria : inactivas) {
            opciones.add(opcion(categoria, true));
        }

        cmbCategoria.getItems().setAll(opciones);
        cmbCategoria.setValue(opciones.get(0));
    }

    private static OpcionCategoria opcion(Categoria categoria, boolean inactiva) {
        String nombre = categoria.getNombre();
        return new OpcionCategoria(categoria.getIdCategoria(), nombre,
                inactiva ? nombre + " (inactiva)" : nombre);
    }

    private void filtrosCambiados() {
        if (reporteActual != null) {
            lblEstado.setText("Los filtros cambiaron · presiona «Generar reporte» para actualizar");
        }
    }

    // ------------------------------------------------------------------
    // Generar
    // ------------------------------------------------------------------

    @FXML
    private void generarReporte() {
        generar(true);
    }

    // conAlertas = false al abrir la pantalla: los errores se avisan
    // cuando la ventana ya está visible.
    private void generar(boolean conAlertas) {
        LocalDate desde = leerFecha(dpDesde);
        LocalDate hasta = leerFecha(dpHasta);

        if (desde == null || hasta == null) {
            error(conAlertas, "Fecha no válida",
                    "Escribe las fechas Desde y Hasta con el formato dd/mm/aaaa.");
            return;
        }

        if (hasta.isBefore(desde)) {
            error(conAlertas, "Periodo no válido",
                    "La fecha Desde no puede ser posterior a la fecha Hasta.");
            return;
        }

        OpcionCategoria categoria = cmbCategoria.getValue();
        if (categoria == null) {
            categoria = cmbCategoria.getItems().get(0);
            cmbCategoria.setValue(categoria);
        }

        FiltroReorden filtro = cmbReordenar.getValue() == null
                ? FiltroReorden.TODOS
                : cmbReordenar.getValue();

        List<FilaInventario> filas = reporteDAO.consultar(desde, hasta, categoria.idCategoria());

        if (filas == null) {
            error(conAlertas, "No se pudo generar el reporte", reporteDAO.getMensajeError());
            return;
        }

        List<FilaInventario> filtradas = filas.stream().filter(filtro::incluye).toList();

        String tipo = cmbTipoReporte.getValue() == null ? TIPO_INVENTARIO_ACTUAL : cmbTipoReporte.getValue();

        reporteActual = new ReporteInventario(
                tipo, desde, hasta,
                categoria.nombre(),
                filtro == FiltroReorden.TODOS ? null : filtro.toString(),
                LocalDateTime.now().withSecond(0).withNano(0),
                Sesion.getNombreParaMostrar(),
                filtradas);

        mostrarReporte();
        lblEstado.setText(TextosReporte.resumen(reporteActual));
    }

    // Lee lo escrito en el selector aunque todavía no se haya confirmado.
    private static LocalDate leerFecha(DatePicker selector) {
        String texto = selector.getEditor().getText();

        if (texto != null && !texto.isBlank()) {
            LocalDate fecha = interpretarFecha(texto);

            if (fecha != null && !fecha.equals(selector.getValue())) {
                selector.setValue(fecha);
            }
            return fecha;
        }

        return selector.getValue();
    }

    private void mostrarReporte() {
        contenedorPaginas.getChildren().setAll(
                PaginasReporteFx.construir(reporteActual, DisenoReporte.ALTO_PAGINA));
        scrollReporte.setVvalue(0);

        btnImprimir.setDisable(false);
        btnExportarPdf.setDisable(false);
        btnExportarExcel.setDisable(false);
    }

    // ------------------------------------------------------------------
    // Imprimir
    // ------------------------------------------------------------------

    @FXML
    private void imprimir() {
        if (reporteActual == null) {
            return;
        }

        PrinterJob trabajo = PrinterJob.createPrinterJob();

        if (trabajo == null) {
            mostrarError("Sin impresora", "No hay ninguna impresora disponible en este equipo.");
            return;
        }

        // Carta horizontal por omisión; el usuario la puede cambiar.
        Printer impresora = trabajo.getPrinter();
        trabajo.getJobSettings().setPageLayout(impresora.createPageLayout(
                Paper.NA_LETTER, PageOrientation.LANDSCAPE, Printer.MarginType.DEFAULT));

        if (!trabajo.showPrintDialog(ventana())) {
            trabajo.cancelJob();
            return;
        }

        PageLayout hoja = trabajo.getJobSettings().getPageLayout();

        // Escala para que el ancho del papel del reporte llene el área
        // imprimible; el alto de cada página se ajusta a la hoja elegida.
        double escala = hoja.getPrintableWidth() / DisenoReporte.ANCHO_PAGINA;
        double altoPagina = hoja.getPrintableHeight() / escala;

        List<VBox> paginas = PaginasReporteFx.construir(reporteActual, altoPagina);
        String estilos = getClass().getResource(HOJA_ESTILOS).toExternalForm();
        boolean impreso = true;

        for (VBox pagina : paginas) {
            // Una escena temporal aplica laroca.css a la página antes de imprimir.
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
            lblEstado.setText("Reporte enviado a la impresora · " + paginas.size()
                    + (paginas.size() == 1 ? " página" : " páginas"));
        } else {
            trabajo.cancelJob();
            mostrarError("Error al imprimir", "No se pudo imprimir el reporte.");
        }
    }

    // ------------------------------------------------------------------
    // Exportar
    // ------------------------------------------------------------------

    @FXML
    private void exportarPdf() {
        File archivo = elegirArchivo("Exportar reporte a PDF", "PDF (*.pdf)", "pdf");

        if (archivo == null) {
            return;
        }

        try {
            ExportadorPdf.exportar(reporteActual, archivo);
            lblEstado.setText("PDF guardado en " + archivo.getAbsolutePath());
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
            mostrarError("No se pudo exportar a PDF", e.getMessage());
        }
    }

    @FXML
    private void exportarExcel() {
        File archivo = elegirArchivo("Exportar reporte a Excel", "Libro de Excel (*.xlsx)", "xlsx");

        if (archivo == null) {
            return;
        }

        try {
            ExportadorExcel.exportar(reporteActual, archivo);
            lblEstado.setText("Excel guardado en " + archivo.getAbsolutePath());
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
            mostrarError("No se pudo exportar a Excel", e.getMessage());
        }
    }

    private File elegirArchivo(String titulo, String descripcion, String extension) {
        if (reporteActual == null) {
            return null;
        }

        FileChooser selector = new FileChooser();
        selector.setTitle(titulo);
        selector.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(descripcion, "*." + extension));
        selector.setInitialFileName("Reporte_Inventario_"
                + reporteActual.getDesde().format(FECHA_ARCHIVO) + "_"
                + reporteActual.getHasta().format(FECHA_ARCHIVO) + "." + extension);

        File archivo = selector.showSaveDialog(ventana());

        if (archivo != null && !archivo.getName().toLowerCase().endsWith("." + extension)) {
            archivo = new File(archivo.getParentFile(), archivo.getName() + "." + extension);
        }

        return archivo;
    }

    // ------------------------------------------------------------------
    // Navegación
    // ------------------------------------------------------------------

    @FXML
    private void volverAlMenu() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/laroca/vista/menu.fxml"));
            Parent vista = loader.load();

            Navegacion.mostrar((Stage) ventana(), vista, Navegacion.TITULO_MENU);
        } catch (IOException | IllegalStateException e) {
            e.printStackTrace();
            mostrarError("Error de navegación",
                    "No se pudo regresar al menú principal.\n" + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Mensajes: la barra de estado para lo normal, alertas para errores.
    // ------------------------------------------------------------------

    private void error(boolean conAlerta, String titulo, String mensaje) {
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
        alerta.setContentText(mensaje == null || mensaje.isBlank()
                ? "Ocurrió un error inesperado." : mensaje);

        Window duenio = ventana();
        if (duenio != null) {
            alerta.initOwner(duenio);
        }

        alerta.showAndWait();
    }

    private Window ventana() {
        return btnGenerar.getScene() == null ? null : btnGenerar.getScene().getWindow();
    }
}
