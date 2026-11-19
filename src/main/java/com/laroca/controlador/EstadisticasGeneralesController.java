package com.laroca.controlador;

import com.laroca.conexion.Conexion;

import java.io.File;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class EstadisticasGeneralesController {

    @FXML private ComboBox<String> cmbPeriodo;
    @FXML private ComboBox<String> cmbCategoria;
    @FXML private TextField txtDesde;
    @FXML private TextField txtHasta;
    @FXML private Button btnVolver;
    @FXML private Button btnGenerar;
    @FXML private Button btnActualizar;
    @FXML private Button btnExportar;

    @FXML private Label lblVentasPeriodo;
    @FXML private Label lblNumVentas;
    @FXML private Label lblTicketPromedio;
    @FXML private Label lblComprasPeriodo;
    @FXML private Label lblSubCompras;
    @FXML private Label lblMermasPeriodo;
    @FXML private Label lblEstado;

    @FXML private BarChart<String, Number> graficoVentasDia;

    @FXML private TableView<TopProducto> tablaTopProductos;
    @FXML private TableColumn<TopProducto, String> colTopNombre;
    @FXML private TableColumn<TopProducto, Integer> colTopCantidad;

    @FXML private TableColumn<CategoriaItem, String> colCatNombre;
    @FXML private TableColumn<CategoriaItem, Double> colCatTotal;
    @FXML private TableView<CategoriaItem> tablaCategoria;

    @FXML private TableColumn<MetodoPagoItem, String> colMetodoNombre;
    @FXML private TableColumn<MetodoPagoItem, Double> colMetodoTotal;
    @FXML private TableView<MetodoPagoItem> tablaMetodoPago;

    @FXML private TableView<StockBajoItem> tablaStockBajo;
    @FXML private TableColumn<StockBajoItem, String> colStockProd;
    @FXML private TableColumn<StockBajoItem, Integer> colStockActual;
    @FXML private TableColumn<StockBajoItem, Integer> colStockMinimo;
    @FXML private TableColumn<StockBajoItem, String> colStockEstado;

    @FXML
    public void initialize() {
        colTopNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colTopCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));

        colCatNombre.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colCatTotal.setCellValueFactory(new PropertyValueFactory<>("total"));

        colMetodoNombre.setCellValueFactory(new PropertyValueFactory<>("metodo"));
        colMetodoTotal.setCellValueFactory(new PropertyValueFactory<>("total"));

        colStockProd.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colStockActual.setCellValueFactory(new PropertyValueFactory<>("stockActual"));
        colStockMinimo.setCellValueFactory(new PropertyValueFactory<>("puntoReorden"));
        colStockEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        cargarFiltrosDinamicos();

        cmbPeriodo.setOnAction(event -> {
            String seleccionado = cmbPeriodo.getValue();
            if (seleccionado != null && !seleccionado.isEmpty()) {
                aplicarRangoFechasPorSeleccion(seleccionado);
                cargarDatosDashboard();
            }
        });

        cmbCategoria.setOnAction(event -> {
            if (cmbCategoria.getValue() != null) {
                cargarDatosDashboard();
            }
        });

        cargarDatosDashboard();
    }

    private void cargarFiltrosDinamicos() {
        try (Connection conn = Conexion.conectar()) {

            if (conn == null) {
                if (lblEstado != null) {
                    lblEstado.setText("No se pudo conectar a la base de datos.");
                }
                return;
            }

            ObservableList<String> periodos = FXCollections.observableArrayList();
            String qMeses = "SELECT DISTINCT fecha FROM Ventas ORDER BY fecha DESC";

            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(qMeses)) {

                while (rs.next()) {
                    String fechaStr = rs.getString(1);

                    if (fechaStr != null && fechaStr.length() >= 7) {
                        int anio = Integer.parseInt(fechaStr.substring(0, 4));
                        int mes = Integer.parseInt(fechaStr.substring(5, 7));

                        LocalDate fechaLocal = LocalDate.of(anio, mes, 1);
                        String nombreMes = fechaLocal.getMonth()
                                .getDisplayName(TextStyle.FULL, new Locale("es", "ES"));

                        String etiqueta = capitalize(nombreMes) + " " + anio;

                        if (!periodos.contains(etiqueta)) {
                            periodos.add(etiqueta);
                        }
                    }
                }
            }

            if (periodos.isEmpty()) {
                LocalDate hoy = LocalDate.now();
                String mesActual = capitalize(
                        hoy.getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "ES"))
                );
                periodos.add(mesActual + " " + hoy.getYear());
            }

            periodos.add("Histórico (General)");

            String seleccionadoPrevio = cmbPeriodo.getValue();
            cmbPeriodo.setItems(periodos);

            if (seleccionadoPrevio != null && periodos.contains(seleccionadoPrevio)) {
                cmbPeriodo.setValue(seleccionadoPrevio);
            } else {
                cmbPeriodo.setValue(periodos.get(0));
                aplicarRangoFechasPorSeleccion(periodos.get(0));
            }

            String categoriaPrevia = cmbCategoria.getValue();
            ObservableList<String> categorias =
                    FXCollections.observableArrayList("Todas");

            String qCat = "SELECT nombre FROM Categoria ORDER BY nombre";

            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(qCat)) {

                while (rs.next()) {
                    categorias.add(rs.getString(1));
                }
            }

            cmbCategoria.setItems(categorias);

            if (categoriaPrevia != null && categorias.contains(categoriaPrevia)) {
                cmbCategoria.setValue(categoriaPrevia);
            } else {
                cmbCategoria.setValue("Todas");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            if (lblEstado != null) {
                lblEstado.setText("Error al cargar filtros dinámicos de la BD: " + e.getMessage());
            }
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private void aplicarRangoFechasPorSeleccion(String seleccion) {
        if (seleccion.contains("Histórico")) {
            txtDesde.setText("2020-01-01");
            txtHasta.setText(LocalDate.now().toString());
            return;
        }

        try {
            String[] partes = seleccion.split(" ");

            if (partes.length >= 2) {
                String nombreMes = partes[0].toLowerCase();
                int anio = Integer.parseInt(partes[1]);
                int mesNum = obtenerNumeroMes(nombreMes);

                if (mesNum > 0) {
                    YearMonth ym = YearMonth.of(anio, mesNum);
                    LocalDate inicio = ym.atDay(1);
                    LocalDate fin = ym.atEndOfMonth();

                    txtDesde.setText(inicio.toString());
                    txtHasta.setText(fin.toString());
                }
            }
        } catch (Exception e) {
            LocalDate hoy = LocalDate.now();
            txtDesde.setText(hoy.withDayOfMonth(1).toString());
            txtHasta.setText(hoy.toString());
        }
    }

    private int obtenerNumeroMes(String mesStr) {
        switch (mesStr) {
            case "enero": return 1;
            case "febrero": return 2;
            case "marzo": return 3;
            case "abril": return 4;
            case "mayo": return 5;
            case "junio": return 6;
            case "julio": return 7;
            case "agosto": return 8;
            case "septiembre":
            case "setiembre": return 9;
            case "octubre": return 10;
            case "noviembre": return 11;
            case "diciembre": return 12;
            default: return 1;
        }
    }

    @FXML
    private void handleGenerar() {
        cargarDatosDashboard();

        if (lblEstado != null) {
            lblEstado.setText("Estadísticas generadas del "
                    + txtDesde.getText() + " al " + txtHasta.getText());
        }
    }

    @FXML
    private void handleActualizar() {
        cargarFiltrosDinamicos();
        cargarDatosDashboard();

        if (lblEstado != null) {
            lblEstado.setText("Datos sincronizados con éxito desde MySQL a las "
                    + java.time.LocalTime.now().withNano(0));
        }
    }

    @FXML
    private void handleExportar() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Reporte de Estadísticas");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivo de texto (*.txt)", "*.txt")
        );
        fileChooser.setInitialFileName("Reporte_Estadisticas_LaRoca.txt");

        Stage stage = (Stage) btnExportar.getScene().getWindow();
        File archivo = fileChooser.showSaveDialog(stage);

        if (archivo != null) {
            try (PrintWriter pw = new PrintWriter(archivo)) {
                pw.println("==========================================");
                pw.println("  SISTEMA ABARROTES 'LA ROCA' - ESTADÍSTICAS");
                pw.println("==========================================");
                pw.println("Periodo: " + txtDesde.getText() + " al " + txtHasta.getText());
                pw.println("Categoría: " + cmbCategoria.getValue());
                pw.println("------------------------------------------");
                pw.println("Ventas del Periodo: " + lblVentasPeriodo.getText());
                pw.println("Número de Ventas: " + lblNumVentas.getText());
                pw.println("Ticket Promedio: " + lblTicketPromedio.getText());
                pw.println("Compras del Periodo: " + lblComprasPeriodo.getText());
                pw.println("Mermas del Periodo: " + lblMermasPeriodo.getText());
                pw.println("==========================================");
                pw.println("Reporte generado con éxito.");

                Alert alerta = new Alert(Alert.AlertType.INFORMATION);
                alerta.setTitle("Exportación Exitosa");
                alerta.setHeaderText(null);
                alerta.setContentText("El reporte se ha guardado correctamente en:\n"
                        + archivo.getAbsolutePath());
                alerta.showAndWait();

            } catch (Exception e) {
                e.printStackTrace();

                Alert alerta = new Alert(Alert.AlertType.ERROR);
                alerta.setTitle("Error de Exportación");
                alerta.setHeaderText(null);
                alerta.setContentText("No se pudo guardar el archivo: " + e.getMessage());
                alerta.showAndWait();
            }
        }
    }

    @FXML
    private void volverAlMenu() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/laroca/vista/menu.fxml")
            );

            Parent vista = loader.load();
            Stage ventana = (Stage) btnVolver.getScene().getWindow();

            Navegacion.mostrar(ventana, vista, "La Roca - Menú Principal");

        } catch (Exception e) {
            e.printStackTrace();

            if (lblEstado != null) {
                lblEstado.setText("Error al volver al menú principal.");
            }
        }
    }

    private void cargarDatosDashboard() {
        String desde = txtDesde.getText();
        String hasta = txtHasta.getText();
        String categoriaSeleccionada = cmbCategoria.getValue();

        try (Connection conn = Conexion.conectar()) {

            if (conn == null) {
                if (lblEstado != null) {
                    lblEstado.setText("No se pudo conectar a la base de datos.");
                }
                return;
            }

            // 1. Cargar KPIs de Ventas filtrando por fechas y categoría
            String qVentas;
            PreparedStatement pstVentas;

            if (categoriaSeleccionada == null || categoriaSeleccionada.equals("Todas")) {
                qVentas = "SELECT COUNT(DISTINCT v.id_venta), SUM(v.total) "
                        + "FROM Ventas v WHERE v.fecha BETWEEN ? AND ?";

                pstVentas = conn.prepareStatement(qVentas);
                pstVentas.setString(1, desde);
                pstVentas.setString(2, hasta);

            } else {
                qVentas = "SELECT COUNT(DISTINCT v.id_venta), "
                        + "SUM(dv.cantidad * dv.precio_unitario) "
                        + "FROM Ventas v "
                        + "JOIN detalle_ventas dv ON v.id_venta = dv.id_venta "
                        + "JOIN Productos p ON dv.id_producto = p.id_producto "
                        + "JOIN Categoria c ON p.id_categoria = c.id_categoria "
                        + "WHERE v.fecha BETWEEN ? AND ? AND c.nombre = ?";

                pstVentas = conn.prepareStatement(qVentas);
                pstVentas.setString(1, desde);
                pstVentas.setString(2, hasta);
                pstVentas.setString(3, categoriaSeleccionada);
            }

            try (PreparedStatement pst = pstVentas;
                 ResultSet rs = pst.executeQuery()) {

                if (rs.next()) {
                    int numVentas = rs.getInt(1);
                    double sumVentas = rs.getDouble(2);
                    double ticketProm = numVentas > 0 ? sumVentas / numVentas : 0.0;

                    lblNumVentas.setText(String.valueOf(numVentas));
                    lblVentasPeriodo.setText(String.format("$%,.2f", sumVentas));
                    lblTicketPromedio.setText(String.format("$%,.2f", ticketProm));

                } else {
                    lblNumVentas.setText("0");
                    lblVentasPeriodo.setText("$0.00");
                    lblTicketPromedio.setText("$0.00");
                }
            }

            // 2. Cargar KPIs de Compras
            String qCompras =
                    "SELECT COUNT(*), SUM(total) FROM Compras WHERE fecha BETWEEN ? AND ?";

            try (PreparedStatement pst = conn.prepareStatement(qCompras)) {
                pst.setString(1, desde);
                pst.setString(2, hasta);

                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) {
                        int numCom = rs.getInt(1);
                        double sumCom = rs.getDouble(2);

                        lblComprasPeriodo.setText(String.format("$%,.2f", sumCom));
                        lblSubCompras.setText(numCom + " compras");
                    } else {
                        lblComprasPeriodo.setText("$0.00");
                        lblSubCompras.setText("0 compras");
                    }
                }
            }

            // 3. Cargar KPIs de Mermas
            try {
                String qMermas = "SELECT COALESCE(SUM(dm.cantidad * dm.precio), 0) "
                        + "FROM detalle_mermas dm "
                        + "JOIN Mermas m ON dm.id_merma = m.id_merma "
                        + "WHERE m.fecha BETWEEN ? AND ?";

                try (PreparedStatement pst = conn.prepareStatement(qMermas)) {
                    pst.setString(1, desde);
                    pst.setString(2, hasta);

                    try (ResultSet rs = pst.executeQuery()) {
                        if (rs.next()) {
                            lblMermasPeriodo.setText(String.format("$%,.2f", rs.getDouble(1)));
                        }
                    }
                }
            } catch (Exception e) {
                lblMermasPeriodo.setText("$0.00");
            }

            // 4. Cargar Gráfica de Ventas por Día
            graficoVentasDia.getData().clear();
            XYChart.Series<String, Number> serie = new XYChart.Series<>();

            String qGrafico =
                    "SELECT fecha, SUM(total) FROM Ventas "
                    + "WHERE fecha BETWEEN ? AND ? GROUP BY fecha ORDER BY fecha";

            try (PreparedStatement pst = conn.prepareStatement(qGrafico)) {
                pst.setString(1, desde);
                pst.setString(2, hasta);

                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        serie.getData().add(
                                new XYChart.Data<>(rs.getString(1), rs.getDouble(2))
                        );
                    }
                }
            }

            graficoVentasDia.getData().add(serie);

            // 5. Cargar Top Productos Más Vendidos
            ObservableList<TopProducto> topProds =
                    FXCollections.observableArrayList();

            String qTop = "SELECT p.nombre, SUM(dv.cantidad) as total_cant "
                    + "FROM detalle_ventas dv "
                    + "JOIN Ventas v ON dv.id_venta = v.id_venta "
                    + "JOIN Productos p ON dv.id_producto = p.id_producto "
                    + "WHERE v.fecha BETWEEN ? AND ? "
                    + "GROUP BY p.id_producto ORDER BY total_cant DESC LIMIT 5";

            try (PreparedStatement pst = conn.prepareStatement(qTop)) {
                pst.setString(1, desde);
                pst.setString(2, hasta);

                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        topProds.add(new TopProducto(rs.getString(1), rs.getInt(2)));
                    }
                }
            }

            tablaTopProductos.setItems(topProds);

            // 6. Cargar Ventas por Categoría
            ObservableList<CategoriaItem> cats =
                    FXCollections.observableArrayList();

            String qCat = "SELECT c.nombre, SUM(dv.cantidad * dv.precio_unitario) as importe "
                    + "FROM detalle_ventas dv "
                    + "JOIN Ventas v ON dv.id_venta = v.id_venta "
                    + "JOIN Productos p ON dv.id_producto = p.id_producto "
                    + "JOIN Categoria c ON p.id_categoria = c.id_categoria "
                    + "WHERE v.fecha BETWEEN ? AND ? GROUP BY c.id_categoria";

            try (PreparedStatement pst = conn.prepareStatement(qCat)) {
                pst.setString(1, desde);
                pst.setString(2, hasta);

                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        cats.add(new CategoriaItem(rs.getString(1), rs.getDouble(2)));
                    }
                }
            }

            tablaCategoria.setItems(cats);

            // 7. Cargar Métodos de Pago
            ObservableList<MetodoPagoItem> metodos =
                    FXCollections.observableArrayList();

            String qMet = "SELECT mp.nombre, SUM(v.total) "
                    + "FROM Ventas v "
                    + "JOIN Metodos_Pago mp ON v.id_metodo = mp.id_metodo "
                    + "WHERE v.fecha BETWEEN ? AND ? GROUP BY mp.id_metodo";

            try (PreparedStatement pst = conn.prepareStatement(qMet)) {
                pst.setString(1, desde);
                pst.setString(2, hasta);

                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        metodos.add(new MetodoPagoItem(rs.getString(1), rs.getDouble(2)));
                    }
                }
            }

            tablaMetodoPago.setItems(metodos);

            // 8. Cargar Productos con Stock Bajo
            ObservableList<StockBajoItem> stockBajo =
                    FXCollections.observableArrayList();

            String qStock = "SELECT p.nombre, p.punto_reorden, "
                    + "COALESCE((SELECT SUM(dc.cantidad) FROM detalle_compras dc "
                    + "WHERE dc.id_producto = p.id_producto), 0) - "
                    + "COALESCE((SELECT SUM(dv.cantidad) FROM detalle_ventas dv "
                    + "WHERE dv.id_producto = p.id_producto), 0) as stock_act "
                    + "FROM Productos p HAVING stock_act <= p.punto_reorden";

            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(qStock)) {

                while (rs.next()) {
                    String nombre = rs.getString(1);
                    int reorden = rs.getInt(2);
                    int actual = rs.getInt(3);

                    String estado = actual <= 3 ? "Stock bajo" : "Por agotarse";

                    stockBajo.add(new StockBajoItem(nombre, actual, reorden, estado));
                }
            }

            tablaStockBajo.setItems(stockBajo);

        } catch (SQLException e) {
            e.printStackTrace();

            if (lblEstado != null) {
                lblEstado.setText("Error al cargar estadísticas: " + e.getMessage());
            }
        }
    }

    // Clases modelo auxiliares para las tablas
    public static class TopProducto {
        private String nombre;
        private int cantidad;

        public TopProducto(String n, int c) {
            this.nombre = n;
            this.cantidad = c;
        }

        public String getNombre() { return nombre; }
        public int getCantidad() { return cantidad; }
    }

    public static class CategoriaItem {
        private String categoria;
        private double total;

        public CategoriaItem(String c, double t) {
            this.categoria = c;
            this.total = t;
        }

        public String getCategoria() { return categoria; }
        public double getTotal() { return total; }
    }

    public static class MetodoPagoItem {
        private String metodo;
        private double total;

        public MetodoPagoItem(String m, double t) {
            this.metodo = m;
            this.total = t;
        }

        public String getMetodo() { return metodo; }
        public double getTotal() { return total; }
    }

    public static class StockBajoItem {
        private String nombre;
        private int stockActual;
        private int puntoReorden;
        private String estado;

        public StockBajoItem(String n, int s, int p, String e) {
            this.nombre = n;
            this.stockActual = s;
            this.puntoReorden = p;
            this.estado = e;
        }

        public String getNombre() { return nombre; }
        public int getStockActual() { return stockActual; }
        public int getPuntoReorden() { return puntoReorden; }
        public String getEstado() { return estado; }
    }
}
