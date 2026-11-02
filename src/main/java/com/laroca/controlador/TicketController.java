package com.laroca.controlador;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import com.laroca.dao.VentaDAO;
import com.laroca.modelo.DetalleVenta;
import com.laroca.modelo.Venta;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.print.PageLayout;
import javafx.print.PrinterJob;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Scale;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public class TicketController {

    private static final DateTimeFormatter FECHA_TICKET = DateTimeFormatter.ofPattern("dd/MM/yy");
    private static final DateTimeFormatter HORA_TICKET = DateTimeFormatter.ofPattern("HH:mm");

    // Los renglones de productos empiezan después del encabezado y la línea.
    private static final int PRIMERA_FILA = 2;

    @FXML
    private Button btnImprimir;
    @FXML
    private Button btnCerrar;

    @FXML
    private VBox papel;
    @FXML
    private Label lblFolio;
    @FXML
    private Label lblFecha;
    @FXML
    private Label lblHora;
    @FXML
    private GridPane gridProductos;
    @FXML
    private Label lblVendedor;
    @FXML
    private Label lblSubtotal;
    @FXML
    private Label lblMontoPagado;
    @FXML
    private Label lblCambio;

    private final VentaDAO ventaDAO = new VentaDAO();

    // Abre el ticket de la venta como ventana modal y espera a que se cierre.
    // Se usa al cobrar una venta nueva y al reimprimir.
    public static void mostrar(Window duenio, int idVenta) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    TicketController.class.getResource("/com/laroca/vista/ticket.fxml"));

            Parent root = loader.load();
            TicketController controlador = loader.getController();

            if (!controlador.cargarVenta(idVenta)) {
                controlador.mostrarAlerta(
                        "Error",
                        "No se encontró la venta para generar el ticket.");
                return;
            }

            Stage ventana = new Stage();
            ventana.initOwner(duenio);
            ventana.initModality(Modality.APPLICATION_MODAL);
            ventana.setTitle("La Roca - Ticket de compra");
            ventana.setScene(new Scene(root, 520, 600));
            ventana.setResizable(false);
            ventana.sizeToScene();
            ventana.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();

            Alert alerta = new Alert(Alert.AlertType.WARNING);
            alerta.setTitle("Error");
            alerta.setHeaderText(null);
            alerta.setContentText("No se pudo abrir el ticket.");
            alerta.showAndWait();
        }
    }

    // Carga la venta y su detalle desde la base de datos.
    // Devuelve false si la venta no existe.
    public boolean cargarVenta(int idVenta) {
        Venta venta = ventaDAO.obtenerVenta(idVenta);

        if (venta == null) {
            return false;
        }

        lblFolio.setText("Folio: " + venta.getFolio());

        if (venta.getFecha() != null) {
            lblFecha.setText("fecha: " + venta.getFecha().format(FECHA_TICKET));
            lblHora.setText("hora:   " + venta.getFecha().format(HORA_TICKET));
        } else {
            lblFecha.setText("fecha:");
            lblHora.setText("hora:");
        }

        // Quitar renglones de una carga anterior, conservando el encabezado.
        gridProductos.getChildren().removeIf(
                n -> Objects.requireNonNullElse(GridPane.getRowIndex(n), 0) >= PRIMERA_FILA);

        List<DetalleVenta> detalles = ventaDAO.listarDetalle(idVenta);
        int fila = PRIMERA_FILA;

        for (DetalleVenta detalle : detalles) {
            Label producto = new Label(detalle.getNombreProducto());
            producto.setWrapText(true);

            gridProductos.add(producto, 0, fila);
            gridProductos.add(new Label(String.valueOf(detalle.getCantidad())), 1, fila);
            gridProductos.add(new Label(Formato.dinero(detalle.getPrecioUnitario())), 2, fila);
            gridProductos.add(new Label(Formato.dinero(detalle.getSubtotal())), 3, fila);
            fila++;
        }

        lblVendedor.setText("Vendedor: "
                + Objects.toString(venta.getNombreEmpleado(), ""));

        // Ventas anteriores al módulo pueden no tener monto pagado ni cambio.
        BigDecimal total = venta.getTotal();
        BigDecimal montoPagado = venta.getMontoPagado() != null ? venta.getMontoPagado() : total;
        BigDecimal cambio = venta.getCambio() != null ? venta.getCambio() : BigDecimal.ZERO;

        lblSubtotal.setText(Formato.dinero(total));
        lblMontoPagado.setText(Formato.dinero(montoPagado));
        lblCambio.setText(Formato.dinero(cambio));

        return true;
    }

    // Imprime solo el papel del ticket, reducido si no cabe en la hoja.
    @FXML
    private void imprimir() {
        PrinterJob trabajo = PrinterJob.createPrinterJob();

        if (trabajo == null) {
            mostrarAlerta(
                    "Sin impresora",
                    "No hay ninguna impresora disponible en este equipo.");
            return;
        }

        if (!trabajo.showPrintDialog(papel.getScene().getWindow())) {
            trabajo.cancelJob();
            return;
        }

        PageLayout pagina = trabajo.getJobSettings().getPageLayout();
        double ancho = papel.getBoundsInParent().getWidth();
        double alto = papel.getBoundsInParent().getHeight();

        double escala = Math.min(1.0, Math.min(
                pagina.getPrintableWidth() / ancho,
                pagina.getPrintableHeight() / alto));

        Scale ajuste = new Scale(escala, escala);
        papel.getTransforms().add(ajuste);

        boolean impreso = trabajo.printPage(papel);

        papel.getTransforms().remove(ajuste);

        if (impreso) {
            trabajo.endJob();
        } else {
            trabajo.cancelJob();
            mostrarAlerta(
                    "Error al imprimir",
                    "No se pudo imprimir el ticket.");
        }
    }

    @FXML
    private void cerrar() {
        ((Stage) papel.getScene().getWindow()).close();
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.WARNING);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
