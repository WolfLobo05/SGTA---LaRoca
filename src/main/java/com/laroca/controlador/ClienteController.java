package com.laroca.controlador;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.laroca.dao.ClienteDAO;
import com.laroca.modelo.Cliente;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class ClienteController {

    @FXML
    private Button btnNuevo;
    @FXML
    private Button btnEliminar;
    @FXML
    private Button btnVolverMenu;
    @FXML
    private Button btnGuardar;
    @FXML
    private Button btnCancelar;

    @FXML
    private TextField txtNombre;
    @FXML
    private TextField txtApellidoPaterno;
    @FXML
    private TextField txtApellidoMaterno;
    @FXML
    private TextField txtTelefono;
    @FXML
    private TextField txtCorreo;

    @FXML
    private TableView<Cliente> tableClientes;
    @FXML
    private TableColumn<Cliente, Integer> colId;
    @FXML
    private TableColumn<Cliente, String> colNombre;
    @FXML
    private TableColumn<Cliente, String> colApellidoPaterno;
    @FXML
    private TableColumn<Cliente, String> colTelefono;
    @FXML
    private TableColumn<Cliente, String> colCorreo;

    @FXML
    private Label lblFechaActual;
    @FXML
    private Label lblEstadoFooter;

    private final ClienteDAO clienteDAO = new ClienteDAO();

    private ObservableList<Cliente> listaClientes;
    private Cliente clienteSeleccionado;

    private boolean formularioActivo = false;

    @FXML
    public void initialize() {

        lblFechaActual.setText(
                LocalDate.now().format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        // El ID se utiliza internamente, pero no se muestra.
        colId.setCellValueFactory(
                new PropertyValueFactory<>("idCliente"));
        colId.setVisible(false);

        colNombre.setCellValueFactory(
                new PropertyValueFactory<>("nombre"));
        colApellidoPaterno.setCellValueFactory(
                new PropertyValueFactory<>("apellidoPaterno"));
        colTelefono.setCellValueFactory(
                new PropertyValueFactory<>("telefono"));
        colCorreo.setCellValueFactory(
                new PropertyValueFactory<>("correo"));

        aplicarRestricciones();
        configurarListeners();
        cargarTabla();

        bloquearFormulario(true);

        btnEliminar.setDisable(true);
        btnGuardar.setDisable(true);
        btnCancelar.setDisable(true);

        tableClientes.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, anterior, seleccionado) -> {

                    if (seleccionado != null) {
                        clienteSeleccionado = seleccionado;
                        formularioActivo = true;

                        mostrarCliente(seleccionado);
                        bloquearFormulario(false);

                        btnEliminar.setDisable(false);
                        btnCancelar.setDisable(false);

                        actualizarEstadoGuardar();

                        lblEstadoFooter.setText(
                                "Modo: Editando cliente");
                    } else {
                        btnEliminar.setDisable(true);
                    }
                });
    }

    private void cargarTabla() {
        listaClientes = FXCollections.observableArrayList(
                clienteDAO.listar());

        tableClientes.setItems(listaClientes);

        lblEstadoFooter.setText(
                listaClientes.size()
                        + (listaClientes.size() == 1
                                ? " cliente registrado"
                                : " clientes registrados"));
    }

    private void mostrarCliente(Cliente cliente) {
        txtNombre.setText(cliente.getNombre());
        txtApellidoPaterno.setText(
                cliente.getApellidoPaterno());
        txtApellidoMaterno.setText(
                cliente.getApellidoMaterno());
        txtTelefono.setText(cliente.getTelefono());
        txtCorreo.setText(cliente.getCorreo());
    }

    @FXML
    private void nuevoCliente() {
        clienteSeleccionado = null;
        formularioActivo = true;

        tableClientes.getSelectionModel().clearSelection();

        limpiarCamposTexto();
        bloquearFormulario(false);

        btnEliminar.setDisable(true);
        btnCancelar.setDisable(false);

        actualizarEstadoGuardar();

        txtNombre.requestFocus();
        lblEstadoFooter.setText(
                "Modo: Registrando nuevo cliente");
    }

    @FXML
    private void guardarCliente() {

        String nombre = txtNombre.getText().trim();
        String paterno = txtApellidoPaterno.getText().trim();
        String materno = txtApellidoMaterno.getText().trim();
        String telefono = txtTelefono.getText().trim();
        String correo = txtCorreo.getText().trim();

        if (!formularioActivo || !formularioValido()) {
            mostrarAlerta(
                    "Datos incompletos o inválidos",
                    "Revisa los campos obligatorios y sus formatos.");
            actualizarEstadoGuardar();
            return;
        }

        int idExcluir = clienteSeleccionado == null
                ? 0
                : clienteSeleccionado.getIdCliente();

        if (clienteDAO.existeCliente(
                nombre, paterno, materno,
                telefono, correo, idExcluir)) {

            mostrarAlerta(
                    "Teléfono duplicado",
                    "Ya existe otro cliente registrado con ese teléfono.");
            return;
        }

        if (clienteSeleccionado == null) {

            int id = clienteDAO.obtenerSiguienteId();

            if (id <= 0) {
                mostrarAlerta(
                        "Error",
                        "No se pudo obtener el ID del cliente.");
                return;
            }

            Cliente nuevo = new Cliente(
                    id,
                    nombre,
                    paterno,
                    materno.isEmpty() ? null : materno,
                    telefono.isEmpty() ? null : telefono,
                    correo.isEmpty() ? null : correo);

            if (clienteDAO.insertar(nuevo)) {
                finalizarOperacion();

                mostrarAlerta(
                        "Registro exitoso",
                        "El cliente se registró correctamente.");
            } else {
                mostrarAlerta(
                        "Error",
                        "No se pudo registrar el cliente.");
            }

        } else {

            clienteSeleccionado.setNombre(nombre);
            clienteSeleccionado.setApellidoPaterno(paterno);
            clienteSeleccionado.setApellidoMaterno(
                    materno.isEmpty() ? null : materno);
            clienteSeleccionado.setTelefono(
                    telefono.isEmpty() ? null : telefono);
            clienteSeleccionado.setCorreo(
                    correo.isEmpty() ? null : correo);

            if (clienteDAO.actualizar(clienteSeleccionado)) {
                finalizarOperacion();

                mostrarAlerta(
                        "Actualización exitosa",
                        "El cliente se actualizó correctamente.");
            } else {
                mostrarAlerta(
                        "Error",
                        "No se pudo actualizar el cliente.");
            }
        }
    }

    @FXML
    private void eliminarCliente() {
        if (clienteSeleccionado == null) {
            return;
        }

        Alert confirmacion = new Alert(
                Alert.AlertType.CONFIRMATION,
                "¿Deseas eliminar al cliente "
                        + clienteSeleccionado.getNombre() + "?",
                ButtonType.YES,
                ButtonType.NO);

        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText(null);
        confirmacion.showAndWait();

        if (confirmacion.getResult() == ButtonType.YES) {

            if (clienteDAO.eliminar(
                    clienteSeleccionado.getIdCliente())) {

                finalizarOperacion();

                mostrarAlerta(
                        "Eliminación exitosa",
                        "El cliente se eliminó correctamente.");
            } else {
                mostrarAlerta(
                        "No se pudo eliminar",
                        "El cliente podría estar relacionado "
                                + "con otros registros.");
            }
        }
    }

    @FXML
    private void cancelarAccion() {
        finalizarOperacion();

        lblEstadoFooter.setText(
                listaClientes.size()
                        + (listaClientes.size() == 1
                                ? " cliente registrado"
                                : " clientes registrados"));
    }

    private void finalizarOperacion() {
        formularioActivo = false;
        clienteSeleccionado = null;

        limpiarCamposTexto();
        bloquearFormulario(true);

        tableClientes.getSelectionModel().clearSelection();

        btnEliminar.setDisable(true);
        btnGuardar.setDisable(true);
        btnCancelar.setDisable(true);

        cargarTabla();
    }

    private void limpiarCamposTexto() {
        txtNombre.clear();
        txtApellidoPaterno.clear();
        txtApellidoMaterno.clear();
        txtTelefono.clear();
        txtCorreo.clear();
    }

    private void bloquearFormulario(boolean bloquear) {
        txtNombre.setDisable(bloquear);
        txtApellidoPaterno.setDisable(bloquear);
        txtApellidoMaterno.setDisable(bloquear);
        txtTelefono.setDisable(bloquear);
        txtCorreo.setDisable(bloquear);

        btnGuardar.setDisable(true);
        btnCancelar.setDisable(bloquear);
    }

    private boolean formularioValido() {
        String nombre = txtNombre.getText().trim();
        String paterno = txtApellidoPaterno.getText().trim();
        String materno = txtApellidoMaterno.getText().trim();
        String telefono = txtTelefono.getText().trim();
        String correo = txtCorreo.getText().trim();

        boolean nombreValido = !nombre.isEmpty()
                && nombre.length() <= 100
                && nombre.matches("[\\p{L} ]+");

        boolean paternoValido = !paterno.isEmpty()
                && paterno.length() <= 100
                && paterno.matches("[\\p{L} ]+");

        boolean maternoValido = materno.isEmpty()
                || (materno.length() <= 100
                        && materno.matches("[\\p{L} ]+"));

        boolean telefonoValido = telefono.isEmpty()
                || telefono.matches("\\d{10}");

        boolean correoValido = correo.isEmpty()
                || (correo.length() <= 150
                        && correo.matches(
                                "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"));

        return nombreValido
                && paternoValido
                && maternoValido
                && telefonoValido
                && correoValido;
    }

    private void actualizarEstadoGuardar() {
        boolean valido = formularioActivo && formularioValido();
        btnGuardar.setDisable(!valido);
    }

    private void aplicarRestricciones() {

        TextFormatter<String> filtroNombre = new TextFormatter<>(cambio -> cambio.getControlNewText()
                .matches("[\\p{L} ]*")
                        ? cambio
                        : null);

        TextFormatter<String> filtroPaterno = new TextFormatter<>(cambio -> cambio.getControlNewText()
                .matches("[\\p{L} ]*")
                        ? cambio
                        : null);

        TextFormatter<String> filtroMaterno = new TextFormatter<>(cambio -> cambio.getControlNewText()
                .matches("[\\p{L} ]*")
                        ? cambio
                        : null);

        TextFormatter<String> filtroTelefono = new TextFormatter<>(cambio -> cambio.getControlNewText()
                .matches("\\d{0,10}")
                        ? cambio
                        : null);

        txtNombre.setTextFormatter(filtroNombre);
        txtApellidoPaterno.setTextFormatter(filtroPaterno);
        txtApellidoMaterno.setTextFormatter(filtroMaterno);
        txtTelefono.setTextFormatter(filtroTelefono);

        txtCorreo.textProperty().addListener(
                (obs, anterior, actual) -> actualizarEstadoGuardar());
    }

    private void configurarListeners() {
        txtNombre.textProperty().addListener(
                (obs, anterior, actual) -> actualizarEstadoGuardar());

        txtApellidoPaterno.textProperty().addListener(
                (obs, anterior, actual) -> actualizarEstadoGuardar());

        txtApellidoMaterno.textProperty().addListener(
                (obs, anterior, actual) -> actualizarEstadoGuardar());

        txtTelefono.textProperty().addListener(
                (obs, anterior, actual) -> actualizarEstadoGuardar());
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

            stage.setScene(new Scene(root));
            stage.setTitle(
                    "Sistema Abarrotes \"La Roca\" — Menú");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();

            mostrarAlerta(
                    "Error",
                    "No se pudo abrir el menú principal.");
        }
    }
}
