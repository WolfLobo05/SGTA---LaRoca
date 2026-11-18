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

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
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
    @FXML private Button btnGenerar;
    @FXML private Button btnActualizar;
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

    @FXML private BarChart<String, Number> graficoVentasDia;

    @FXML private TableView<TopProducto> tablaTopProductos;
    @FXML private TableColumn<TopProducto, String> colTopNombre;
    @FXML private TableColumn<TopProducto, Integer> colTopCantidad;

    @FXML private TableView<Importe> tablaCategoria;
    @FXML private TableColumn<Importe, String> colCatNombre;
    @FXML private TableColumn<Importe, String> colCatTotal;

    @FXML private TableView<Importe> tablaMetodoPago;
    @FXML private TableColumn<Importe, String> colMetodoNombre;
    @FXML private TableColumn<Importe, String> colMetodoTotal;

    @FXML private TableView<StockBajo> tablaStockBajo;
    @FXML private TableColumn<StockBajo, String> colStockProd;
    @FXML private TableColumn<StockBajo, Integer> colStockActual;
    @FXML private TableColumn<StockBajo, Integer> colStockMinimo;
    @FXML private TableColumn<StockBajo, String> colStockEstado;

    private final CategoriaDAO categoriaDAO = new CategoriaDAO();

    // Evita recargar mientras se llenan los combos por código.
    private boolean cargandoFiltros;

    @FXML
    public void initialize() {
        lblFecha.setText(Formato.fecha(LocalDate.now()));

        colTopNombre.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().nombre()));
        colTopCantidad.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().cantidad()));

        colCatNombre.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().nombre()));
        colCatTotal.setCellValueFactory(c -> new ReadOnlyStringWrapper(Formato.dinero(c.getValue().total())));

        colMetodoNombre.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().nombre()));
        colMetodoTotal.setCellValueFactory(c -> new ReadOnlyStringWrapper(Formato.dinero(c.getValue().total())));

        colStockProd.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().nombre()));
        colStockActual.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().stock()));
        colStockMinimo.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().puntoReorden()));
        colStockEstado.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().estado()));

        configurarFecha(dpDesde);
        configurarFecha(dpHasta);

        cmbPeriodo.valueProperty().addListener((obs, antes, periodo) -> {
            if (periodo != null && !cargandoFiltros) {
                dpDesde.setValue(periodo.desde());
                dpHasta.setValue(periodo.hasta());
                cargarDatosDashboard();
            }
        });

        cmbCategoria.valueProperty().addListener((obs, antes, ahora) -> {
            if (ahora != null && !cargandoFiltros) {
                cargarDatosDashboard();
            }
        });

        cargarFiltrosDinamicos();
        cargarDatosDashboard();
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

    // Meses con ventas activas (más el mes actual) y el histórico;
    // categorías de la tabla Categoria.
    private void cargarFiltrosDinamicos() {
        cargandoFiltros = true;

        try {
            Periodo previo = cmbPeriodo.getValue();
            List<Periodo> periodos = new ArrayList<>();
            YearMonth actual = YearMonth.now();
            periodos.add(mes(actual));

            LocalDate primeraFecha = null;

            String sqlMeses = "SELECT DISTINCT YEAR(v.fecha) AS anio, MONTH(v.fecha) AS mes "
                    + "FROM Ventas v WHERE v.fecha IS NOT NULL AND " + activo("v") + " "
                    + "ORDER BY anio DESC, mes DESC";

            String sqlPrimera = "SELECT MIN(f) FROM ("
                    + "SELECT MIN(DATE(v.fecha)) AS f FROM Ventas v "
                    + "UNION ALL SELECT MIN(c.fecha) FROM Compras c) t";

            try (Connection conexion = Conexion.conectar()) {
                if (conexion == null) {
                    lblEstado.setText("No se pudo conectar con la base de datos.");
                } else {
                    try (PreparedStatement ps = conexion.prepareStatement(sqlMeses);
                         ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            YearMonth ym = YearMonth.of(rs.getInt("anio"), rs.getInt("mes"));
                            if (!ym.equals(actual)) {
                                periodos.add(mes(ym));
                            }
                        }
                    }

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

            LocalDate hoy = LocalDate.now();
            LocalDate inicio = primeraFecha == null || primeraFecha.isAfter(hoy) ? hoy : primeraFecha;
            periodos.add(new Periodo("Histórico (general)", inicio, hoy));

            cmbPeriodo.getItems().setAll(periodos);
            Periodo seleccion = periodos.get(0);
            if (previo != null) {
                for (Periodo p : periodos) {
                    if (p.etiqueta().equals(previo.etiqueta())) {
                        seleccion = p;
                    }
                }
            }
            cmbPeriodo.setValue(seleccion);

            if (previo == null) {
                dpDesde.setValue(seleccion.desde());
                dpHasta.setValue(seleccion.hasta());
            }

            // Categorías: activas y, al final, inactivas.
            OpcionCategoria previa = cmbCategoria.getValue();
            List<OpcionCategoria> categorias = new ArrayList<>();
            categorias.add(new OpcionCategoria(null, "Todas"));
            List<OpcionCategoria> inactivas = new ArrayList<>();

            for (Categoria categoria : categoriaDAO.listar()) {
                if (Estados.esActivo(categoria.getEstado())) {
                    categorias.add(new OpcionCategoria(categoria.getIdCategoria(), categoria.getNombre()));
                } else {
                    inactivas.add(new OpcionCategoria(categoria.getIdCategoria(),
                            categoria.getNombre() + " (inactiva)"));
                }
            }
            categorias.addAll(inactivas);

            cmbCategoria.getItems().setAll(categorias);
            OpcionCategoria elegida = categorias.get(0);
            if (previa != null) {
                for (OpcionCategoria opcion : categorias) {
                    if (opcion.texto().equals(previa.texto())) {
                        elegida = opcion;
                    }
                }
            }
            cmbCategoria.setValue(elegida);

        } finally {
            cargandoFiltros = false;
        }
    }

    private static Periodo mes(YearMonth ym) {
        String nombre = ym.getMonth().getDisplayName(TextStyle.FULL, ESPANOL);
        String etiqueta = nombre.substring(0, 1).toUpperCase(ESPANOL) + nombre.substring(1) + " " + ym.getYear();
        LocalDate fin = ym.atEndOfMonth();
        LocalDate hoy = LocalDate.now();
        return new Periodo(etiqueta, ym.atDay(1), fin.isAfter(hoy) && !ym.atDay(1).isAfter(hoy) ? hoy : fin);
    }

    private static String activo(String alias) {
        return String.format(ACTIVO, alias);
    }

    // ------------------------------------------------------------------
    // Acciones
    // ------------------------------------------------------------------

    @FXML
    private void handleGenerar() {
        cargarDatosDashboard();
    }

    @FXML
    private void handleActualizar() {
        cargarFiltrosDinamicos();
        cargarDatosDashboard();
    }

    @FXML
    private void handleExportar() {
        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar reporte de estadísticas");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo de texto (*.txt)", "*.txt"));
        selector.setInitialFileName("Reporte_Estadisticas_LaRoca.txt");

        File archivo = selector.showSaveDialog(btnExportar.getScene().getWindow());

        if (archivo == null) {
            return;
        }

        try (PrintWriter pw = new PrintWriter(archivo, StandardCharsets.UTF_8)) {
            pw.println("==========================================");
            pw.println("  SISTEMA ABARROTES 'LA ROCA' - ESTADÍSTICAS");
            pw.println("==========================================");
            pw.println("Periodo: " + Formato.fecha(dpDesde.getValue()) + " al " + Formato.fecha(dpHasta.getValue()));
            pw.println("Categoría: " + cmbCategoria.getValue());
            pw.println("------------------------------------------");
            pw.println("Ventas del periodo: " + lblVentasPeriodo.getText());
            pw.println("Número de ventas: " + lblNumVentas.getText());
            pw.println("Ticket promedio: " + lblTicketPromedio.getText());
            pw.println("Compras del periodo: " + lblComprasPeriodo.getText() + " (" + lblSubCompras.getText() + ")");
            pw.println("Mermas del periodo: " + lblMermasPeriodo.getText() + " (" + lblSubMermas.getText() + ")");
            pw.println("------------------------------------------");
            pw.println("Productos más vendidos:");
            for (TopProducto p : tablaTopProductos.getItems()) {
                pw.println("  " + p.nombre() + ": " + p.cantidad());
            }
            pw.println("Productos a reordenar:");
            for (StockBajo s : tablaStockBajo.getItems()) {
                pw.println("  " + s.nombre() + ": stock " + s.stock() + " / reorden " + s.puntoReorden());
            }
            pw.println("==========================================");

            lblEstado.setText("Reporte guardado en " + archivo.getAbsolutePath());

        } catch (IOException e) {
            e.printStackTrace();
            mostrarError("Error de exportación", "No se pudo guardar el archivo: " + e.getMessage());
        }
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

    private void cargarDatosDashboard() {
        LocalDate desde = leerFecha(dpDesde);
        LocalDate hasta = leerFecha(dpHasta);

        if (desde == null || hasta == null) {
            lblEstado.setText("Escribe las fechas Desde y Hasta con el formato dd/mm/aaaa.");
            alertaDiferida("Fecha no válida", "Escribe las fechas Desde y Hasta con el formato dd/mm/aaaa.");
            return;
        }

        if (hasta.isBefore(desde)) {
            lblEstado.setText("La fecha Desde no puede ser posterior a la fecha Hasta.");
            alertaDiferida("Periodo no válido", "La fecha Desde no puede ser posterior a la fecha Hasta.");
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

            // 3. Mermas.
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

            // 4. Ventas por día (por día de calendario, no por hora).
            cargarGrafica(conexion, filtro);

            // 5. Productos más vendidos.
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

            // 6. Ventas por categoría.
            String sqlCategorias = "SELECT c.id_categoria, c.nombre, "
                    + "SUM(dv.cantidad * dv.precio_unitario) AS importe "
                    + filtro.desdeVentas("INNER JOIN Categoria c ON c.id_categoria = p.id_categoria ")
                    + "GROUP BY c.id_categoria, c.nombre ORDER BY importe DESC, c.nombre";
            tablaCategoria.getItems().setAll(importes(conexion, sqlCategorias, filtro));

            // 7. Métodos de pago.
            String sqlMetodos = "SELECT mp.id_metodo, mp.nombre, "
                    + "SUM(dv.cantidad * dv.precio_unitario) AS importe "
                    + filtro.desdeVentas("INNER JOIN Metodos_Pago mp ON mp.id_metodo = v.id_metodo ")
                    + "GROUP BY mp.id_metodo, mp.nombre ORDER BY importe DESC, mp.nombre";
            tablaMetodoPago.getItems().setAll(importes(conexion, sqlMetodos, filtro));

            // 8. Productos a reordenar: mismo stock que el Reporte de Inventario.
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
                    XYChart.Data<String, Number> dato = new XYChart.Data<>(dia.format(DIA_GRAFICA), importe);
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
        lblSubVentas.setText(dias + (dias == 1 ? " día con ventas" : " días con ventas"));

        String rango = Formato.fecha(filtro.desde()) + " – " + Formato.fecha(filtro.hasta());

        if (mayor == null) {
            lblSubGrafico.setText(rango + " · sin ventas en el periodo");
        } else {
            lblSubGrafico.setText(rango + " · el día más alto está resaltado ("
                    + mayor.getXValue() + ": " + Formato.dinero(importeMayor) + ")");

            if (mayor.getNode() != null) {
                mayor.getNode().getStyleClass().add("barra-destacada");
            }
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

        if (btnGenerar.getScene() != null && btnGenerar.getScene().getWindow() != null) {
            alerta.initOwner(btnGenerar.getScene().getWindow());
        }

        alerta.showAndWait();
    }
}
