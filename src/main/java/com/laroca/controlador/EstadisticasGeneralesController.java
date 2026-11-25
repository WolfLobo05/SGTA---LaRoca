package com.laroca.controlador;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.laroca.conexion.Conexion;
import com.laroca.dao.CategoriaDAO;
import com.laroca.modelo.Categoria;
import com.laroca.modelo.Estados;
import com.laroca.modelo.Sesion;
import com.laroca.reporte.DatosReporteEstadisticas;
import com.laroca.reporte.ExportadorEstadisticasPdf;
import com.laroca.reporte.ExportadorEstadisticasExcel;

import javafx.animation.Animation;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.DateCell;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

// Estadísticas generales del periodo.
//
// - Todas las consultas usan Conexion, días completos
//   (fecha >= desde AND fecha < hasta + 1) y solo registros activos
//   (las ventas, compras y mermas canceladas no cuentan).
// - La categoría filtra todos los paneles.
// - "Productos a reordenar" usa el mismo stock que el Reporte de
//   Inventario: Productos.existencia contra punto_reorden.
public class EstadisticasGeneralesController {

    private static final Locale ESPANOL = Locale.of("es", "MX");

    private static final DateTimeFormatter FECHA_CAPTURA =
            DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private static final DateTimeFormatter DIA_GRAFICA = DateTimeFormatter.ofPattern("dd/MM");

    private static final String ACTIVO = "UPPER(COALESCE(TRIM(%s.estado), '')) IN ('', 'ACTIVO')";

    // Opción del combo Periodo: un mes o todo el historial.
    private record Periodo(String etiqueta, LocalDate desde, LocalDate hasta) {

        @Override
        public String toString() {
            return etiqueta;
        }
    }

    // idCategoria null = "Todas".
    private record OpcionCategoria(Integer idCategoria, String texto) {

        @Override
        public String toString() {
            return texto;
        }
    }

    private record TopProducto(String nombre, int cantidad) {
    }

    private record Importe(String nombre, BigDecimal total) {
    }

    private record CompraProveedor(String nombre, int compras, BigDecimal total) {
    }

    private record MermaProducto(String nombre, int cantidad, BigDecimal total) {
    }

    private record StockBajo(String nombre, int stock, int puntoReorden) {

        String estado() {
            if (stock < 0) {
                return "Negativo";
            }
            return stock == 0 ? "Agotado" : "Reordenar";
        }
    }

    @FXML private ComboBox<Periodo> cmbPeriodo;
    @FXML private ComboBox<OpcionCategoria> cmbCategoria;
    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private Button btnVolver;
    @FXML private Button btnExportar;

    @FXML private Label lblFecha;
    @FXML private Label lblVentasPeriodo;
    @FXML private Label lblSubVentas;
    @FXML private Label lblNumVentas;
    @FXML private Label lblTicketPromedio;
    @FXML private Label lblComprasPeriodo;
    @FXML private Label lblSubCompras;
    @FXML private Label lblMermasPeriodo;
    @FXML private Label lblSubMermas;
    @FXML private Label lblSubGrafico;
    @FXML private Label lblEstado;

    @FXML private LineChart<String, Number> graficoVentasDia;

    @FXML private TableView<TopProducto> tablaTopProductos;
    @FXML private TableColumn<TopProducto, String> colTopNombre;
    @FXML private TableColumn<TopProducto, Integer> colTopCantidad;

    @FXML private TableView<Importe> tablaCategoria;
    @FXML private TableColumn<Importe, String> colCatNombre;
    @FXML private TableColumn<Importe, String> colCatTotal;

    @FXML private TableView<Importe> tablaMetodoPago;
    @FXML private TableColumn<Importe, String> colMetodoNombre;
    @FXML private TableColumn<Importe, String> colMetodoTotal;

    @FXML private TableView<CompraProveedor> tablaComprasProveedor;
    @FXML private TableColumn<CompraProveedor, String> colProveedorNombre;
    @FXML private TableColumn<CompraProveedor, Integer> colProveedorCompras;
    @FXML private TableColumn<CompraProveedor, String> colProveedorTotal;

    @FXML private TableView<MermaProducto> tablaMermasProducto;
    @FXML private TableColumn<MermaProducto, String> colMermaProducto;
    @FXML private TableColumn<MermaProducto, Integer> colMermaCantidad;
    @FXML private TableColumn<MermaProducto, String> colMermaTotal;

    @FXML private TableView<StockBajo> tablaStockBajo;
    @FXML private TableColumn<StockBajo, String> colStockProd;
    @FXML private TableColumn<StockBajo, Integer> colStockActual;
    @FXML private TableColumn<StockBajo, Integer> colStockMinimo;
    @FXML private TableColumn<StockBajo, String> colStockEstado;

    private final CategoriaDAO categoriaDAO = new CategoriaDAO();

    // Evita recargar mientras se llenan los combos por código.
    private boolean cargandoFiltros;
    private boolean actualizandoFechas;
    // Límites reales del historial disponibles para los selectores de fecha.
    private LocalDate primeraFechaHistorica;

    private static final String PERIODO_HISTORICO = "Histórico completo";
    private static final String PERIODO_PERSONALIZADO = "Rango personalizado";

    // Espera breve antes de consultar, para no hacerlo en cada tecla.
    private final PauseTransition recarga = new PauseTransition(Duration.millis(300));

    @FXML
    public void initialize() {
        if (!Navegacion.verificarPermiso(btnVolver, "ESTADISTICAS_GENERALES")) {
            return;
        }

        lblFecha.setText(Formato.fecha(LocalDate.now()));

        colTopNombre.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().nombre()));
        colTopCantidad.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().cantidad()));

        colCatNombre.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().nombre()));
        colCatTotal.setCellValueFactory(c -> new ReadOnlyStringWrapper(Formato.dinero(c.getValue().total())));

        colMetodoNombre.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().nombre()));
        colMetodoTotal.setCellValueFactory(c -> new ReadOnlyStringWrapper(Formato.dinero(c.getValue().total())));

        colProveedorNombre.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().nombre()));
        colProveedorCompras.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().compras()));
        colProveedorTotal.setCellValueFactory(c -> new ReadOnlyStringWrapper(Formato.dinero(c.getValue().total())));

        colMermaProducto.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().nombre()));
        colMermaCantidad.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().cantidad()));
        colMermaTotal.setCellValueFactory(c -> new ReadOnlyStringWrapper(Formato.dinero(c.getValue().total())));

        colStockProd.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().nombre()));
        colStockActual.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().stock()));
        colStockMinimo.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().puntoReorden()));
        colStockEstado.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().estado()));

        configurarFecha(dpDesde);
        configurarFecha(dpHasta);

        recarga.setOnFinished(e -> cargarDatosDashboard());

        // Al elegir un mes o el histórico, se aplican sus fechas.
        // "Rango personalizado" conserva las fechas que escribió el usuario.
        cmbPeriodo.valueProperty().addListener((obs, antes, periodo) -> {
            if (periodo != null && !cargandoFiltros) {
                if (periodo.desde() != null && periodo.hasta() != null) {
                    actualizandoFechas = true;
                    try {
                        dpDesde.setValue(periodo.desde());
                        dpHasta.setValue(periodo.hasta());
                    } finally {
                        actualizandoFechas = false;
                    }
                }
                programarRecarga();
            }
        });

        cmbCategoria.valueProperty().addListener((obs, antes, ahora) -> {
            if (ahora != null) {
                programarRecarga();
            }
        });

        cargarFiltrosDinamicos();
        cargarDatosDashboard();
    }

    private void programarRecarga() {
        if (!cargandoFiltros) {
            recarga.playFromStart();
        }
    }

    private void configurarFecha(DatePicker selector) {
        selector.valueProperty().addListener((obs, antes, ahora) -> {
            if (!actualizandoFechas && !cargandoFiltros) {
                marcarRangoPersonalizado();
                programarRecarga();
            }
        });
        selector.getEditor().textProperty().addListener((obs, antes, ahora) -> {
            if (!actualizandoFechas && !cargandoFiltros) {
                LocalDate escrita = interpretarFecha(ahora);
                // Solo cambiar a personalizado cuando el texto sea una fecha válida.
                if (escrita != null && !escrita.equals(selector.getValue())) {
                    // Solo se considera personalizado si la fecha realmente cambió.
                    // JavaFX también actualiza el texto del editor al cargar un periodo;
                    // ese refresco visual no debe cambiar el periodo seleccionado.
                    selector.setValue(escrita);
                }
            }
        });

        // Confirma lo escrito al salir del campo.
        selector.focusedProperty().addListener((obs, antes, enfocado) -> {
            if (!enfocado) {
                leerFecha(selector);
            }
        });

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

    private void marcarRangoPersonalizado() {
        if (cmbPeriodo == null || cargandoFiltros || actualizandoFechas) {
            return;
        }
        Periodo seleccionado = cmbPeriodo.getValue();
        if (seleccionado == null || !PERIODO_PERSONALIZADO.equals(seleccionado.etiqueta())) {
            for (Periodo opcion : cmbPeriodo.getItems()) {
                if (PERIODO_PERSONALIZADO.equals(opcion.etiqueta())) {
                    cmbPeriodo.setValue(opcion);
                    return;
                }
            }
            cmbPeriodo.setValue(new Periodo(PERIODO_PERSONALIZADO, null, null));
        }
    }

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

    // Meses disponibles desde el primer movimiento histórico hasta el actual.
    // Al abrir el dashboard se selecciona siempre el mes actual.
    private void cargarFiltrosDinamicos() {
        cargandoFiltros = true;

        try {
            Periodo previo = cmbPeriodo.getValue();
            List<Periodo> periodos = new ArrayList<>();
            YearMonth actual = YearMonth.now();
            LocalDate primeraFecha = null;

            // Primer movimiento activo entre ventas, compras, mermas y devoluciones.
            String sqlPrimera = "SELECT MIN(f) FROM ("
                    + "SELECT MIN(DATE(v.fecha)) AS f FROM Ventas v "
                    + "WHERE v.fecha IS NOT NULL AND " + activo("v") + " "
                    + "UNION ALL SELECT MIN(c.fecha) FROM Compras c "
                    + "WHERE c.fecha IS NOT NULL AND " + activo("c") + " "
                    + "UNION ALL SELECT MIN(m.fecha) FROM Mermas m "
                    + "WHERE m.fecha IS NOT NULL AND " + activo("m") + " "
                    + "UNION ALL SELECT MIN(dc.fecha) FROM Devolucion_Clientes dc "
                    + "WHERE dc.fecha IS NOT NULL AND " + activo("dc") + " "
                    + "UNION ALL SELECT MIN(dp.fecha) FROM Devolucion_Proveedores dp "
                    + "WHERE dp.fecha IS NOT NULL AND " + activo("dp") + ") t";

            try (Connection conexion = Conexion.conectar()) {
                if (conexion == null) {
                    lblEstado.setText("No se pudo conectar con la base de datos.");
                } else {
                    try (PreparedStatement ps = conexion.prepareStatement(sqlPrimera);
                         ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getDate(1) != null) {
                            primeraFecha = rs.getDate(1).toLocalDate();
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                lblEstado.setText("Error al cargar los periodos: " + e.getMessage());
            }

            primeraFechaHistorica = primeraFecha == null || primeraFecha.isAfter(LocalDate.now())
                    ? LocalDate.now() : primeraFecha;
            aplicarLimitesFechas();

            YearMonth primerMes = primeraFechaHistorica == null
                    ? actual : YearMonth.from(primeraFechaHistorica);

            // Mes actual primero, seguido de los meses anteriores.
            for (YearMonth ym = actual; !ym.isBefore(primerMes); ym = ym.minusMonths(1)) {
                periodos.add(mes(ym, primeraFechaHistorica));
            }

            // Evita dejar vacío el selector si no hay movimientos históricos.
            if (periodos.isEmpty()) {
                periodos.add(mes(actual, primeraFechaHistorica));
            }

            LocalDate hoy = LocalDate.now();
            LocalDate inicioHistorico = primeraFechaHistorica == null ? hoy : primeraFechaHistorica;
            periodos.add(new Periodo(PERIODO_HISTORICO, inicioHistorico, hoy));
            periodos.add(new Periodo(PERIODO_PERSONALIZADO, null, null));

            cmbPeriodo.getItems().setAll(periodos);

            // Al abrir por primera vez, el mes actual es el predeterminado.
            // Si se recargan filtros, se conserva el mes que estaba seleccionado.
            Periodo seleccion = periodos.get(0);
            if (previo != null) {
                for (Periodo p : periodos) {
                    if (p.etiqueta().equals(previo.etiqueta())) {
                        seleccion = p;
                        break;
                    }
                }
            }

            cmbPeriodo.setValue(seleccion);
            if (seleccion.desde() != null && seleccion.hasta() != null) {
                actualizandoFechas = true;
                try {
                    dpDesde.setValue(seleccion.desde());
                    dpHasta.setValue(seleccion.hasta());
                } finally {
                    actualizandoFechas = false;
                }
            }

            // Categorías: activas y, al final, inactivas.
            OpcionCategoria previa = cmbCategoria.getValue();
            List<OpcionCategoria> categorias = new ArrayList<>();
            List<OpcionCategoria> inactivas = new ArrayList<>();
            categorias.add(new OpcionCategoria(null, "Todas"));

            for (Categoria categoria : categoriaDAO.listar()) {
                if (Estados.esActivo(categoria.getEstado())) {
                    categorias.add(new OpcionCategoria(
                            categoria.getIdCategoria(), categoria.getNombre()));
                } else {
                    inactivas.add(new OpcionCategoria(
                            categoria.getIdCategoria(), categoria.getNombre() + " (inactiva)"));
                }
            }

            categorias.addAll(inactivas);
            cmbCategoria.getItems().setAll(categorias);

            OpcionCategoria elegida = categorias.get(0);
            if (previa != null) {
                for (OpcionCategoria opcion : categorias) {
                    if (opcion.texto().equals(previa.texto())) {
                        elegida = opcion;
                        break;
                    }
                }
            }
            cmbCategoria.setValue(elegida);

        } finally {
            cargandoFiltros = false;
        }
    }

    private void aplicarLimitesFechas() {
        LocalDate hoy = LocalDate.now();
        LocalDate minimo = primeraFechaHistorica == null ? hoy : primeraFechaHistorica;

        // DatePicker no tiene setMin/setMax. Los límites se aplican
        // deshabilitando las celdas fuera del rango permitido.
        dpDesde.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate fecha, boolean vacia) {
                super.updateItem(fecha, vacia);
                setDisable(vacia || fecha.isBefore(minimo) || fecha.isAfter(hoy));
            }
        });

        dpHasta.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate fecha, boolean vacia) {
                super.updateItem(fecha, vacia);
                setDisable(vacia || fecha.isBefore(minimo) || fecha.isAfter(hoy));
            }
        });
    }

    private boolean validarRangoFechas(LocalDate desde, LocalDate hasta, boolean mostrarEnBarra) {
        LocalDate hoy = LocalDate.now();
        LocalDate minimo = primeraFechaHistorica == null ? hoy : primeraFechaHistorica;

        if (desde == null || hasta == null) {
            if (mostrarEnBarra) lblEstado.setText("Selecciona las fechas Desde y Hasta.");
            return false;
        }
        if (desde.isBefore(minimo) || hasta.isBefore(minimo)) {
            if (mostrarEnBarra) lblEstado.setText("No puedes consultar fechas anteriores al primer movimiento registrado ("
                    + Formato.fecha(minimo) + ").");
            return false;
        }
        if (desde.isAfter(hoy) || hasta.isAfter(hoy)) {
            if (mostrarEnBarra) lblEstado.setText("No puedes consultar fechas posteriores a hoy.");
            return false;
        }
        if (hasta.isBefore(desde)) {
            if (mostrarEnBarra) lblEstado.setText("La fecha Desde no puede ser posterior a la fecha Hasta.");
            return false;
        }
        return true;
    }

    private static Periodo mes(YearMonth ym, LocalDate primeraFechaHistorica) {
        String nombre = ym.getMonth().getDisplayName(TextStyle.FULL, ESPANOL);
        String etiqueta = nombre.substring(0, 1).toUpperCase(ESPANOL) + nombre.substring(1) + " " + ym.getYear();
        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = ym.atDay(1);
        LocalDate finMes = ym.atEndOfMonth();

        // En el primer mes con movimientos, el periodo comienza en el primer
        // movimiento registrado, no en un día anterior al historial disponible.
        LocalDate desde = primeraFechaHistorica != null
                && YearMonth.from(primeraFechaHistorica).equals(ym)
                ? primeraFechaHistorica
                : inicioMes;
        LocalDate hasta = finMes.isAfter(hoy) ? hoy : finMes;

        return new Periodo(etiqueta, desde, hasta);
    }

    private static String activo(String alias) {
        return String.format(ACTIVO, alias);
    }

    // ------------------------------------------------------------------
    // Acciones
    // ------------------------------------------------------------------

    /** Ejecuta nuevamente las consultas con los filtros seleccionados. */
    @FXML
    private void handleGenerar() {
        if (!Sesion.tienePermiso("ESTADISTICAS_GENERALES")) {
            return;
        }

        recarga.stop();
        cargarDatosDashboard();
    }

    /** Actualiza los periodos disponibles y vuelve a consultar los datos. */
    @FXML
    private void handleActualizar() {
        if (!Sesion.tienePermiso("ESTADISTICAS_GENERALES")) {
            return;
        }

        recarga.stop();
        cargarFiltrosDinamicos();
        cargarDatosDashboard();
    }

    @FXML
    private void handleExportar() {
        if (!Sesion.tienePermiso("ESTADISTICAS_GENERALES")) {
            return;
        }

        // Si hay una actualización pendiente, se aplica antes de exportar.
        if (recarga.getStatus() == Animation.Status.RUNNING) {
            recarga.stop();
            cargarDatosDashboard();
        }

        // Validar el mismo rango que se utilizará en el reporte.
        LocalDate desde = leerFecha(dpDesde);
        LocalDate hasta = leerFecha(dpHasta);

        if (!validarRangoFechas(desde, hasta, false)) {
            mostrarError("Periodo no válido", "El rango debe estar entre el primer movimiento registrado y hoy, "
                    + "y la fecha Desde no puede ser posterior a Hasta.");
            return;
        }

        FileChooser selector = new FileChooser();
        selector.setTitle("Exportar estadísticas generales");

        FileChooser.ExtensionFilter filtroPdf =
                new FileChooser.ExtensionFilter("Documento PDF (*.pdf)", "*.pdf");
        FileChooser.ExtensionFilter filtroExcel =
                new FileChooser.ExtensionFilter("Libro de Excel (*.xlsx)", "*.xlsx");

        selector.getExtensionFilters().addAll(filtroPdf, filtroExcel);
        selector.setSelectedExtensionFilter(filtroPdf);
        selector.setInitialFileName("Estadisticas_Generales_LaRoca.pdf");

        File archivo = selector.showSaveDialog(btnExportar.getScene().getWindow());
        if (archivo == null) {
            return;
        }

        try {
            DatosReporteEstadisticas datos = crearDatosReporte();

            if (selector.getSelectedExtensionFilter() == filtroExcel) {
                archivo = asegurarExtension(archivo, ".xlsx");
                ExportadorEstadisticasExcel.exportar(datos, archivo);
                lblEstado.setText("Excel guardado en " + archivo.getAbsolutePath());
            } else {
                archivo = asegurarExtension(archivo, ".pdf");
                ExportadorEstadisticasPdf.exportar(datos, archivo);
                lblEstado.setText("PDF guardado en " + archivo.getAbsolutePath());
            }

        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
            mostrarError("Error de exportación",
                    "No se pudo generar el reporte: " + e.getMessage());
        }
    }

    private File asegurarExtension(File archivo, String extension) {
        if (archivo.getName().toLowerCase(ESPANOL).endsWith(extension)) {
            return archivo;
        }
        return new File(archivo.getParentFile(), archivo.getName() + extension);
    }

    private DatosReporteEstadisticas crearDatosReporte() {
        LocalDate desde = leerFecha(dpDesde);
        LocalDate hasta = leerFecha(dpHasta);

        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new IllegalStateException("El periodo seleccionado no es válido.");
        }

        String categoria = cmbCategoria.getValue() == null
                ? "Todas" : cmbCategoria.getValue().texto();

        List<DatosReporteEstadisticas.Dato> indicadores = List.of(
                new DatosReporteEstadisticas.Dato("Ventas del periodo", lblVentasPeriodo.getText(), ""),
                new DatosReporteEstadisticas.Dato("Número de ventas", lblNumVentas.getText(), ""),
                new DatosReporteEstadisticas.Dato("Ticket promedio", lblTicketPromedio.getText(), ""),
                new DatosReporteEstadisticas.Dato("Compras del periodo",
                        lblComprasPeriodo.getText(), lblSubCompras.getText()),
                new DatosReporteEstadisticas.Dato("Mermas del periodo",
                        lblMermasPeriodo.getText(), lblSubMermas.getText())
        );

        List<DatosReporteEstadisticas.Seccion> secciones = new ArrayList<>();

        List<DatosReporteEstadisticas.Dato> ventasDia = new ArrayList<>();
        for (XYChart.Series<String, Number> serie : graficoVentasDia.getData()) {
            for (XYChart.Data<String, Number> dato : serie.getData()) {
                ventasDia.add(new DatosReporteEstadisticas.Dato(
                        dato.getXValue(),
                        Formato.dinero(new BigDecimal(dato.getYValue().toString())),
                        ""));
            }
        }
        secciones.add(new DatosReporteEstadisticas.Seccion("Ventas por día", ventasDia));

        List<DatosReporteEstadisticas.Dato> categorias = new ArrayList<>();
        for (Importe dato : tablaCategoria.getItems()) {
            categorias.add(new DatosReporteEstadisticas.Dato(
                    dato.nombre(), Formato.dinero(dato.total()), ""));
        }
        secciones.add(new DatosReporteEstadisticas.Seccion("Ventas por categoría", categorias));

        List<DatosReporteEstadisticas.Dato> metodos = new ArrayList<>();
        for (Importe dato : tablaMetodoPago.getItems()) {
            metodos.add(new DatosReporteEstadisticas.Dato(
                    dato.nombre(), Formato.dinero(dato.total()), ""));
        }
        secciones.add(new DatosReporteEstadisticas.Seccion("Ventas por método de pago", metodos));

        List<DatosReporteEstadisticas.Dato> proveedores = new ArrayList<>();
        for (CompraProveedor dato : tablaComprasProveedor.getItems()) {
            proveedores.add(new DatosReporteEstadisticas.Dato(
                    dato.nombre(), Formato.dinero(dato.total()),
                    dato.compras() + (dato.compras() == 1 ? " compra" : " compras")));
        }
        secciones.add(new DatosReporteEstadisticas.Seccion("Compras por proveedor", proveedores));

        List<DatosReporteEstadisticas.Dato> mermas = new ArrayList<>();
        for (MermaProducto dato : tablaMermasProducto.getItems()) {
            mermas.add(new DatosReporteEstadisticas.Dato(
                    dato.nombre(), Formato.dinero(dato.total()), dato.cantidad() + " unidades"));
        }
        secciones.add(new DatosReporteEstadisticas.Seccion("Mermas por producto", mermas));

        List<DatosReporteEstadisticas.Dato> masVendidos = new ArrayList<>();
        int posicion = 1;
        for (TopProducto dato : tablaTopProductos.getItems()) {
            masVendidos.add(new DatosReporteEstadisticas.Dato(
                    posicion++ + ". " + dato.nombre(), dato.cantidad() + " unidades", ""));
        }
        secciones.add(new DatosReporteEstadisticas.Seccion("Productos más vendidos", masVendidos));

        List<DatosReporteEstadisticas.Dato> stockBajo = new ArrayList<>();
        for (StockBajo dato : tablaStockBajo.getItems()) {
            stockBajo.add(new DatosReporteEstadisticas.Dato(
                    dato.nombre(),
                    "Stock: " + dato.stock(),
                    "Punto de reorden: " + dato.puntoReorden() + " · " + dato.estado()));
        }
        secciones.add(new DatosReporteEstadisticas.Seccion("Productos a reordenar", stockBajo));

        return new DatosReporteEstadisticas(
                desde, hasta, categoria, LocalDate.now(),
                indicadores, secciones);
    }

    @FXML
    private void volverAlMenu() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/laroca/vista/menu.fxml"));
            Parent vista = loader.load();
            Stage ventana = (Stage) btnVolver.getScene().getWindow();
            Navegacion.mostrar(ventana, vista, Navegacion.TITULO_MENU);
        } catch (IOException | IllegalStateException e) {
            e.printStackTrace();
            mostrarError("Error de navegación", "No se pudo regresar al menú principal.\n" + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Datos
    // ------------------------------------------------------------------

    // Con un rango no válido solo se avisa en la barra de estado y se
    // conservan los últimos resultados válidos.
    private void cargarDatosDashboard() {
        LocalDate desde = leerFecha(dpDesde);
        LocalDate hasta = leerFecha(dpHasta);

        // leerFecha puede confirmar la fecha y volver a programar la recarga.
        recarga.stop();

        if (!validarRangoFechas(desde, hasta, true)) {
            return;
        }

        Integer idCategoria = cmbCategoria.getValue() == null ? null : cmbCategoria.getValue().idCategoria();
        Filtro filtro = new Filtro(desde, hasta, idCategoria);

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                lblEstado.setText("No se pudo conectar con la base de datos.");
                return;
            }

            // 1. Ventas (importe del detalle, para poder filtrar por categoría).
            String sqlVentas = "SELECT COUNT(DISTINCT v.id_venta) AS ventas, "
                    + "COALESCE(SUM(dv.cantidad * dv.precio_unitario), 0) AS importe "
                    + filtro.desdeVentas();
            int numVentas = 0;
            BigDecimal importeVentas = BigDecimal.ZERO;

            try (PreparedStatement ps = conexion.prepareStatement(sqlVentas)) {
                filtro.parametrosVentas(ps);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        numVentas = rs.getInt("ventas");
                        importeVentas = rs.getBigDecimal("importe");
                    }
                }
            }

            lblNumVentas.setText(Formato.entero(numVentas));
            lblVentasPeriodo.setText(Formato.dinero(importeVentas));
            lblTicketPromedio.setText(Formato.dinero(numVentas == 0
                    ? BigDecimal.ZERO
                    : importeVentas.divide(BigDecimal.valueOf(numVentas), 2, RoundingMode.HALF_UP)));

            // 2. Compras.
            String sqlCompras = "SELECT COUNT(DISTINCT c.id_compras) AS compras, "
                    + "COALESCE(SUM(d.cantidad * d.precio_compra), 0) AS importe "
                    + "FROM Compras c "
                    + "INNER JOIN detalle_compras d ON d.id_compras = c.id_compras "
                    + "INNER JOIN Productos p ON p.id_producto = d.id_producto "
                    + "WHERE c.fecha >= ? AND c.fecha < ? AND " + activo("c")
                    + filtro.condicionCategoria();

            try (PreparedStatement ps = conexion.prepareStatement(sqlCompras)) {
                filtro.parametrosFecha(ps);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    int numCompras = rs.getInt("compras");
                    lblComprasPeriodo.setText(Formato.dinero(rs.getBigDecimal("importe")));
                    lblSubCompras.setText(numCompras + (numCompras == 1 ? " compra" : " compras"));
                }
            }

            // 3. Compras desglosadas por proveedor.
            String sqlComprasProveedor = "SELECT COALESCE(NULLIF(TRIM(pr.nombre), ''), 'Sin proveedor') AS nombre, "
                    + "COUNT(DISTINCT c.id_compras) AS compras, "
                    + "COALESCE(SUM(d.cantidad * d.precio_compra), 0) AS importe "
                    + "FROM Compras c "
                    + "INNER JOIN Proveedores pr ON pr.id_proveedor = c.id_proveedor "
                    + "INNER JOIN detalle_compras d ON d.id_compras = c.id_compras "
                    + "INNER JOIN Productos p ON p.id_producto = d.id_producto "
                    + "WHERE c.fecha >= ? AND c.fecha < ? AND " + activo("c")
                    + filtro.condicionCategoria()
                    + " GROUP BY pr.id_proveedor, pr.nombre ORDER BY importe DESC, nombre";
            List<CompraProveedor> comprasProveedor = new ArrayList<>();

            try (PreparedStatement ps = conexion.prepareStatement(sqlComprasProveedor)) {
                filtro.parametrosFecha(ps);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        comprasProveedor.add(new CompraProveedor(
                                rs.getString("nombre"), rs.getInt("compras"),
                                rs.getBigDecimal("importe")));
                    }
                }
            }
            tablaComprasProveedor.getItems().setAll(comprasProveedor);

            // 4. Mermas.
            String sqlMermas = "SELECT COALESCE(SUM(d.cantidad * d.precio), 0) AS importe "
                    + "FROM Mermas m "
                    + "INNER JOIN detalle_mermas d ON d.id_merma = m.id_merma "
                    + "INNER JOIN Productos p ON p.id_producto = d.id_producto "
                    + "WHERE m.fecha >= ? AND m.fecha < ? AND " + activo("m")
                    + filtro.condicionCategoria();

            try (PreparedStatement ps = conexion.prepareStatement(sqlMermas)) {
                filtro.parametrosFecha(ps);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    BigDecimal mermas = rs.getBigDecimal("importe");
                    lblMermasPeriodo.setText(Formato.dinero(mermas));
                    lblSubMermas.setText(importeVentas.signum() == 0
                            ? "— de ventas"
                            : mermas.multiply(BigDecimal.valueOf(100))
                                    .divide(importeVentas, 1, RoundingMode.HALF_UP) + "% de ventas");
                }
            }

            // 5. Mermas desglosadas por producto.
            String sqlMermasProducto = "SELECT p.id_producto, p.nombre, "
                    + "COALESCE(SUM(d.cantidad), 0) AS unidades, "
                    + "COALESCE(SUM(d.cantidad * d.precio), 0) AS importe "
                    + "FROM Mermas m "
                    + "INNER JOIN detalle_mermas d ON d.id_merma = m.id_merma "
                    + "INNER JOIN Productos p ON p.id_producto = d.id_producto "
                    + "WHERE m.fecha >= ? AND m.fecha < ? AND " + activo("m")
                    + filtro.condicionCategoria()
                    + " GROUP BY p.id_producto, p.nombre ORDER BY importe DESC, p.nombre";
            List<MermaProducto> mermasProducto = new ArrayList<>();

            try (PreparedStatement ps = conexion.prepareStatement(sqlMermasProducto)) {
                filtro.parametrosFecha(ps);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        mermasProducto.add(new MermaProducto(
                                rs.getString("nombre"), rs.getInt("unidades"),
                                rs.getBigDecimal("importe")));
                    }
                }
            }
            tablaMermasProducto.getItems().setAll(mermasProducto);

            // 6. Ventas por día (por día de calendario, no por hora).
            cargarGrafica(conexion, filtro);

            // 7. Productos más vendidos.
            String sqlTop = "SELECT p.id_producto, p.nombre, SUM(dv.cantidad) AS unidades "
                    + filtro.desdeVentas()
                    + "GROUP BY p.id_producto, p.nombre "
                    + "ORDER BY unidades DESC, p.nombre LIMIT 5";
            List<TopProducto> top = new ArrayList<>();

            try (PreparedStatement ps = conexion.prepareStatement(sqlTop)) {
                filtro.parametrosVentas(ps);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        top.add(new TopProducto(rs.getString("nombre"), rs.getInt("unidades")));
                    }
                }
            }
            tablaTopProductos.getItems().setAll(top);

            // 8. Ventas por categoría.
            String sqlCategorias = "SELECT c.id_categoria, c.nombre, "
                    + "SUM(dv.cantidad * dv.precio_unitario) AS importe "
                    + filtro.desdeVentas("INNER JOIN Categoria c ON c.id_categoria = p.id_categoria ")
                    + "GROUP BY c.id_categoria, c.nombre ORDER BY importe DESC, c.nombre";
            tablaCategoria.getItems().setAll(importes(conexion, sqlCategorias, filtro));

            // 9. Métodos de pago.
            String sqlMetodos = "SELECT mp.id_metodo, mp.nombre, "
                    + "SUM(dv.cantidad * dv.precio_unitario) AS importe "
                    + filtro.desdeVentas("INNER JOIN Metodos_Pago mp ON mp.id_metodo = v.id_metodo ")
                    + "GROUP BY mp.id_metodo, mp.nombre ORDER BY importe DESC, mp.nombre";
            tablaMetodoPago.getItems().setAll(importes(conexion, sqlMetodos, filtro));

            // 10. Productos a reordenar: mismo stock que el Reporte de Inventario.
            String sqlStock = "SELECT p.nombre, p.existencia, p.punto_reorden "
                    + "FROM Productos p "
                    + "WHERE " + activo("p") + " "
                    + "AND p.punto_reorden IS NOT NULL "
                    + "AND p.existencia <= p.punto_reorden"
                    + filtro.condicionCategoria()
                    + " ORDER BY p.existencia - CAST(p.punto_reorden AS SIGNED), p.nombre";
            List<StockBajo> stock = new ArrayList<>();

            try (PreparedStatement ps = conexion.prepareStatement(sqlStock)) {
                if (idCategoria != null) {
                    ps.setInt(1, idCategoria);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        stock.add(new StockBajo(rs.getString("nombre"),
                                rs.getInt("existencia"), rs.getInt("punto_reorden")));
                    }
                }
            }
            tablaStockBajo.getItems().setAll(stock);

            lblEstado.setText("Estadísticas del " + Formato.fecha(desde) + " al " + Formato.fecha(hasta)
                    + " · " + numVentas + (numVentas == 1 ? " venta" : " ventas")
                    + " · " + stock.size() + " por reordenar");

        } catch (SQLException e) {
            e.printStackTrace();
            lblEstado.setText("Error al cargar estadísticas: " + e.getMessage());
            alertaDiferida("Error al cargar estadísticas", e.getMessage());
        }
    }

    private void cargarGrafica(Connection conexion, Filtro filtro) throws SQLException {
        String sql = "SELECT DATE(v.fecha) AS dia, SUM(dv.cantidad * dv.precio_unitario) AS importe "
                + filtro.desdeVentas()
                + "GROUP BY DATE(v.fecha) ORDER BY dia";

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        XYChart.Data<String, Number> mayor = null;
        BigDecimal importeMayor = null;

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            filtro.parametrosVentas(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDate dia = rs.getDate("dia").toLocalDate();
                    BigDecimal importe = rs.getBigDecimal("importe");
                    XYChart.Data<String, Number> dato =
                            new XYChart.Data<>(dia.format(DIA_GRAFICA), importe);
                    serie.getData().add(dato);

                    if (importeMayor == null || importe.compareTo(importeMayor) > 0) {
                        importeMayor = importe;
                        mayor = dato;
                    }
                }
            }
        }

        graficoVentasDia.getData().setAll(List.of(serie));

        int dias = serie.getData().size();
        lblSubVentas.setText(dias + (dias == 1
                ? " día con ventas" : " días con ventas"));

        String rango = Formato.fecha(filtro.desde()) + " – " + Formato.fecha(filtro.hasta());

        if (mayor == null) {
            lblSubGrafico.setText(rango + " · sin ventas en el periodo");
        } else {
            lblSubGrafico.setText(rango + " · el día más alto está resaltado ("
                    + mayor.getXValue() + ": " + Formato.dinero(importeMayor) + ")");

            // Espera a que JavaFX cree los nodos de los puntos para resaltarlo.
            XYChart.Data<String, Number> puntoMayor = mayor;
            Platform.runLater(() -> {
                if (puntoMayor.getNode() != null) {
                    puntoMayor.getNode().getStyleClass().add("punto-destacado");
                }
            });
        }
    }

    private List<Importe> importes(Connection conexion, String sql, Filtro filtro) throws SQLException {
        List<Importe> lista = new ArrayList<>();

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            filtro.parametrosVentas(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Importe(rs.getString("nombre"), rs.getBigDecimal("importe")));
                }
            }
        }

        return lista;
    }

    // Periodo y categoría de una consulta.
    private record Filtro(LocalDate desde, LocalDate hasta, Integer idCategoria) {

        // FROM y WHERE comunes de las consultas sobre ventas activas.
        String desdeVentas() {
            return desdeVentas("");
        }

        // uniones: INNER JOIN adicionales (Categoria, Metodos_Pago).
        String desdeVentas(String uniones) {
            return "FROM Ventas v "
                    + "INNER JOIN detalle_ventas dv ON dv.id_venta = v.id_venta "
                    + "INNER JOIN Productos p ON p.id_producto = dv.id_producto "
                    + uniones
                    + "WHERE v.fecha >= ? AND v.fecha < ? AND " + activo("v")
                    + condicionCategoria() + " ";
        }

        String condicionCategoria() {
            return idCategoria == null ? "" : " AND p.id_categoria = ?";
        }

        // Ventas.fecha es DATETIME.
        void parametrosVentas(PreparedStatement ps) throws SQLException {
            ps.setTimestamp(1, Timestamp.valueOf(desde.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(hasta.plusDays(1).atStartOfDay()));
            if (idCategoria != null) {
                ps.setInt(3, idCategoria);
            }
        }

        // Compras.fecha y Mermas.fecha son DATE.
        void parametrosFecha(PreparedStatement ps) throws SQLException {
            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta.plusDays(1)));
            if (idCategoria != null) {
                ps.setInt(3, idCategoria);
            }
        }
    }

    // ------------------------------------------------------------------
    // Mensajes
    // ------------------------------------------------------------------

    // Al abrir la pantalla la ventana aún no está visible: el aviso espera.
    private void alertaDiferida(String titulo, String mensaje) {
        Platform.runLater(() -> mostrarError(titulo, mensaje));
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje == null || mensaje.isBlank() ? "Ocurrió un error inesperado." : mensaje);

        if (btnVolver.getScene() != null && btnVolver.getScene().getWindow() != null) {
            alerta.initOwner(btnVolver.getScene().getWindow());
        }

        alerta.showAndWait();
    }
}
