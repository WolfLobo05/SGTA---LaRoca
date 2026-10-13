
package com.laroca.controlador;

import com.laroca.dao.RolDAO;
import com.laroca.modelo.Rol;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class RolController {

    @FXML
    private TableView<Rol> tablaRoles;

    @FXML
    private TableColumn<Rol, Integer> columnaId;

    @FXML
    private TableColumn<Rol, String> columnaNombre;

    @FXML
    private TableColumn<Rol, String> columnaDescripcion;

    @FXML
    private TableColumn<Rol, String> columnaEstado;

    @FXML
    private TextField txtNombre;

    @FXML
    private TextField txtDescripcion;

    @FXML
    private ComboBox<String> cmbEstado;

    @FXML
    private Button btnEditar;

    @FXML
    private Button btnEliminar;

    @FXML
    private Button btnGuardar;

    @FXML
    private Button btnCancelar;

    @FXML
    private Label lblFecha;

    @FXML
    private Label lblRegistros;

    private final RolDAO dao = new RolDAO();

    private Rol rolSeleccionado;

    private boolean modoEdicion = false;

    @FXML
    public void initialize() {

        lblFecha.setText(
            LocalDate.now().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy")
            )
        );

        columnaId.setCellValueFactory(
            new PropertyValueFactory<>("idRol")
        );

        columnaNombre.setCellValueFactory(
            new PropertyValueFactory<>("nombre")
        );

        columnaDescripcion.setCellValueFactory(
            new PropertyValueFactory<>("descripcion")
        );

        columnaEstado.setCellValueFactory(
            new PropertyValueFactory<>("estado")
        );

        cmbEstado.setItems(
            FXCollections.observableArrayList("Activo", "Inactivo")
        );

        cargarRoles();

        tablaRoles.getSelectionModel()
            .selectedItemProperty()
            .addListener((observable, anterior, actual) -> {

                if (actual != null) {
                    rolSeleccionado = actual;

                    txtNombre.setText(actual.getNombre());
                    txtDescripcion.setText(actual.getDescripcion());
                    cmbEstado.setValue(actual.getEstado());

                    btnEditar.setDisable(false);
                    btnEliminar.setDisable(false);
                } else {
                    btnEditar.setDisable(true);
                    btnEliminar.setDisable(true);
                }
            });

        btnEditar.setDisable(true);
        btnEliminar.setDisable(true);

        bloquearFormulario(true);
    }

    private void cargarRoles() {
        ObservableList<Rol> lista =
            FXCollections.observableArrayList(dao.listar());

        tablaRoles.setItems(lista);

        int total = lista.size();

        lblRegistros.setText(
            total + (total == 1
                ? " rol registrado"
                : " roles registrados")
        );
    }

    @FXML
    private void nuevo() {
        modoEdicion = false;
        rolSeleccionado = null;

        tablaRoles.getSelectionModel().clearSelection();

        limpiarFormulario();
        cmbEstado.setValue("Activo");

        bloquearFormulario(false);

        btnEditar.setDisable(true);
        btnEliminar.setDisable(true);

        txtNombre.requestFocus();
    }

    @FXML
    private void editar() {
        if (rolSeleccionado == null) {
            mostrarAlerta(
                Alert.AlertType.WARNING,
                "Sin selección",
                "Selecciona primero un rol."
            );
            return;
        }

        modoEdicion = true;

        txtNombre.setText(rolSeleccionado.getNombre());
        txtDescripcion.setText(rolSeleccionado.getDescripcion());
        cmbEstado.setValue(rolSeleccionado.getEstado());

        bloquearFormulario(false);
        txtNombre.requestFocus();
    }

    @FXML
    private void guardar() {
        String nombre = txtNombre.getText().trim();
        String descripcion = txtDescripcion.getText().trim();
        String estado = cmbEstado.getValue();

        if (nombre.isEmpty()) {
            mostrarAlerta(
                Alert.AlertType.WARNING,
                "Campo obligatorio",
                "Debes ingresar el nombre del rol."
            );
            txtNombre.requestFocus();
            return;
        }

        if (estado == null || estado.isBlank()) {
            mostrarAlerta(
                Alert.AlertType.WARNING,
                "Estado obligatorio",
                "Selecciona el estado del rol."
            );
            return;
        }

        if (modoEdicion) {
            if (rolSeleccionado == null) {
                mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Sin selección",
                    "Selecciona un rol para editar."
                );
                return;
            }

            rolSeleccionado.setNombre(nombre);
            rolSeleccionado.setDescripcion(
                descripcion.isEmpty() ? null : descripcion
            );
            rolSeleccionado.setEstado(estado);

            if (dao.actualizar(rolSeleccionado)) {
                finalizarOperacion();

                mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Actualización exitosa",
                    "El rol se actualizó correctamente."
                );
            } else {
                mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error",
                    "No se pudo actualizar el rol."
                );
            }

        } else {
            int id = dao.obtenerSiguienteId();

            if (id <= 0) {
                mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error",
                    "No se pudo obtener el ID del rol."
                );
                return;
            }

            Rol rol = new Rol(id, estado, nombre,
                descripcion.isEmpty() ? null : descripcion);

            if (dao.insertar(rol)) {
                finalizarOperacion();

                mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Registro exitoso",
                    "El rol se registró correctamente."
                );
            } else {
                mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error",
                    "No se pudo registrar el rol."
                );
            }
        }
    }

    @FXML
    private void eliminar() {
        if (rolSeleccionado == null) {
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);

        confirmacion.setTitle("Eliminar rol");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText(
            "¿Deseas eliminar el rol \""
                + rolSeleccionado.getNombre() + "\"?"
        );

        Optional<ButtonType> respuesta = confirmacion.showAndWait();

        if (respuesta.isPresent()
                && respuesta.get() == ButtonType.OK) {

            if (dao.eliminar(rolSeleccionado.getIdRol())) {
                finalizarOperacion();

                mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Eliminación exitosa",
                    "El rol se eliminó correctamente."
                );
            } else {
                mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "No se pudo eliminar",
                    "El rol podría estar asociado a uno o más empleados."
                );
            }
        }
    }

    @FXML
    private void cancelar() {
        finalizarOperacion();
    }

    private void finalizarOperacion() {
        modoEdicion = false;
        rolSeleccionado = null;

        limpiarFormulario();
        bloquearFormulario(true);

        cargarRoles();

        tablaRoles.getSelectionModel().clearSelection();

        btnEditar.setDisable(true);
        btnEliminar.setDisable(true);
    }

    private void limpiarFormulario() {
        txtNombre.clear();
        txtDescripcion.clear();
        cmbEstado.getSelectionModel().clearSelection();
    }

    private void bloquearFormulario(boolean bloquear) {
        txtNombre.setDisable(bloquear);
        txtDescripcion.setDisable(bloquear);
        cmbEstado.setDisable(bloquear);
        btnGuardar.setDisable(bloquear);
        btnCancelar.setDisable(bloquear);
    }

    private void mostrarAlerta(
            Alert.AlertType tipo,
            String titulo,
            String mensaje) {

        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    @FXML
    private void volverAlMenu() throws IOException {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/com/laroca/vista/menu.fxml")
        );

        Parent vista = loader.load();

        Stage ventanaActual =
            (Stage) txtNombre.getScene().getWindow();

        ventanaActual.setScene(new Scene(vista));
        ventanaActual.setTitle("Sistema Abarrotes La Roca");
        ventanaActual.setResizable(false);
    }
}
