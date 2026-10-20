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
    private Proveedor proveedorSeleccionado = null;

    @FXML
    public void initialize() {
        // 1. Mostrar fecha real de la laptop
        lblFechaActual.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        // 2. Configurar columnas de la tabla
        colRfc.setCellValueFactory(new PropertyValueFactory<>("rfc"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colContacto.setCellValueFactory(new PropertyValueFactory<>("contacto"));
        colCorreo.setCellValueFactory(new PropertyValueFactory<>("correo"));
        colDireccion.setCellValueFactory(new PropertyValueFactory<>("direccion"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));

        // 3. Cargar datos
        cargarTabla();

        // 4. Estado inicial: campos bloqueados y botones restringidos
        bloquearFormulario(true);
        btnEliminar.setDisable(true);
        btnGuardar.setDisable(true);

        // 5. Listener para seleccionar de la tabla y habilitar edición/eliminación
        tableProveedores.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                proveedorSeleccionado = newSelection;
                
                // Desbloquear para permitir modificar el registro seleccionado
                bloquearFormulario(false);
                btnGuardar.setDisable(false);

                txtRfc.setText(newSelection.getRfc());
                txtNombre.setText(newSelection.getNombre());
                txtContacto.setText(newSelection.getContacto());
                txtCorreo.setText(newSelection.getCorreo());
                txtDireccion.setText(newSelection.getDireccion());
                txtTelefono.setText(newSelection.getTelefono());

                btnEliminar.setDisable(false);
            } else {
                btnEliminar.setDisable(true);
            }
        });

        // 6. Aplicar validaciones estrictas por tipo de dato
        aplicarRestricciones();
    }

    private void cargarTabla() {
        listaProveedores = FXCollections.observableArrayList(proveedorDAO.listar());
        tableProveedores.setItems(listaProveedores);
        lblEstadoFooter.setText(listaProveedores.size() + " proveedores registrados");
    }

    @FXML
    private void guardarProveedor() {
        String rfc = txtRfc.getText().trim();
        String nombre = txtNombre.getText().trim();
        String contacto = txtContacto.getText().trim();
        String correo = txtCorreo.getText().trim();
        String direccion = txtDireccion.getText().trim();
        String telefono = txtTelefono.getText().trim();

        if (nombre.isEmpty()) {
            mostrarAlerta("Campo Requerido", "El campo Nombre es obligatorio.");
            return;
        }

        if (proveedorSeleccionado == null) {
            // INSERTAR NUEVO
            Proveedor nuevo = new Proveedor(0, rfc, nombre, telefono, correo, direccion, contacto, "ACTIVO");
            if (proveedorDAO.guardar(nuevo)) {
                lblEstadoFooter.setText("Proveedor guardado correctamente.");
                limpiarFormulario();
                cargarTabla();
            } else {
                mostrarAlerta("Error", "No se pudo guardar el proveedor.");
            }
        } else {
            // ACTUALIZAR EXISTENTE (Usa su ID sin duplicar)
            proveedorSeleccionado.setRfc(rfc);
            proveedorSeleccionado.setNombre(nombre);
            proveedorSeleccionado.setContacto(contacto);
            proveedorSeleccionado.setCorreo(correo);
            proveedorSeleccionado.setDireccion(direccion);
            proveedorSeleccionado.setTelefono(telefono);

            if (proveedorDAO.actualizar(proveedorSeleccionado)) {
                lblEstadoFooter.setText("Proveedor actualizado correctamente.");
                limpiarFormulario();
                cargarTabla();
            } else {
                mostrarAlerta("Error", "No se pudo actualizar el proveedor.");
            }
        }
    }

    @FXML
    private void eliminarProveedor() {
        if (proveedorSeleccionado != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Deseas eliminar el proveedor " + proveedorSeleccionado.getNombre() + "?", ButtonType.YES, ButtonType.NO);
            confirm.showAndWait();

            if (confirm.getResult() == ButtonType.YES) {
                if (proveedorDAO.eliminar(proveedorSeleccionado.getIdProveedor())) {
                    lblEstadoFooter.setText("Proveedor eliminado.");
                    limpiarFormulario();
                    cargarTabla();
                } else {
                    mostrarAlerta("Error", "No se pudo eliminar el proveedor.");
                }
            }
        }
    }

    @FXML
    private void nuevoProveedor() {
        proveedorSeleccionado = null; // Indica que es un registro nuevo
        tableProveedores.getSelectionModel().clearSelection();
        limpiarCamposTexto();
        bloquearFormulario(false); // Habilita la escritura
        btnGuardar.setDisable(false);
        btnEliminar.setDisable(true);
        txtRfc.requestFocus();
        lblEstadoFooter.setText("Modo: Registrando nuevo proveedor");
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
        bloquearFormulario(true); // Bloquea los campos nuevamente
        btnEliminar.setDisable(true);
        btnGuardar.setDisable(true);
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
    }

    @FXML
    private void volverAlMenu(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/laroca/vista/menu.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Sistema Abarrotes \"La Roca\" — Menú");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void aplicarRestricciones() {
        // RFC: Máximo 13 caracteres, convierte a mayúsculas automáticamente
        txtRfc.textProperty().addListener((obs, oldText, newText) -> {
            if (newText.length() > 13) {
                txtRfc.setText(oldText);
            } else {
                txtRfc.setText(newText.toUpperCase());
            }
        });

        // Nombre: Máximo 100 caracteres (VARCHAR 100)
        txtNombre.textProperty().addListener((obs, oldText, newText) -> {
            if (newText.length() > 100) txtNombre.setText(oldText);
        });

        // Contacto: SOLO LETRAS, espacios y acentos (máximo 15 caracteres)
        txtContacto.textProperty().addListener((obs, oldText, newText) -> {
            if (!newText.matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]*$") || newText.length() > 15) {
                txtContacto.setText(oldText);
            }
        });

        // Correo: Máximo 150 caracteres (VARCHAR 150)
        txtCorreo.textProperty().addListener((obs, oldText, newText) -> {
            if (newText.length() > 150) txtCorreo.setText(oldText);
        });

        // Dirección: Máximo 100 caracteres (VARCHAR 100)
        txtDireccion.textProperty().addListener((obs, oldText, newText) -> {
            if (newText.length() > 100) txtDireccion.setText(oldText);
        });

        // Teléfono: SOLO NÚMEROS (máximo 15 dígitos)
        txtTelefono.textProperty().addListener((obs, oldText, newText) -> {
            if (!newText.matches("\\d*") || newText.length() > 15) {
                txtTelefono.setText(oldText);
            }
        });
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}