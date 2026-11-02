package com.laroca.controlador;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import com.laroca.dao.ClienteDAO;
import com.laroca.dao.MetodoPagoDAO;
import com.laroca.dao.VentaDAO;
import com.laroca.modelo.Cliente;
import com.laroca.modelo.DetalleVenta;
import com.laroca.modelo.Estados;
import com.laroca.modelo.MetodoPago;
import com.laroca.modelo.Venta;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class VentaController {

    @FXML
    private Button btnNuevo;
    @FXML
    private Button btnEditar;
    @FXML
    private Button btnEliminar;
    @FXML
    private Button btnVolverMenu;
    @FXML
    private Button btnReimprimir;
    @FXML
    private Button btnGuardar;
    @FXML
    private Button btnCancelar;
    @FXML
    private HBox accionesEdicion;

    @FXML
    private CheckBox chkMostrarCanceladas;

    @FXML
    private TableView<Venta> tableVentas;
    @FXML
    private TableColumn<Venta, Integer> colId;
    @FXML
    private TableColumn<Venta, String> colFolio;
    @FXML
    private TableColumn<Venta, String> colEmpleado;
    @FXML
    private TableColumn<Venta, String> colCliente;
    @FXML
    private TableColumn<Venta, String> colMetodo;
    @FXML
    private TableColumn<Venta, String> colFecha;
    @FXML
    private TableColumn<Venta, String> colTotal;

    @FXML
    private TextField txtFolio;
    @FXML
    private TextField txtFecha;
    @FXML
    private TextField txtEmpleado;
    @FXML
    private TextField txtCliente;
    @FXML
    private TextField txtMetodo;
    @FXML
    private ComboBox<Cliente> cmbCliente;
    @FXML
    private ComboBox<MetodoPago> cmbMetodo;

    @FXML
    private TableView<DetalleVenta> tableDetalle;
    @FXML
    private TableColumn<DetalleVenta, String> colProducto;
    @FXML
    private TableColumn<DetalleVenta, Integer> colCantidad;
    @FXML
    private TableColumn<DetalleVenta, String> colPrecio;
    @FXML
    private TableColumn<DetalleVenta, String> colSubtotal;

    @FXML
    private Label lblTotal;
    @FXML
    private Label lblFechaActual;
    @FXML
    private Label lblEstadoFooter;

    private final VentaDAO ventaDAO = new VentaDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final MetodoPagoDAO metodoPagoDAO = new MetodoPagoDAO();

    private ObservableList<Venta> listaVentas;
    private Venta ventaSeleccionada;

    private boolean modoEdicion = false;

    @FXML
    public void initialize() {

        lblFechaActual.setText(
                LocalDate.now().format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        colId.setCellValueFactory(
                new PropertyValueFactory<>("idVenta"));
        colFolio.setCellValueFactory(
                new PropertyValueFactory<>("folio"));
        colEmpleado.setCellValueFactory(
                new PropertyValueFactory<>("nombreEmpleado"));
        colCliente.setCellValueFactory(
                new PropertyValueFactory<>("nombreCliente"));
        colMetodo.setCellValueFactory(
                new PropertyValueFactory<>("nombreMetodo"));
        colFecha.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                Formato.fecha(c.getValue().getFecha())));
        colTotal.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                Formato.dinero(c.getValue().getTotal())));

        colProducto.setCellValueFactory(
                new PropertyValueFactory<>("nombreProducto"));
        colCantidad.setCellValueFactory(
                new PropertyValueFactory<>("cantidad"));
        colPrecio.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                Formato.dinero(c.getValue().getPrecioUnitario())));
        colSubtotal.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                Formato.dinero(c.getValue().getSubtotal())));

        tableDetalle.setPlaceholder(new Label(""));

        cmbCliente.setConverter(new StringConverter<Cliente>() {
            @Override
            public String toString(Cliente cliente) {
                return cliente == null ? "" : cliente.getNombreCompleto();
            }

            @Override
            public Cliente fromString(String texto) {
                return null;
            }
        });

        cmbMetodo.setConverter(new StringConverter<MetodoPago>() {
            @Override
            public String toString(MetodoPago metodo) {
                return metodo == null ? "" : metodo.getNombre();
            }

            @Override
            public MetodoPago fromString(String texto) {
                return null;
            }
        });

        // Los ComboBox reemplazan a los campos de texto solo al editar.
        txtCliente.managedProperty().bind(txtCliente.visibleProperty());
        cmbCliente.managedProperty().bind(cmbCliente.visibleProperty());
        txtMetodo.managedProperty().bind(txtMetodo.visibleProperty());
        cmbMetodo.managedProperty().bind(cmbMetodo.visibleProperty());

        cargarTabla();
        limpiarDetalle();

        tableVentas.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, anterior, seleccionada) -> {

                    // Cambiar de venta descarta la edición en curso.
                    if (modoEdicion) {
                        terminarEdicion();
                    }

                    ventaSeleccionada = seleccionada;

                    if (seleccionada != null) {
                        mostrarVenta(seleccionada);

                        // Las ventas canceladas solo se consultan.
                        boolean canceladas = chkMostrarCanceladas.isSelected();
                        btnEditar.setDisable(canceladas);
                        btnEliminar.setDisable(canceladas);
                        btnReimprimir.setDisable(false);
                    } else {
                        limpiarDetalle();
                    }
                });
    }

    // Muestra solo las registradas o solo las canceladas, según la casilla.
    private void cargarTabla() {
        boolean canceladas = chkMostrarCanceladas.isSelected();

        listaVentas = FXCollections.observableArrayList();

        for (Venta venta : ventaDAO.listar()) {
            if (Estados.esActivo(venta.getEstado()) != canceladas) {
                listaVentas.add(venta);
            }
        }

        tableVentas.setItems(listaVentas);

        actualizarPie();
    }

    private void actualizarPie() {
        boolean canceladas = chkMostrarCanceladas.isSelected();
        int total = listaVentas.size();

        lblEstadoFooter.setText(
                total + (total == 1
                        ? (canceladas ? " venta cancelada" : " venta registrada")
                        : (canceladas ? " ventas canceladas" : " ventas registradas")));
    }

    private void mostrarVenta(Venta venta) {
        txtFolio.setText(venta.getFolio());
        txtFecha.setText(Formato.fecha(venta.getFecha()));
        txtEmpleado.setText(Objects.toString(venta.getNombreEmpleado(), ""));
        txtCliente.setText(Objects.toString(venta.getNombreCliente(), ""));
        txtMetodo.setText(Objects.toString(venta.getNombreMetodo(), ""));
        lblTotal.setText(Formato.dinero(venta.getTotal()));

        tableDetalle.setItems(FXCollections.observableArrayList(
                ventaDAO.listarDetalle(venta.getIdVenta())));
    }

    private void limpiarDetalle() {
        txtFolio.clear();
        txtFecha.clear();
        txtEmpleado.clear();
        txtCliente.clear();
        txtMetodo.clear();
        lblTotal.setText("");
        tableDetalle.getItems().clear();

        btnEditar.setDisable(true);
        btnEliminar.setDisable(true);
        btnReimprimir.setDisable(true);
    }

    @FXML
    private void nuevaVenta(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/laroca/vista/nueva-venta.fxml"));

            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene().getWindow();

            Navegacion.mostrar(stage, root, "La Roca - Nueva Venta");

        } catch (IOException e) {
            e.printStackTrace();

            mostrarAlerta(
                    "Error",
                    "No se pudo abrir la pantalla de nueva venta.");
        }
    }

    // Solo se pueden corregir el cliente y el método de pago.
    // Los productos y el total nunca se editan.
    @FXML
    private void editarVenta() {
        if (ventaSeleccionada == null) {
            return;
        }

        modoEdicion = true;

        List<Cliente> activos = clienteDAO.listar().stream()
                .filter(c -> Estados.esActivo(c.getEstado()))
                .toList();

        cmbCliente.setItems(FXCollections.observableArrayList(activos));
        seleccionarCliente(ventaSeleccionada.getIdCliente(),
                ventaSeleccionada.getNombreCliente());

        cmbMetodo.setItems(FXCollections.observableArrayList(
                metodoPagoDAO.listar()));
        cmbMetodo.setValue(null);

        for (MetodoPago metodo : cmbMetodo.getItems()) {
            if (metodo.getIdMetodo() == ventaSeleccionada.getIdMetodo()) {
                cmbMetodo.setValue(metodo);
            }
        }

        txtCliente.setVisible(false);
        cmbCliente.setVisible(true);
        txtMetodo.setVisible(false);
        cmbMetodo.setVisible(true);

        btnReimprimir.setVisible(false);
        accionesEdicion.setVisible(true);

        btnNuevo.setDisable(true);
        btnEditar.setDisable(true);
        btnEliminar.setDisable(true);
        chkMostrarCanceladas.setDisable(true);

        lblEstadoFooter.setText("Modo: Editando venta " + ventaSeleccionada.getFolio());
        cmbCliente.requestFocus();
    }

    // Si el cliente actual está inactivo no aparece en la lista,
    // así que se muestra con un objeto temporal (solo como valor actual).
    private void seleccionarCliente(int idCliente, String nombre) {
        for (Cliente cliente : cmbCliente.getItems()) {
            if (cliente.getIdCliente() == idCliente) {
                cmbCliente.setValue(cliente);
                return;
            }
        }

        Cliente actual = new Cliente(idCliente, nombre, null, null, null, null);
        cmbCliente.setValue(actual);
    }

    @FXML
    private void guardarVenta() {
        if (ventaSeleccionada == null) {
            return;
        }

        Cliente cliente = cmbCliente.getValue();
        MetodoPago metodo = cmbMetodo.getValue();

        if (cliente == null || metodo == null) {
            mostrarAlerta(
                    "Datos incompletos",
                    "Selecciona el cliente y el método de pago.");
            return;
        }

        int idVenta = ventaSeleccionada.getIdVenta();

        if (ventaDAO.actualizarDatos(idVenta,
                cliente.getIdCliente(), metodo.getIdMetodo())) {

            terminarEdicion();
            cargarTabla();
            seleccionarPorId(idVenta);

            mostrarAlerta(
                    "Actualización exitosa",
                    "La venta se actualizó correctamente.");
        } else {
            mostrarAlerta(
                    "Error",
                    "No se pudo actualizar la venta.");
        }
    }

    @FXML
    private void cancelarAccion() {
        terminarEdicion();

        if (ventaSeleccionada != null) {
            mostrarVenta(ventaSeleccionada);
        }
    }

    private void terminarEdicion() {
        modoEdicion = false;

        txtCliente.setVisible(true);
        cmbCliente.setVisible(false);
        txtMetodo.setVisible(true);
        cmbMetodo.setVisible(false);

        btnReimprimir.setVisible(true);
        accionesEdicion.setVisible(false);

        boolean canceladas = chkMostrarCanceladas.isSelected();
        btnNuevo.setDisable(canceladas);
        btnEditar.setDisable(canceladas || ventaSeleccionada == null);
        btnEliminar.setDisable(canceladas || ventaSeleccionada == null);
        chkMostrarCanceladas.setDisable(false);

        actualizarPie();
    }

    // Selecciona la venta y la deja visible para mostrar su Detalle.
    // Nueva Venta lo usa al volver después de cobrar.
    public void seleccionarPorId(int idVenta) {
        for (Venta venta : listaVentas) {
            if (venta.getIdVenta() == idVenta) {
                tableVentas.getSelectionModel().select(venta);
                tableVentas.scrollTo(venta);
                tableVentas.requestFocus();
                return;
            }
        }
    }

    // Cancela la venta y devuelve el stock. No hay reactivación.
    @FXML
    private void eliminarVenta() {
        if (ventaSeleccionada == null) {
            return;
        }

        Alert confirmacion = new Alert(
                Alert.AlertType.CONFIRMATION,
                "¿Deseas cancelar la venta "
                        + ventaSeleccionada.getFolio() + " por "
                        + Formato.dinero(ventaSeleccionada.getTotal()) + "?\n\n"
                        + "La venta quedará cancelada y el stock se devolverá.",
                ButtonType.YES,
                ButtonType.NO);

        confirmacion.setTitle("Confirmar cancelación");
        confirmacion.setHeaderText(null);
        confirmacion.showAndWait();

        if (confirmacion.getResult() == ButtonType.YES) {

            if (ventaDAO.cancelar(ventaSeleccionada.getIdVenta())) {
                tableVentas.getSelectionModel().clearSelection();
                cargarTabla();

                mostrarAlerta(
                        "Venta cancelada",
                        "La venta se canceló y el stock se devolvió al inventario.");
            } else {
                mostrarAlerta(
                        "No se pudo cancelar",
                        ventaDAO.getMensajeError());
            }
        }
    }

    @FXML
    private void reimprimirTicket() {
        if (ventaSeleccionada == null) {
            return;
        }

        TicketController.mostrar(
                tableVentas.getScene().getWindow(),
                ventaSeleccionada.getIdVenta());
    }

    // Cambia entre las ventas registradas y las canceladas.
    @FXML
    private void cambiarMostrarCanceladas() {
        boolean canceladas = chkMostrarCanceladas.isSelected();

        btnNuevo.setDisable(canceladas);

        tableVentas.getSelectionModel().clearSelection();
        cargarTabla();
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.WARNING);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    @FXML
    private void volverAlMenu(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/laroca/vista/menu.fxml"));

            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene().getWindow();

            Navegacion.mostrar(stage, root, Navegacion.TITULO_MENU);

        } catch (IOException e) {
            e.printStackTrace();

            mostrarAlerta(
                    "Error",
                    "No se pudo abrir el menú principal.");
        }
    }
}
