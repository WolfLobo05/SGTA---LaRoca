package com.laroca.controlador;

import com.laroca.conexion.Conexion;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class EstadisticasStockController {

    @FXML private TableView<StockItem> tablaStock;
    @FXML private TableColumn<StockItem, String> colProducto;
    @FXML private TableColumn<StockItem, Integer> colEntradas;
    @FXML private TableColumn<StockItem, Integer> colSalidas;
    @FXML private TableColumn<StockItem, Integer> colStockAct;
    @FXML private TableColumn<StockItem, Integer> colCantExistente;

    @FXML private Label lblTotalProductos;
    @FXML private Label lblTotalEntradas;
    @FXML private Label lblTotalSalidas;
    @FXML private Label lblMayorSalida;
    @FXML private Label lblMenorStock;
    @FXML private Label lblEstadoRegistros;

    @FXML private Button btnVolverMenu;
    @FXML private ComboBox<String> cmbPeriodo;

    @FXML
    public void initialize() {
        colProducto.setCellValueFactory(new PropertyValueFactory<>("nombreProducto"));
        colEntradas.setCellValueFactory(new PropertyValueFactory<>("entradas"));
        colSalidas.setCellValueFactory(new PropertyValueFactory<>("salidas"));
        colStockAct.setCellValueFactory(new PropertyValueFactory<>("stockActual"));
        colCantExistente.setCellValueFactory(new PropertyValueFactory<>("stockActual"));

        cargarPeriodosDisponibles();

        if (cmbPeriodo != null) {
            cmbPeriodo.setOnAction(event -> cargarDatosEstadisticas());
        }

        cargarDatosEstadisticas();
    }

    private void cargarPeriodosDisponibles() {
        ObservableList<String> periodos = FXCollections.observableArrayList();
        periodos.add("Histórico (General)");

        String query = """
            SELECT DISTINCT DATE_FORMAT(fecha, '%Y-%m') AS anio_mes
            FROM (
                SELECT fecha FROM Compras
                UNION
                SELECT fecha FROM Ventas
            ) AS t
            ORDER BY anio_mes DESC
        """;

        try (Connection conn = Conexion.conectar()) {

            if (conn == null) {
                if (lblEstadoRegistros != null) {
                    lblEstadoRegistros.setText("No se pudo conectar a la base de datos.");
                }
                return;
            }

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {

                while (rs.next()) {
                    String anioMes = rs.getString("anio_mes");

                    if (anioMes != null && anioMes.length() >= 7) {
                        String anio = anioMes.substring(0, 4);
                        String mesNum = anioMes.substring(5, 7);
                        String nombreMes = convertirNumeroAMes(mesNum);
                        String periodoStr = nombreMes + " " + anio;

                        if (!periodos.contains(periodoStr)) {
                            periodos.add(periodoStr);
                        }
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();

            if (lblEstadoRegistros != null) {
                lblEstadoRegistros.setText("Error al cargar periodos: " + e.getMessage());
            }
        }

        if (cmbPeriodo != null) {
            cmbPeriodo.setItems(periodos);

            if (periodos.size() > 1) {
                cmbPeriodo.setValue(periodos.get(1));
            } else {
                cmbPeriodo.setValue("Histórico (General)");
            }
        }
    }

    private String convertirNumeroAMes(String mesNum) {
        return switch (mesNum) {
            case "01" -> "Enero";
            case "02" -> "Febrero";
            case "03" -> "Marzo";
            case "04" -> "Abril";
            case "05" -> "Mayo";
            case "06" -> "Junio";
            case "07" -> "Julio";
            case "08" -> "Agosto";
            case "09" -> "Septiembre";
            case "10" -> "Octubre";
            case "11" -> "Noviembre";
            case "12" -> "Diciembre";
            default -> "Mes";
        };
    }

    @FXML
    private void handleGenerarEstadisticas() {
        cargarDatosEstadisticas();
        mostrarAlerta("Éxito", "Estadísticas generadas correctamente.");
    }

    @FXML
    private void handleActualizar() {
        cargarPeriodosDisponibles();
        cargarDatosEstadisticas();
        mostrarAlerta("Actualizado", "Datos sincronizados con éxito.");
    }

    @FXML
    private void handleExportar() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Estadísticas de Stock");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivo CSV (*.csv)", "*.csv")
        );
        fileChooser.setInitialFileName("Estadisticas_Stock.csv");

        File archivo = fileChooser.showSaveDialog(tablaStock.getScene().getWindow());

        if (archivo != null) {
            try (FileWriter writer = new FileWriter(archivo)) {
                writer.write("Producto,Entradas,Salidas,Stock Actual\n");

                for (StockItem item : tablaStock.getItems()) {
                    writer.write(String.format("\"%s\",%d,%d,%d\n",
                            item.getNombreProducto(),
                            item.getEntradas(),
                            item.getSalidas(),
                            item.getStockActual()));
                }

                mostrarAlerta("Exportación Exitosa",
                        "Reporte guardado en:\n" + archivo.getAbsolutePath());

            } catch (IOException e) {
                e.printStackTrace();
                mostrarAlerta("Error", "No se pudo exportar: " + e.getMessage());
            }
        }
    }

    @FXML
    private void volverAlMenu() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/laroca/vista/menu.fxml")
            );

            javafx.scene.Parent vista = loader.load();
            Stage ventana = (Stage) btnVolverMenu.getScene().getWindow();

            Navegacion.mostrar(ventana, vista, "La Roca - Menú Principal");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cargarDatosEstadisticas() {
        ObservableList<StockItem> listaStock = FXCollections.observableArrayList();

        String seleccion = cmbPeriodo != null
                ? cmbPeriodo.getValue()
                : "Histórico (General)";

        boolean filtrarPorMes = seleccion != null && !seleccion.contains("Histórico");

        String mesNum = "11";
        String anio = "2026";

        if (filtrarPorMes) {
            String[] partes = seleccion.split(" ");

            if (partes.length >= 2) {
                anio = partes[1];
                mesNum = convertirMesANumero(partes[0]);
            }
        }

        String query;

        if (filtrarPorMes) {
            query = """
                SELECT
                    p.nombre,
                    COALESCE((SELECT SUM(dc.cantidad)
                              FROM detalle_compras dc
                              JOIN Compras c ON dc.id_compras = c.id_compras
                              WHERE dc.id_producto = p.id_producto
                              AND MONTH(c.fecha) = ? AND YEAR(c.fecha) = ?), 0)
                              AS total_entradas,
                    COALESCE((SELECT SUM(dv.cantidad)
                              FROM detalle_ventas dv
                              JOIN Ventas v ON dv.id_venta = v.id_venta
                              WHERE dv.id_producto = p.id_producto
                              AND MONTH(v.fecha) = ? AND YEAR(v.fecha) = ?), 0)
                              AS total_salidas
                FROM Productos p
            """;
        } else {
            query = """
                SELECT
                    p.nombre,
                    COALESCE((SELECT SUM(dc.cantidad)
                              FROM detalle_compras dc
                              WHERE dc.id_producto = p.id_producto), 0)
                              AS total_entradas,
                    COALESCE((SELECT SUM(dv.cantidad)
                              FROM detalle_ventas dv
                              WHERE dv.id_producto = p.id_producto), 0)
                              AS total_salidas
                FROM Productos p
            """;
        }

        try (Connection conn = Conexion.conectar()) {

            if (conn == null) {
                if (lblEstadoRegistros != null) {
                    lblEstadoRegistros.setText("No se pudo conectar a la base de datos.");
                }
                return;
            }

            try (PreparedStatement pstmt = conn.prepareStatement(query)) {

                if (filtrarPorMes) {
                    pstmt.setInt(1, Integer.parseInt(mesNum));
                    pstmt.setInt(2, Integer.parseInt(anio));
                    pstmt.setInt(3, Integer.parseInt(mesNum));
                    pstmt.setInt(4, Integer.parseInt(anio));
                }

                try (ResultSet rs = pstmt.executeQuery()) {
                    int totalProd = 0;
                    int sumEntradas = 0;
                    int sumSalidas = 0;

                    StockItem mayorSalida = null;
                    StockItem menorStock = null;

                    while (rs.next()) {
                        String nombre = rs.getString("nombre");
                        int entradas = rs.getInt("total_entradas");
                        int salidas = rs.getInt("total_salidas");

                        // Protección contra stock negativo (nunca baja de 0)
                        int stockActual = Math.max(0, entradas - salidas);

                        StockItem item = new StockItem(
                                nombre, entradas, salidas, stockActual
                        );

                        listaStock.add(item);

                        totalProd++;
                        sumEntradas += entradas;
                        sumSalidas += salidas;

                        if (mayorSalida == null || salidas > mayorSalida.getSalidas()) {
                            mayorSalida = item;
                        }

                        if (menorStock == null
                                || stockActual < menorStock.getStockActual()) {
                            menorStock = item;
                        }
                    }

                    tablaStock.setItems(listaStock);

                    lblTotalProductos.setText(totalProd + " productos");
                    lblTotalEntradas.setText(sumEntradas + " unidades");
                    lblTotalSalidas.setText(sumSalidas + " unidades");

                    lblMayorSalida.setText(
                            mayorSalida != null
                                    ? mayorSalida.getNombreProducto()
                                    : "-"
                    );

                    lblMenorStock.setText(
                            menorStock != null
                                    ? menorStock.getNombreProducto()
                                    : "-"
                    );

                    lblEstadoRegistros.setText(
                            totalProd + " productos (" + seleccion + ")"
                    );
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            lblEstadoRegistros.setText("Error SQL: " + e.getMessage());
        }
    }

    private String convertirMesANumero(String nombreMes) {
        return switch (nombreMes) {
            case "Enero" -> "01";
            case "Febrero" -> "02";
            case "Marzo" -> "03";
            case "Abril" -> "04";
            case "Mayo" -> "05";
            case "Junio" -> "06";
            case "Julio" -> "07";
            case "Agosto" -> "08";
            case "Septiembre" -> "09";
            case "Octubre" -> "10";
            case "Noviembre" -> "11";
            case "Diciembre" -> "12";
            default -> "11";
        };
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    public static class StockItem {
        private String nombreProducto;
        private int entradas;
        private int salidas;
        private int stockActual;

        public StockItem(String nombreProducto, int entradas,
                         int salidas, int stockActual) {
            this.nombreProducto = nombreProducto;
            this.entradas = entradas;
            this.salidas = salidas;
            this.stockActual = stockActual;
        }

        public String getNombreProducto() { return nombreProducto; }
        public int getEntradas() { return entradas; }
        public int getSalidas() { return salidas; }
        public int getStockActual() { return stockActual; }
    }
}
