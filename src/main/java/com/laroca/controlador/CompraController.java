package com.laroca.controlador;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import com.laroca.dao.CompraDAO;
import com.laroca.dao.ProveedorDAO;
import com.laroca.modelo.Compra;
import com.laroca.modelo.DetalleCompra;
import com.laroca.modelo.Estados;
import com.laroca.modelo.Proveedor;

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

public class CompraController {

    @FXML
    private Button btnNuevo;
    @FXML
    private Button btnEditar;
    @FXML
    private Button btnEliminar;
    @FXML
    private Button btnVolverMenu;
    @FXML
    private Button btnGuardar;
    @FXML
    private Button btnCancelar;
    @FXML
    private HBox accionesEdicion;

    @FXML
    private CheckBox chkMostrarCanceladas;

    @FXML
    private TableView<Compra> tableCompras;
    @FXML
    private TableColumn<Compra, Integer> colId;
    @FXML
    private TableColumn<Compra, String> colFolio;
    @FXML
    private TableColumn<Compra, String> colProveedor;
    @FXML
    private TableColumn<Compra, String> colEmpleado;
    @FXML
    private TableColumn<Compra, String> colFecha;
    @FXML
    private TableColumn<Compra, String> colTotal;

    @FXML
    private TextField txtFolio;
    @FXML
    private TextField txtFecha;
    @FXML
    private TextField txtProveedor;
    @FXML
    private ComboBox<Proveedor> cmbProveedor;
    @FXML
    private TextField txtEmpleado;

    @FXML
    private TableView<DetalleCompra> tableDetalle;
    @FXML
    private TableColumn<DetalleCompra, String> colProducto;
    @FXML
    private TableColumn<DetalleCompra, Integer> colCantidad;
    @FXML
    private TableColumn<DetalleCompra, String> colPrecio;
    @FXML
    private TableColumn<DetalleCompra, String> colSubtotal;

    @FXML
    private Label lblTotal;
    @FXML
    private Label lblFechaActual;
    @FXML
    private Label lblEstadoFooter;

    private final CompraDAO compraDAO = new CompraDAO();
    private final ProveedorDAO proveedorDAO = new ProveedorDAO();

    private ObservableList<Compra> listaCompras;
    private Compra compraSeleccionada;

    private boolean modoEdicion = false;

    @FXML
    public void initialize() {

        lblFechaActual.setText(
                LocalDate.now().format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        colId.setCellValueFactory(
                new PropertyValueFactory<>("idCompra"));
        colFolio.setCellValueFactory(
                new PropertyValueFactory<>("folio"));
        colProveedor.setCellValueFactory(
                new PropertyValueFactory<>("nombreProveedor"));
        colEmpleado.setCellValueFactory(
                new PropertyValueFactory<>("nombreEmpleado"));
        colFecha.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                Formato.fecha(c.getValue().getFecha())));
        colTotal.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                Formato.dinero(c.getValue().getTotal())));

        colProducto.setCellValueFactory(
                new PropertyValueFactory<>("nombreProducto"));
        colCantidad.setCellValueFactory(
                new PropertyValueFactory<>("cantidad"));
        colPrecio.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                Formato.dinero(c.getValue().getPrecioCompra())));
        colSubtotal.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                Formato.dinero(c.getValue().getSubtotal())));

        tableDetalle.setPlaceholder(new Label(""));

        cmbProveedor.setConverter(new StringConverter<Proveedor>() {
            @Override
            public String toString(Proveedor proveedor) {
                return proveedor == null ? "" : proveedor.getNombre();
            }

            @Override
            public Proveedor fromString(String texto) {
                return null;
            }
        });

        // El ComboBox reemplaza al campo de texto solo al editar.
        txtProveedor.managedProperty().bind(txtProveedor.visibleProperty());
        cmbProveedor.managedProperty().bind(cmbProveedor.visibleProperty());

        cargarTabla();
        limpiarDetalle();

        tableCompras.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, anterior, seleccionada) -> {

                    // Cambiar de compra descarta la edición en curso.
                    if (modoEdicion) {
                        terminarEdicion();
                    }

                    compraSeleccionada = seleccionada;

                    if (seleccionada != null) {
                        mostrarCompra(seleccionada);

                        // Las compras canceladas solo se consultan.
                        boolean canceladas = chkMostrarCanceladas.isSelected();
                        btnEditar.setDisable(canceladas);
                        btnEliminar.setDisable(canceladas);
                    } else {
                        limpiarDetalle();
                    }
                });
    }

    // Muestra solo las registradas o solo las canceladas, según la casilla.
    private void cargarTabla() {
        boolean canceladas = chkMostrarCanceladas.isSelected();

        listaCompras = FXCollections.observableArrayList();

        for (Compra compra : compraDAO.listar()) {
            if (Estados.esActivo(compra.getEstado()) != canceladas) {
                listaCompras.add(compra);
            }
        }

        tableCompras.setItems(listaCompras);

        actualizarPie();
    }

    private void actualizarPie() {
        boolean canceladas = chkMostrarCanceladas.isSelected();
        int total = listaCompras.size();

        lblEstadoFooter.setText(
                total + (total == 1
                        ? (canceladas ? " compra cancelada" : " compra registrada")
                        : (canceladas ? " compras canceladas" : " compras registradas")));
    }

    private void mostrarCompra(Compra compra) {
        txtFolio.setText(compra.getFolio());
        txtFecha.setText(Formato.fecha(compra.getFecha()));
        txtProveedor.setText(Objects.toString(compra.getNombreProveedor(), ""));
        txtEmpleado.setText(Objects.toString(compra.getNombreEmpleado(), ""));
        lblTotal.setText(Formato.dinero(compra.getTotal()));

        tableDetalle.setItems(FXCollections.observableArrayList(
                compraDAO.listarDetalle(compra.getIdCompra())));
    }

    private void limpiarDetalle() {
        txtFolio.clear();
        txtFecha.clear();
        txtProveedor.clear();
        txtEmpleado.clear();
        lblTotal.setText("");
        tableDetalle.getItems().clear();

        btnEditar.setDisable(true);
        btnEliminar.setDisable(true);
    }

    @FXML
    private void nuevaCompra(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/laroca/vista/registrar-compra.fxml"));

            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene().getWindow();

            Navegacion.mostrar(stage, root, "La Roca - Registrar Compra");

        } catch (IOException e) {
            e.printStackTrace();

            mostrarAlerta(
                    "Error",
                    "No se pudo abrir la pantalla para registrar compras.");
        }
    }

    // Solo se puede corregir el proveedor.
    // Los productos y el total nunca se editan.
    @FXML
    private void editarCompra() {
        if (compraSeleccionada == null) {
            return;
        }

        modoEdicion = true;

        List<Proveedor> activos = proveedorDAO.listar().stream()
                .filter(p -> Estados.esActivo(p.getEstado()))
                .toList();

        cmbProveedor.setItems(FXCollections.observableArrayList(activos));
        seleccionarProveedor(compraSeleccionada.getIdProveedor(),
                compraSeleccionada.getNombreProveedor());

        txtProveedor.setVisible(false);
        cmbProveedor.setVisible(true);
        accionesEdicion.setVisible(true);

        btnNuevo.setDisable(true);
        btnEditar.setDisable(true);
        btnEliminar.setDisable(true);
        chkMostrarCanceladas.setDisable(true);

        lblEstadoFooter.setText("Modo: Editando compra " + compraSeleccionada.getFolio());
        cmbProveedor.requestFocus();
    }

    // Si el proveedor actual está inactivo no aparece en la lista,
    // así que se muestra con un objeto temporal (solo como valor actual).
    private void seleccionarProveedor(int idProveedor, String nombre) {
        for (Proveedor proveedor : cmbProveedor.getItems()) {
            if (proveedor.getIdProveedor() == idProveedor) {
                cmbProveedor.setValue(proveedor);
                return;
            }
        }

        Proveedor actual = new Proveedor();
        actual.setIdProveedor(idProveedor);
        actual.setNombre(nombre);
        cmbProveedor.setValue(actual);
    }

    @FXML
    private void guardarCompra() {
        if (compraSeleccionada == null) {
            return;
        }

        Proveedor proveedor = cmbProveedor.getValue();

        if (proveedor == null) {
            mostrarAlerta(
                    "Proveedor obligatorio",
                    "Selecciona el proveedor de la compra.");
            return;
        }

        int idCompra = compraSeleccionada.getIdCompra();

        if (compraDAO.actualizarProveedor(idCompra, proveedor.getIdProveedor())) {

            terminarEdicion();
            cargarTabla();
            seleccionarPorId(idCompra);

            mostrarAlerta(
                    "Actualización exitosa",
                    "La compra se actualizó correctamente.");
        } else {
            mostrarAlerta(
                    "Error",
                    "No se pudo actualizar la compra.");
        }
    }

    @FXML
    private void cancelarAccion() {
        terminarEdicion();

        if (compraSeleccionada != null) {
            mostrarCompra(compraSeleccionada);
        }
    }

    private void terminarEdicion() {
        modoEdicion = false;

        txtProveedor.setVisible(true);
        cmbProveedor.setVisible(false);
        accionesEdicion.setVisible(false);

        boolean canceladas = chkMostrarCanceladas.isSelected();
        btnNuevo.setDisable(canceladas);
        btnEditar.setDisable(canceladas || compraSeleccionada == null);
        btnEliminar.setDisable(canceladas || compraSeleccionada == null);
        chkMostrarCanceladas.setDisable(false);

        actualizarPie();
    }

    // Selecciona la compra y la deja visible para mostrar su Detalle.
    // Registrar Compra lo usa al volver después de guardar.
    public void seleccionarPorId(int idCompra) {
        for (Compra compra : listaCompras) {
            if (compra.getIdCompra() == idCompra) {
                tableCompras.getSelectionModel().select(compra);
                tableCompras.scrollTo(compra);
                tableCompras.requestFocus();
                return;
            }
        }
    }

    // Cancela la compra y resta su mercancía del stock. No hay reactivación.
    @FXML
    private void eliminarCompra() {
        if (compraSeleccionada == null) {
            return;
        }

        Alert confirmacion = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Compra " + compraSeleccionada.getFolio() + " de "
                        + Objects.toString(compraSeleccionada.getNombreProveedor(), "")
                        + " por " + Formato.dinero(compraSeleccionada.getTotal()) + "\n\n"
                        + "La compra quedará cancelada y su mercancía se restará "
                        + "del stock. ¿Continuar?",
                ButtonType.YES,
                ButtonType.NO);

        confirmacion.setTitle("Confirmar cancelación");
        confirmacion.setHeaderText(null);
        confirmacion.showAndWait();

        if (confirmacion.getResult() == ButtonType.YES) {

            if (compraDAO.cancelar(compraSeleccionada.getIdCompra())) {
                tableCompras.getSelectionModel().clearSelection();
                cargarTabla();

                mostrarAlerta(
                        "Compra cancelada",
                        "La compra se canceló y su mercancía se restó del inventario.");
            } else {
                mostrarAlerta(
                        "No se pudo cancelar",
                        compraDAO.getMensajeError());
            }
        }
    }

    // Cambia entre las compras registradas y las canceladas.
    @FXML
    private void cambiarMostrarCanceladas() {
        boolean canceladas = chkMostrarCanceladas.isSelected();

        btnNuevo.setDisable(canceladas);

        tableCompras.getSelectionModel().clearSelection();
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
