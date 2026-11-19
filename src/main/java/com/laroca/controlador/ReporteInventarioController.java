package com.laroca.controlador;

import com.laroca.conexion.Conexion;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.print.PrinterJob;

public class ReporteInventarioController {

    @FXML private BorderPane panelReporte;
    @FXML private ComboBox<String> cmbCategoria;
    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private TableView<ProductoReporte> tablaReporte;

    @FXML private TableColumn<ProductoReporte, String> colCodigo;
    @FXML private TableColumn<ProductoReporte, String> colProducto;
    @FXML private TableColumn<ProductoReporte, String> colCategoria;
    @FXML private TableColumn<ProductoReporte, String> colUnidad;
    @FXML private TableColumn<ProductoReporte, Integer> colEntradas;
    @FXML private TableColumn<ProductoReporte, Integer> colSalidas;
    @FXML private TableColumn<ProductoReporte, Integer> colStock;
    @FXML private TableColumn<ProductoReporte, Double> colPrecio;
    @FXML private TableColumn<ProductoReporte, Double> colValor;
    @FXML private TableColumn<ProductoReporte, String> colReorden;

    @FXML private Label lblProductos;
    @FXML private Label lblExistencias;
    @FXML private Label lblValor;
    @FXML private Label lblReorden;
    @FXML private Label lblEstado;

    private final ObservableList<ProductoReporte> datos =
            FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configurarTabla();
        cargarCategorias();

        dpDesde.setValue(LocalDate.now().withDayOfMonth(1));
        dpHasta.setValue(LocalDate.now());

        generarReporte();
    }

    private void configurarTabla() {
        colCodigo.setCellValueFactory(
                new PropertyValueFactory<>("codigo"));
        colProducto.setCellValueFactory(
                new PropertyValueFactory<>("producto"));
        colCategoria.setCellValueFactory(
                new PropertyValueFactory<>("categoria"));
        colUnidad.setCellValueFactory(
                new PropertyValueFactory<>("unidad"));
        colEntradas.setCellValueFactory(
                new PropertyValueFactory<>("entradas"));
        colSalidas.setCellValueFactory(
                new PropertyValueFactory<>("salidas"));
        colStock.setCellValueFactory(
                new PropertyValueFactory<>("stock"));
        colPrecio.setCellValueFactory(
                new PropertyValueFactory<>("precio"));
        colValor.setCellValueFactory(
                new PropertyValueFactory<>("valor"));
        colReorden.setCellValueFactory(
                new PropertyValueFactory<>("reorden"));

        tablaReporte.setItems(datos);
    }

    private void cargarCategorias() {
        cmbCategoria.getItems().clear();
        cmbCategoria.getItems().add("Todas");

        String sql = "SELECT nombre FROM Categoria ORDER BY nombre";

        try (Connection con = Conexion.conectar()) {
            if (con == null) return;

            try (PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    cmbCategoria.getItems().add(rs.getString("nombre"));
                }
            }

        } catch (SQLException e) {
            mostrarError("No se pudieron cargar las categorías: "
                    + e.getMessage());
        }

        cmbCategoria.setValue("Todas");
    }

    @FXML
    private void generarReporte() {
        if (dpDesde.getValue() == null || dpHasta.getValue() == null) {
            mostrarError("Selecciona las fechas del reporte.");
            return;
        }

        if (dpDesde.getValue().isAfter(dpHasta.getValue())) {
            mostrarError("La fecha inicial no puede ser posterior a la final.");
            return;
        }

        datos.clear();

        String sql =
            "SELECT p.id_producto, p.codigo_barras, p.nombre, " +
            "c.nombre AS categoria, p.id_unidad, p.precio, " +
            "p.existencia, p.punto_reorden, " +

            "(SELECT COALESCE(SUM(dc.cantidad),0) " +
            " FROM detalle_compras dc " +
            " INNER JOIN Compras co ON co.id_compras = dc.id_compras " +
            " WHERE dc.id_producto = p.id_producto " +
            " AND co.fecha >= ? AND co.fecha < DATE_ADD(?, INTERVAL 1 DAY)) " +
            " AS entradas, " +

            "((SELECT COALESCE(SUM(dv.cantidad),0) " +
            " FROM detalle_ventas dv " +
            " INNER JOIN Ventas v ON v.id_venta = dv.id_venta " +
            " WHERE dv.id_producto = p.id_producto " +
            " AND v.fecha >= ? AND v.fecha < DATE_ADD(?, INTERVAL 1 DAY)) " +

            "+ (SELECT COALESCE(SUM(dm.cantidad),0) " +
            " FROM detalle_mermas dm " +
            " INNER JOIN Mermas m ON m.id_merma = dm.id_merma " +
            " WHERE dm.id_producto = p.id_producto " +
            " AND m.fecha >= ? AND m.fecha < DATE_ADD(?, INTERVAL 1 DAY))) " +
            " AS salidas " +

            "FROM Productos p " +
            "LEFT JOIN Categoria c ON c.id_categoria = p.id_categoria " +
            "WHERE (? = 'Todas' OR c.nombre = ?) " +
            "ORDER BY p.nombre";

        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();
        String categoria = cmbCategoria.getValue() == null
                ? "Todas" : cmbCategoria.getValue();

        try (Connection con = Conexion.conectar()) {
            if (con == null) {
                mostrarError("No fue posible conectar con la base de datos.");
                return;
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setDate(1, java.sql.Date.valueOf(desde));
                ps.setDate(2, java.sql.Date.valueOf(hasta));
                ps.setDate(3, java.sql.Date.valueOf(desde));
                ps.setDate(4, java.sql.Date.valueOf(hasta));
                ps.setDate(5, java.sql.Date.valueOf(desde));
                ps.setDate(6, java.sql.Date.valueOf(hasta));
                ps.setString(7, categoria);
                ps.setString(8, categoria);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String nombre = rs.getString("nombre");
                        String cat = rs.getString("categoria");
                        String codigo = rs.getString("codigo_barras");

                        int entradas = rs.getInt("entradas");
                        int salidas = rs.getInt("salidas");
                        int stock = rs.getInt("existencia");
                        int minimo = rs.getInt("punto_reorden");

                        double precio = rs.getDouble("precio");
                        double valor = stock * precio;

                        datos.add(new ProductoReporte(
                                codigo == null ? "" : codigo,
                                nombre,
                                cat == null ? "" : cat,
                                String.valueOf(rs.getInt("id_unidad")),
                                entradas,
                                salidas,
                                stock,
                                precio,
                                valor,
                                stock <= minimo ? "Reordenar" : "No"
                        ));
                    }
                }
            }

            actualizarResumen();
            lblEstado.setText("Reporte generado: "
                    + datos.size() + " productos.");

        } catch (SQLException e) {
            mostrarError("Error al generar el reporte: " + e.getMessage());
        }
    }

    private void actualizarResumen() {
        int unidades = 0;
        int reorden = 0;
        double valor = 0;

        for (ProductoReporte p : datos) {
            unidades += p.getStock();
            valor += p.getValor();

            if (p.getReorden().equals("Reordenar")) {
                reorden++;
            }
        }

        lblProductos.setText(String.valueOf(datos.size()));
        lblExistencias.setText(String.valueOf(unidades));
        lblValor.setText(String.format("$%,.2f", valor));
        lblReorden.setText(String.valueOf(reorden));
    }

    @FXML
    private void actualizarReporte() {
        generarReporte();
    }

    @FXML
    private void exportarExcel() {
        if (datos.isEmpty()) {
            mostrarError("No hay información para exportar.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar reporte de inventario");
        chooser.setInitialFileName("Reporte_Inventario.csv");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivo CSV", "*.csv"));

        File archivo = chooser.showSaveDialog(
                tablaReporte.getScene().getWindow());

        if (archivo == null) return;

        try (PrintWriter writer = new PrintWriter(
                archivo, StandardCharsets.UTF_8)) {

            writer.println("\uFEFFCódigo,Producto,Categoría,Unidad,"
                    + "Entradas,Salidas,Stock,Precio,Valor,Punto de reorden");

            for (ProductoReporte p : datos) {
                writer.printf("%s,%s,%s,%s,%d,%d,%d,%.2f,%.2f,%s%n",
                        limpiar(p.getCodigo()),
                        limpiar(p.getProducto()),
                        limpiar(p.getCategoria()),
                        limpiar(p.getUnidad()),
                        p.getEntradas(),
                        p.getSalidas(),
                        p.getStock(),
                        p.getPrecio(),
                        p.getValor(),
                        p.getReorden());
            }

            lblEstado.setText("Archivo exportado correctamente.");

        } catch (Exception e) {
            mostrarError("No se pudo exportar: " + e.getMessage());
        }
    }

    private String limpiar(String texto) {
        if (texto == null) return "";
        return "\"" + texto.replace("\"", "\"\"") + "\"";
    }

    @FXML
    private void imprimirReporte() {
        PrinterJob job = PrinterJob.createPrinterJob();

        if (job == null) {
            mostrarError("No hay una impresora disponible.");
            return;
        }

        Stage ventana = (Stage) panelReporte.getScene().getWindow();

        if (job.showPrintDialog(ventana)) {
            if (job.printPage(panelReporte)) {
                job.endJob();
                lblEstado.setText("Reporte enviado a impresión.");
            } else {
                mostrarError("No se pudo imprimir el reporte.");
            }
        }
    }

    @FXML
    private void volverAlMenu() throws Exception {
        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/com/laroca/vista/menu.fxml"));

        javafx.scene.Parent vista = loader.load();

        Stage ventana = (Stage) panelReporte.getScene().getWindow();

        Navegacion.mostrar(ventana, vista, "La Roca - Menú Principal");
    }

    private void mostrarError(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Reporte de inventario");
        alerta.setHeaderText("No se pudo completar la operación");
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    public static class ProductoReporte {
        private final String codigo;
        private final String producto;
        private final String categoria;
        private final String unidad;
        private final int entradas;
        private final int salidas;
        private final int stock;
        private final double precio;
        private final double valor;
        private final String reorden;

        public ProductoReporte(String codigo, String producto,
                String categoria, String unidad, int entradas,
                int salidas, int stock, double precio,
                double valor, String reorden) {
            this.codigo = codigo;
            this.producto = producto;
            this.categoria = categoria;
            this.unidad = unidad;
            this.entradas = entradas;
            this.salidas = salidas;
            this.stock = stock;
            this.precio = precio;
            this.valor = valor;
            this.reorden = reorden;
        }

        public String getCodigo() { return codigo; }
        public String getProducto() { return producto; }
        public String getCategoria() { return categoria; }
        public String getUnidad() { return unidad; }
        public int getEntradas() { return entradas; }
        public int getSalidas() { return salidas; }
        public int getStock() { return stock; }
        public double getPrecio() { return precio; }
        public double getValor() { return valor; }
        public String getReorden() { return reorden; }
    }
}
