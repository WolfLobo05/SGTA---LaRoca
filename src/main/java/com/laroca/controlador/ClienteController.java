package com.laroca.controlador;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import com.laroca.dao.ClienteDAO;
import com.laroca.modelo.Cliente;
import com.laroca.modelo.Estados;

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
    private CheckBox chkMostrarInactivos;

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
    private TextField txtEstado;

    @FXML
    private TableView<Cliente> tableClientes;
    @FXML
    private TableColumn<Cliente, String> colNombre;
    @FXML
    private TableColumn<Cliente, String> colApellidoPaterno;
    @FXML
    private TableColumn<Cliente, String> colApellidoMaterno;
    @FXML
    private TableColumn<Cliente, String> colTelefono;
    @FXML
    private TableColumn<Cliente, String> colCorreo;
    @FXML
    private TableColumn<Cliente, String> colEstado;

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

        colNombre.setCellValueFactory(
                new PropertyValueFactory<>("nombre"));
        colApellidoPaterno.setCellValueFactory(
                new PropertyValueFactory<>("apellidoPaterno"));
        colApellidoMaterno.setCellValueFactory(
                new PropertyValueFactory<>("apellidoMaterno"));
        colTelefono.setCellValueFactory(
                new PropertyValueFactory<>("telefono"));
        colCorreo.setCellValueFactory(
                new PropertyValueFactory<>("correo"));
        colEstado.setCellValueFactory(
                new PropertyValueFactory<>("estado"));

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

                        mostrarCliente(seleccionado);
                        btnEliminar.setDisable(false);

                        if (chkMostrarInactivos.isSelected()) {
                            // Los clientes inactivos solo se consultan.
                            formularioActivo = false;
                            bloquearFormulario(true);
                            btnCancelar.setDisable(false);

                            lblEstadoFooter.setText(
                                    "Modo: Consultando cliente inactivo");
                        } else {
                            formularioActivo = true;
                            bloquearFormulario(false);
                            btnCancelar.setDisable(false);

                            actualizarEstadoGuardar();

                            lblEstadoFooter.setText(
                                    "Modo: Editando cliente");
                        }
                    } else {
                        btnEliminar.setDisable(true);
                    }
                });
    }

    // Muestra solo los activos o solo los inactivos, según la casilla.
    private void cargarTabla() {
        boolean inactivos = chkMostrarInactivos.isSelected();

        listaClientes = FXCollections.observableArrayList();

        for (Cliente cliente : clienteDAO.listar()) {
            if (Estados.esActivo(cliente.getEstado()) != inactivos) {
                listaClientes.add(cliente);
            }
        }

        tableClientes.setItems(listaClientes);

        actualizarPie();
    }

    private void actualizarPie() {
        boolean inactivos = chkMostrarInactivos.isSelected();
        int total = listaClientes.size();

        lblEstadoFooter.setText(
                total + (total == 1
                        ? (inactivos ? " cliente inactivo" : " cliente activo")
                        : (inactivos ? " clientes inactivos" : " clientes activos")));
    }

    private void mostrarCliente(Cliente cliente) {
        txtNombre.setText(Objects.toString(cliente.getNombre(), ""));
        txtApellidoPaterno.setText(
                Objects.toString(cliente.getApellidoPaterno(), ""));
        txtApellidoMaterno.setText(
                Objects.toString(cliente.getApellidoMaterno(), ""));
        txtTelefono.setText(Objects.toString(cliente.getTelefono(), ""));
        txtCorreo.setText(Objects.toString(cliente.getCorreo(), ""));
        txtEstado.setText(Estados.mostrar(cliente.getEstado()));
    }

    @FXML
    private void nuevoCliente() {
        if (chkMostrarInactivos.isSelected()) {
            return;
        }

        clienteSeleccionado = null;
        formularioActivo = true;

        tableClientes.getSelectionModel().clearSelection();

        limpiarCamposTexto();
        txtEstado.setText(Estados.ACTIVO);
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

        // Se compara contra todos los clientes, activos e inactivos.
        if (clienteDAO.existeCliente(
                nombre, paterno, materno,
                telefono, correo, idExcluir)) {

            if (clienteDAO.existeTelefonoInactivo(telefono, idExcluir)) {
                mostrarAlerta(
                        "Cliente inactivo",
                        "Ya existe un cliente con ese teléfono, pero está inactivo. "
                                + "Puedes reactivarlo desde \"Mostrar inactivos\".");
            } else {
                mostrarAlerta(
                        "Teléfono duplicado",
                        "Ya existe otro cliente registrado con ese teléfono.");
            }
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
                    telefono,
                    correo);

            // Todo cliente nuevo se registra como Activo.
            nuevo.setEstado(Estados.ACTIVO);

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
            clienteSeleccionado.setTelefono(telefono);
            clienteSeleccionado.setCorreo(correo);

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

    // Elimina (desactiva) o reactiva el cliente, según la casilla.
    @FXML
    private void eliminarCliente() {
        if (clienteSeleccionado == null) {
            return;
        }

        boolean reactivar = chkMostrarInactivos.isSelected();
        String nombre = clienteSeleccionado.getNombre() + " "
                + Objects.toString(clienteSeleccionado.getApellidoPaterno(), "");

        Alert confirmacion = new Alert(
                Alert.AlertType.CONFIRMATION,
                reactivar
                        ? "¿Deseas reactivar al cliente " + nombre.trim() + "?\n\n"
                                + "Volverá a estar activo."
                        : "¿Deseas eliminar al cliente " + nombre.trim() + "?\n\n"
                                + "Quedará inactivo y dejará de aparecer "
                                + "en la lista de clientes activos.",
                ButtonType.YES,
                ButtonType.NO);

        confirmacion.setTitle(reactivar
                ? "Confirmar reactivación"
                : "Confirmar eliminación");
        confirmacion.setHeaderText(null);
        confirmacion.showAndWait();

        if (confirmacion.getResult() == ButtonType.YES) {

            boolean exito = reactivar
                    ? clienteDAO.reactivar(clienteSeleccionado.getIdCliente())
                    : clienteDAO.desactivar(clienteSeleccionado.getIdCliente());

            if (exito) {

                finalizarOperacion();

                mostrarAlerta(
                        reactivar ? "Reactivación exitosa" : "Eliminación exitosa",
                        reactivar
                                ? "El cliente se reactivó correctamente."
                                : "El cliente se marcó como inactivo.");
            } else {
                mostrarAlerta(
                        "No se pudo cambiar el estado",
                        "No se pudo actualizar el estado del cliente.");
            }
        }
    }

    // Cambia entre la lista de activos y la de inactivos.
    @FXML
    private void cambiarMostrarInactivos() {
        boolean inactivos = chkMostrarInactivos.isSelected();

        btnNuevo.setDisable(inactivos);
        btnEliminar.setText(inactivos ? "Reactivar" : "Eliminar");

        finalizarOperacion();
    }

    @FXML
    private void cancelarAccion() {
        finalizarOperacion();
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
        txtEstado.clear();
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

        // Teléfono y correo son obligatorios.
        boolean telefonoValido = telefono.matches("\\d{10}");

        boolean correoValido = correo.length() <= 150
                && correo.matches(
                        "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

        return nombreValido
                && paternoValido
                && maternoValido
                && telefonoValido
                && correoValido;
    }

    private void actualizarEstadoGuardar() {
        boolean valido = formularioActivo
                && !txtNombre.isDisabled()
                && formularioValido();
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

            Navegacion.mostrar(stage, root, Navegacion.TITULO_MENU);

        } catch (IOException e) {
            e.printStackTrace();

            mostrarAlerta(
                    "Error",
                    "No se pudo abrir el menú principal.");
        }
    }
}
