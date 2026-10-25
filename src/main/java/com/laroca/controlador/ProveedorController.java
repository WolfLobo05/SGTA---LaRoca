package com.laroca.controlador;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.laroca.dao.ProveedorDAO;
import com.laroca.modelo.Proveedor;

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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class ProveedorController {

    @FXML private Button btnNuevo;
    @FXML private Button btnEliminar;
    @FXML private Button btnVolverMenu;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;

    @FXML private TextField txtRfc;
    @FXML private TextField txtNombre;
    @FXML private TextField txtContacto;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtDireccion;
    @FXML private TextField txtTelefono;

    @FXML private TableView<Proveedor> tableProveedores;
    @FXML private TableColumn<Proveedor, String> colRfc;
    @FXML private TableColumn<Proveedor, String> colNombre;
    @FXML private TableColumn<Proveedor, String> colContacto;
    @FXML private TableColumn<Proveedor, String> colCorreo;
    @FXML private TableColumn<Proveedor, String> colDireccion;
    @FXML private TableColumn<Proveedor, String> colTelefono;

    @FXML private Label lblFechaActual;
    @FXML private Label lblEstadoFooter;

    private final ProveedorDAO proveedorDAO = new ProveedorDAO();
    private ObservableList<Proveedor> listaProveedores;
    private Proveedor proveedorSeleccionado;

    @FXML
    public void initialize() {
        lblFechaActual.setText(
                LocalDate.now().format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        colRfc.setCellValueFactory(new PropertyValueFactory<>("rfc"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colContacto.setCellValueFactory(new PropertyValueFactory<>("contacto"));
        colCorreo.setCellValueFactory(new PropertyValueFactory<>("correo"));
        colDireccion.setCellValueFactory(new PropertyValueFactory<>("direccion"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));

        aplicarRestricciones();
        cargarTabla();
        bloquearFormulario(true);

        btnEliminar.setDisable(true);
        btnGuardar.setDisable(true);
        btnCancelar.setDisable(true);

        tableProveedores.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, anterior, seleccionado) -> {
                    if (seleccionado != null) {
                        proveedorSeleccionado = seleccionado;

                        txtRfc.setText(seleccionado.getRfc());
                        txtNombre.setText(seleccionado.getNombre());
                        txtContacto.setText(seleccionado.getContacto());
                        txtCorreo.setText(seleccionado.getCorreo());
                        txtDireccion.setText(seleccionado.getDireccion());
                        txtTelefono.setText(seleccionado.getTelefono());

                        bloquearFormulario(false);
                        btnCancelar.setDisable(false);
                        btnEliminar.setDisable(false);

                        actualizarEstadoGuardar();
                        lblEstadoFooter.setText("Modo: Editando proveedor");
                    } else {
                        btnEliminar.setDisable(true);
                    }
                });
    }

    private void cargarTabla() {
        listaProveedores =
                FXCollections.observableArrayList(proveedorDAO.listar());

        tableProveedores.setItems(listaProveedores);

        lblEstadoFooter.setText(
                listaProveedores.size() + " proveedores registrados");
    }

    @FXML
    private void nuevoProveedor() {
        proveedorSeleccionado = null;
        tableProveedores.getSelectionModel().clearSelection();

        limpiarCamposTexto();
        bloquearFormulario(false);

        btnEliminar.setDisable(true);
        btnCancelar.setDisable(false);

        actualizarEstadoGuardar();

        txtRfc.requestFocus();
        lblEstadoFooter.setText("Modo: Registrando nuevo proveedor");
    }

    @FXML
    private void guardarProveedor() {
        String rfc = txtRfc.getText().trim().toUpperCase();
        String nombre = txtNombre.getText().trim();
        String contacto = txtContacto.getText().trim();
        String correo = txtCorreo.getText().trim();
        String direccion = txtDireccion.getText().trim();
        String telefono = txtTelefono.getText().trim();

        // Validación final antes de guardar.
        if (!formularioValido()) {
            mostrarAlerta(
                    "Datos incompletos o inválidos",
                    "Completa todos los campos correctamente antes de guardar.");
            actualizarEstadoGuardar();
            return;
        }

        // Comprobar RFC duplicado, excepto el proveedor que se está editando.
        boolean rfcDuplicado = proveedorDAO.listar().stream()
                .anyMatch(p ->
                        p.getRfc() != null
                        && p.getRfc().trim().equalsIgnoreCase(rfc)
                        && (proveedorSeleccionado == null
                            || p.getIdProveedor()
                               != proveedorSeleccionado.getIdProveedor()));

        if (rfcDuplicado) {
            mostrarAlerta(
                    "RFC duplicado",
                    "Ya existe un proveedor registrado con ese RFC.");
            txtRfc.requestFocus();
            txtRfc.selectAll();
            return;
        }

        if (proveedorSeleccionado == null) {
            Proveedor nuevo = new Proveedor(
                    0, rfc, nombre, telefono, correo,
                    direccion, contacto, "ACTIVO");

            if (proveedorDAO.guardar(nuevo)) {
                mostrarAlerta(
                        "Registro exitoso",
                        "El proveedor se registró correctamente.");
                limpiarFormulario();
                cargarTabla();
            } else {
                mostrarAlerta(
                        "Error",
                        "No se pudo guardar el proveedor. "
                        + "Comprueba los datos y la conexión.");
            }
        } else {
            proveedorSeleccionado.setRfc(rfc);
            proveedorSeleccionado.setNombre(nombre);
            proveedorSeleccionado.setContacto(contacto);
            proveedorSeleccionado.setCorreo(correo);
            proveedorSeleccionado.setDireccion(direccion);
            proveedorSeleccionado.setTelefono(telefono);

            if (proveedorDAO.actualizar(proveedorSeleccionado)) {
                mostrarAlerta(
                        "Actualización exitosa",
                        "El proveedor se actualizó correctamente.");
                limpiarFormulario();
                cargarTabla();
            } else {
                mostrarAlerta(
                        "Error",
                        "No se pudo actualizar el proveedor.");
            }
        }
    }

    @FXML
    private void eliminarProveedor() {
        if (proveedorSeleccionado == null) {
            return;
        }

        Alert confirmacion = new Alert(
                Alert.AlertType.CONFIRMATION,
                "¿Deseas eliminar el proveedor "
                        + proveedorSeleccionado.getNombre() + "?",
                ButtonType.YES,
                ButtonType.NO);

        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText(null);
        confirmacion.showAndWait();

        if (confirmacion.getResult() == ButtonType.YES) {
            if (proveedorDAO.eliminar(
                    proveedorSeleccionado.getIdProveedor())) {

                limpiarFormulario();
                cargarTabla();

                mostrarAlerta(
                        "Eliminación exitosa",
                        "El proveedor se eliminó correctamente.");
            } else {
                mostrarAlerta(
                        "No se pudo eliminar",
                        "El proveedor podría estar relacionado con otros registros.");
            }
        }
    }

    @FXML
    private void cancelarAccion() {
        limpiarFormulario();
        lblEstadoFooter.setText("Acción cancelada.");
    }

    @FXML
    private void limpiarFormulario() {
        proveedorSeleccionado = null;
        tableProveedores.getSelectionModel().clearSelection();

        limpiarCamposTexto();
        bloquearFormulario(true);

        btnEliminar.setDisable(true);
        btnGuardar.setDisable(true);
        btnCancelar.setDisable(true);
    }

    private void limpiarCamposTexto() {
        txtRfc.clear();
        txtNombre.clear();
        txtContacto.clear();
        txtCorreo.clear();
        txtDireccion.clear();
        txtTelefono.clear();
    }

    private void bloquearFormulario(boolean bloquear) {
        txtRfc.setDisable(bloquear);
        txtNombre.setDisable(bloquear);
        txtContacto.setDisable(bloquear);
        txtCorreo.setDisable(bloquear);
        txtDireccion.setDisable(bloquear);
        txtTelefono.setDisable(bloquear);

        btnGuardar.setDisable(true);
        btnCancelar.setDisable(bloquear);
    }

    private boolean formularioValido() {
        String rfc = txtRfc.getText().trim();
        String nombre = txtNombre.getText().trim();
        String contacto = txtContacto.getText().trim();
        String correo = txtCorreo.getText().trim();
        String direccion = txtDireccion.getText().trim();
        String telefono = txtTelefono.getText().trim();

        return rfc.matches(
                    "(?:[A-ZÑ&]{3}\\d{6}[A-Z0-9]{3}|"
                    + "[A-ZÑ&]{4}\\d{6}[A-Z0-9]{3})")
                && !nombre.isBlank()
                && nombre.length() <= 100
                && !contacto.isBlank()
                && contacto.matches("[\\p{L} ]{1,15}")
                && correo.matches(
                    "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                && correo.length() <= 150
                && !direccion.isBlank()
                && direccion.length() <= 100
                && telefono.matches("\\d{10}");
    }

    private void actualizarEstadoGuardar() {
        btnGuardar.setDisable(!formularioValido());
    }

    private void aplicarRestricciones() {
        // RFC: letras, números y &, en mayúsculas; máximo 13 caracteres.
        txtRfc.textProperty().addListener((obs, anterior, actual) -> {
            String corregido = actual.toUpperCase();

            if (!corregido.matches("[A-ZÑ&0-9]*")
                    || corregido.length() > 13) {
                txtRfc.setText(anterior);
                return;
            }

            if (!actual.equals(corregido)) {
                txtRfc.setText(corregido);
                return;
            }

            actualizarEstadoGuardar();
        });

        // Nombre comercial: admite letras, números y signos razonables.
        txtNombre.textProperty().addListener((obs, anterior, actual) -> {
            if (actual.length() > 100) {
                txtNombre.setText(anterior);
                return;
            }
            actualizarEstadoGuardar();
        });

        // Contacto: letras, acentos y espacios; máximo 15 caracteres.
        txtContacto.textProperty().addListener((obs, anterior, actual) -> {
            if (!actual.matches("[\\p{L} ]{0,15}")) {
                txtContacto.setText(anterior);
                return;
            }
            actualizarEstadoGuardar();
        });

        // Correo: máximo 150 caracteres.
        txtCorreo.textProperty().addListener((obs, anterior, actual) -> {
            if (actual.length() > 150) {
                txtCorreo.setText(anterior);
                return;
            }
            actualizarEstadoGuardar();
        });

        // Dirección: máximo 100 caracteres.
        txtDireccion.textProperty().addListener((obs, anterior, actual) -> {
            if (actual.length() > 100) {
                txtDireccion.setText(anterior);
                return;
            }
            actualizarEstadoGuardar();
        });

        // Teléfono: solo números, máximo 10 dígitos.
        txtTelefono.textProperty().addListener((obs, anterior, actual) -> {
            if (!actual.matches("\\d{0,10}")) {
                txtTelefono.setText(anterior);
                return;
            }
            actualizarEstadoGuardar();
        });
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
            stage.setTitle("Sistema Abarrotes \"La Roca\" — Menú");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta(
                    "Error",
                    "No se pudo abrir el menú principal.");
        }
    }
}
