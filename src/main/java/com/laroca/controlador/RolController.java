package com.laroca.controlador;

import com.laroca.dao.RolDAO;
import com.laroca.modelo.Estados;
import com.laroca.modelo.Rol;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
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
    private TextField txtEstado;

    @FXML
    private Button btnNuevo;

    @FXML
    private Button btnEliminar;

    @FXML
    private Button btnGuardar;

    @FXML
    private Button btnCancelar;

    @FXML
    private CheckBox chkMostrarInactivos;

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
                        DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        columnaNombre.setCellValueFactory(
                new PropertyValueFactory<>("nombre"));

        columnaDescripcion.setCellValueFactory(
                new PropertyValueFactory<>("descripcion"));

        columnaEstado.setCellValueFactory(
                new PropertyValueFactory<>("estado"));

        // Nombre: solo letras, espacios y acentos.
        txtNombre.textProperty().addListener(
                (observable, anterior, actual) -> {
                    String nombreValido = actual.replaceAll("[^\\p{L} ]", "");

                    if (!actual.equals(nombreValido)) {
                        txtNombre.setText(nombreValido);
                        return;
                    }

                    actualizarEstadoGuardar();
                });

        // La descripción es obligatoria.
        txtDescripcion.textProperty().addListener(
                (observable, anterior, actual) -> actualizarEstadoGuardar());

        cargarRoles();

        tablaRoles.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, anterior, actual) -> {

                    if (actual != null) {
                        rolSeleccionado = actual;

                        txtNombre.setText(Objects.toString(actual.getNombre(), ""));
                        txtDescripcion.setText(Objects.toString(actual.getDescripcion(), ""));
                        txtEstado.setText(Estados.mostrar(actual.getEstado()));

                        if (chkMostrarInactivos.isSelected()) {
                            // Los roles inactivos solo se consultan.
                            modoEdicion = false;
                            bloquearFormulario(true);
                            btnCancelar.setDisable(false);
                        } else {
                            modoEdicion = true;
                            bloquearFormulario(false);
                        }

                        btnEliminar.setDisable(false);
                    } else {
                        btnEliminar.setDisable(true);
                    }
                });

        btnEliminar.setDisable(true);

        bloquearFormulario(true);
    }

    private void actualizarEstadoGuardar() {
        // Con el formulario bloqueado no se puede guardar.
        if (txtNombre.isDisabled()) {
            btnGuardar.setDisable(true);
            return;
        }

        String nombre = txtNombre.getText().trim();
        String descripcion = txtDescripcion.getText().trim();

        boolean nombreValido = !nombre.isEmpty()
                && nombre.matches("[\\p{L} ]+");

        boolean descripcionValida = !descripcion.isEmpty();

        boolean formularioValido = nombreValido
                && descripcionValida;

        btnGuardar.setDisable(!formularioValido);
    }

    // Muestra solo los activos o solo los inactivos, según la casilla.
    private void cargarRoles() {
        boolean inactivos = chkMostrarInactivos.isSelected();

        ObservableList<Rol> lista = FXCollections.observableArrayList();

        for (Rol rol : dao.listar()) {
            if (Estados.esActivo(rol.getEstado()) != inactivos) {
                lista.add(rol);
            }
        }

        tablaRoles.setItems(lista);

        int total = lista.size();

        lblRegistros.setText(
                total + (total == 1
                        ? (inactivos ? " rol inactivo" : " rol activo")
                        : (inactivos ? " roles inactivos" : " roles activos")));
    }

    @FXML
    private void nuevo() {
        if (chkMostrarInactivos.isSelected()) {
            return;
        }

        modoEdicion = false;
        rolSeleccionado = null;

        tablaRoles.getSelectionModel().clearSelection();

        limpiarFormulario();
        txtEstado.setText(Estados.ACTIVO);

        bloquearFormulario(false);

        btnEliminar.setDisable(true);

        txtNombre.requestFocus();
    }

    @FXML
    private void guardar() {
        String nombre = txtNombre.getText().trim();
        String descripcion = txtDescripcion.getText().trim();

        if (nombre.isEmpty()) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Campo obligatorio",
                    "Debes ingresar el nombre del rol.");
            txtNombre.requestFocus();
            return;
        }

        int idExcluir = 0;

        if (modoEdicion && rolSeleccionado != null) {
            idExcluir = rolSeleccionado.getIdRol();
        }

        // Se compara contra todos los roles, activos e inactivos.
        if (dao.existeNombre(nombre, idExcluir)) {
            if (dao.existeNombreInactivo(nombre, idExcluir)) {
                mostrarAlerta(
                        Alert.AlertType.WARNING,
                        "Rol inactivo",
                        "Ya existe un rol con ese nombre, pero está inactivo. "
                                + "Puedes reactivarlo desde \"Mostrar inactivos\".");
            } else {
                mostrarAlerta(
                        Alert.AlertType.WARNING,
                        "Nombre duplicado",
                        "Ya existe un rol con ese nombre. Escribe un nombre diferente.");
            }

            txtNombre.requestFocus();
            return;
        }

        if (modoEdicion) {
            if (rolSeleccionado == null) {
                mostrarAlerta(
                        Alert.AlertType.WARNING,
                        "Sin selección",
                        "Selecciona un rol para editar.");
                return;
            }

            // El estado no se modifica al editar.
            rolSeleccionado.setNombre(nombre);
            rolSeleccionado.setDescripcion(
                    descripcion.isEmpty() ? null : descripcion);

            if (dao.actualizar(rolSeleccionado)) {
                finalizarOperacion();

                mostrarAlerta(
                        Alert.AlertType.INFORMATION,
                        "Actualización exitosa",
                        "El rol se actualizó correctamente.");
            } else {
                mostrarAlerta(
                        Alert.AlertType.ERROR,
                        "Error",
                        "No se pudo actualizar el rol.");
            }

        } else {
            int id = dao.obtenerSiguienteId();

            if (id <= 0) {
                mostrarAlerta(
                        Alert.AlertType.ERROR,
                        "Error",
                        "No se pudo obtener el ID del rol.");
                return;
            }

            // Todo rol nuevo se registra como Activo.
            Rol rol = new Rol(id, Estados.ACTIVO, nombre,
                    descripcion.isEmpty() ? null : descripcion);

            if (dao.insertar(rol)) {
                finalizarOperacion();

                mostrarAlerta(
                        Alert.AlertType.INFORMATION,
                        "Registro exitoso",
                        "El rol se registró correctamente.");
            } else {
                mostrarAlerta(
                        Alert.AlertType.ERROR,
                        "Error",
                        "No se pudo registrar el rol.");
            }
        }
    }

    // Elimina (desactiva) o reactiva el rol, según la casilla.
    @FXML
    private void eliminar() {
        if (rolSeleccionado == null) {
            return;
        }

        boolean reactivar = chkMostrarInactivos.isSelected();

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);

        confirmacion.setTitle(reactivar ? "Reactivar rol" : "Eliminar rol");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText(reactivar
                ? "¿Deseas reactivar el rol \""
                        + rolSeleccionado.getNombre() + "\"?\n\n"
                        + "Volverá a estar activo y se podrá asignar a usuarios."
                : "¿Deseas eliminar el rol \""
                        + rolSeleccionado.getNombre() + "\"?\n\n"
                        + "Quedará inactivo y ya no se podrá asignar a usuarios. "
                        + "Los usuarios que ya lo tienen no cambian.");

        Optional<ButtonType> respuesta = confirmacion.showAndWait();

        if (respuesta.isPresent()
                && respuesta.get() == ButtonType.OK) {

            boolean exito = reactivar
                    ? dao.reactivar(rolSeleccionado.getIdRol())
                    : dao.desactivar(rolSeleccionado.getIdRol());

            if (exito) {
                finalizarOperacion();

                mostrarAlerta(
                        Alert.AlertType.INFORMATION,
                        reactivar ? "Reactivación exitosa" : "Eliminación exitosa",
                        reactivar
                                ? "El rol se reactivó correctamente."
                                : "El rol se marcó como inactivo.");
            } else {
                mostrarAlerta(
                        Alert.AlertType.ERROR,
                        "No se pudo cambiar el estado",
                        "No se pudo actualizar el estado del rol.");
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

        btnEliminar.setDisable(true);
    }

    private void limpiarFormulario() {
        txtNombre.clear();
        txtDescripcion.clear();
        txtEstado.clear();
    }

    private void bloquearFormulario(boolean bloquear) {
        txtNombre.setDisable(bloquear);
        txtDescripcion.setDisable(bloquear);

        btnGuardar.setDisable(true);
        btnCancelar.setDisable(bloquear);

        if (!bloquear) {
            actualizarEstadoGuardar();
        }
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
                getClass().getResource("/com/laroca/vista/menu.fxml"));

        Parent vista = loader.load();

        Stage ventanaActual = (Stage) txtNombre.getScene().getWindow();

        Navegacion.mostrar(ventanaActual, vista, Navegacion.TITULO_MENU);
    }
}
