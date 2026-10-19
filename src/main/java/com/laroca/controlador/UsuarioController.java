package com.laroca.controlador;

import com.laroca.dao.RolDAO;
import com.laroca.dao.UsuarioDAO;
import com.laroca.modelo.Rol;
import com.laroca.modelo.Usuario;

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
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class UsuarioController {

    private static final String PROMPT_PASSWORD_NUEVO = "Contraseña";

    private static final String PROMPT_PASSWORD_EDITAR = "Dejar vacío para no cambiarla";

    @FXML
    private TableView<Usuario> tablaUsuarios;

    @FXML
    private TableColumn<Usuario, Integer> columnaId;

    @FXML
    private TableColumn<Usuario, String> columnaUsuario;

    @FXML
    private TableColumn<Usuario, String> columnaNombreCompleto;

    @FXML
    private TableColumn<Usuario, String> columnaRol;

    @FXML
    private TableColumn<Usuario, String> columnaTelefono;

    @FXML
    private TableColumn<Usuario, String> columnaCorreo;

    @FXML
    private TextField txtUsuario;

    @FXML
    private PasswordField txtPassword;

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
    private ComboBox<Rol> cmbRol;

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

    private final UsuarioDAO dao = new UsuarioDAO();

    private final RolDAO rolDao = new RolDAO();

    private Usuario usuarioSeleccionado;

    private boolean modoEdicion = false;

    @FXML
    public void initialize() {

        lblFecha.setText(
                LocalDate.now().format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        columnaId.setCellValueFactory(
                new PropertyValueFactory<>("idUsuario"));

        columnaUsuario.setCellValueFactory(
                new PropertyValueFactory<>("usuario"));

        columnaNombreCompleto.setCellValueFactory(
                new PropertyValueFactory<>("nombreCompleto"));

        columnaRol.setCellValueFactory(
                new PropertyValueFactory<>("nombreRol"));

        columnaTelefono.setCellValueFactory(
                new PropertyValueFactory<>("telefono"));

        columnaCorreo.setCellValueFactory(
                new PropertyValueFactory<>("correo"));

        // El ComboBox muestra el nombre de cada rol.
        cmbRol.setConverter(new StringConverter<Rol>() {
            @Override
            public String toString(Rol rol) {
                return rol == null ? "" : rol.getNombre();
            }

            @Override
            public Rol fromString(String texto) {
                return null;
            }
        });

        cargarRoles();
        cargarUsuarios();

        tablaUsuarios.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, anterior, actual) -> {

                    if (actual != null) {
                        usuarioSeleccionado = actual;
                        modoEdicion = true;

                        mostrarUsuario(actual);
                        txtPassword.setPromptText(PROMPT_PASSWORD_EDITAR);

                        bloquearFormulario(false);
                        btnEliminar.setDisable(false);
                    } else {
                        btnEliminar.setDisable(true);
                    }
                });

        btnEliminar.setDisable(true);

        bloquearFormulario(true);
    }

    private void cargarUsuarios() {
        ObservableList<Usuario> lista = FXCollections.observableArrayList(dao.listar());

        tablaUsuarios.setItems(lista);

        int total = lista.size();

        lblRegistros.setText(
                total + (total == 1
                        ? " usuario registrado"
                        : " usuarios registrados"));
    }

    // Solo se ofrecen los roles activos.
    private void cargarRoles() {
        ObservableList<Rol> lista = FXCollections.observableArrayList();

        for (Rol rol : rolDao.listar()) {
            if ("Activo".equals(rol.getEstado())) {
                lista.add(rol);
            }
        }

        cmbRol.setItems(lista);
    }

    // La contraseña nunca se muestra.
    private void mostrarUsuario(Usuario usuario) {
        txtUsuario.setText(Objects.toString(usuario.getUsuario(), ""));
        txtPassword.clear();
        txtNombre.setText(Objects.toString(usuario.getNombre(), ""));
        txtApellidoPaterno.setText(Objects.toString(usuario.getApellidoPaterno(), ""));
        txtApellidoMaterno.setText(Objects.toString(usuario.getApellidoMaterno(), ""));
        txtTelefono.setText(Objects.toString(usuario.getTelefono(), ""));
        txtCorreo.setText(Objects.toString(usuario.getCorreo(), ""));

        seleccionarRol(usuario.getIdRol(), usuario.getNombreRol());
    }

    // Si el rol está inactivo no aparece en la lista,
    // así que se muestra con un objeto temporal.
    private void seleccionarRol(int id, String nombre) {
        for (Rol rol : cmbRol.getItems()) {
            if (rol.getIdRol() == id) {
                cmbRol.setValue(rol);
                return;
            }
        }

        cmbRol.setValue(new Rol(id, null, nombre, null));
    }

    @FXML
    private void nuevo() {
        modoEdicion = false;
        usuarioSeleccionado = null;

        tablaUsuarios.getSelectionModel().clearSelection();

        limpiarFormulario();
        txtPassword.setPromptText(PROMPT_PASSWORD_NUEVO);

        bloquearFormulario(false);

        btnEliminar.setDisable(true);

        txtUsuario.requestFocus();
    }

    @FXML
    private void guardar() {
        String usuario = txtUsuario.getText().trim();
        String password = txtPassword.getText();
        String nombre = txtNombre.getText().trim();
        String apellidoPaterno = txtApellidoPaterno.getText().trim();
        String apellidoMaterno = txtApellidoMaterno.getText().trim();
        String telefono = txtTelefono.getText().trim();
        String correo = txtCorreo.getText().trim();
        Rol rol = cmbRol.getValue();

        if (usuario.isEmpty()) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Campo obligatorio",
                    "Debes ingresar el nombre de usuario.");
            txtUsuario.requestFocus();
            return;
        }

        if (usuario.matches(".*\\s.*")) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Usuario no válido",
                    "El nombre de usuario no puede contener espacios.");
            txtUsuario.requestFocus();
            return;
        }

        if (usuario.length() > 100) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Usuario no válido",
                    "El nombre de usuario no puede tener más de 100 caracteres.");
            txtUsuario.requestFocus();
            return;
        }

        // Al editar, una contraseña vacía conserva la actual.
        if (!modoEdicion && password.isEmpty()) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Campo obligatorio",
                    "Debes ingresar la contraseña del usuario.");
            txtPassword.requestFocus();
            return;
        }

        if (!password.isEmpty() && password.length() < 6) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Contraseña no válida",
                    "La contraseña debe tener al menos 6 caracteres.");
            txtPassword.requestFocus();
            return;
        }

        if (nombre.isEmpty()) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Campo obligatorio",
                    "Debes ingresar el nombre del usuario.");
            txtNombre.requestFocus();
            return;
        }

        if (nombre.length() > 100) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Nombre no válido",
                    "El nombre no puede tener más de 100 caracteres.");
            txtNombre.requestFocus();
            return;
        }

        if (apellidoPaterno.isEmpty()) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Campo obligatorio",
                    "Debes ingresar el apellido paterno.");
            txtApellidoPaterno.requestFocus();
            return;
        }

        if (apellidoPaterno.length() > 100) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Apellido no válido",
                    "El apellido paterno no puede tener más de 100 caracteres.");
            txtApellidoPaterno.requestFocus();
            return;
        }

        if (apellidoMaterno.length() > 100) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Apellido no válido",
                    "El apellido materno no puede tener más de 100 caracteres.");
            txtApellidoMaterno.requestFocus();
            return;
        }

        if (!telefono.isEmpty() && !telefono.matches("\\d{10}")) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Teléfono no válido",
                    "El teléfono debe tener exactamente 10 dígitos.");
            txtTelefono.requestFocus();
            return;
        }

        if (!correo.isEmpty()
                && (correo.length() > 150
                    || !correo.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$"))) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Correo no válido",
                    "Escribe un correo electrónico válido.");
            txtCorreo.requestFocus();
            return;
        }

        if (rol == null) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Rol obligatorio",
                    "Selecciona el rol del usuario.");
            return;
        }

        int idExcluir = 0;

        if (modoEdicion && usuarioSeleccionado != null) {
            idExcluir = usuarioSeleccionado.getIdUsuario();
        }

        if (dao.existeUsuario(usuario, idExcluir)) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Usuario duplicado",
                    "Ya existe un usuario con ese nombre. Escribe un nombre diferente.");

            txtUsuario.requestFocus();
            return;
        }

        if (modoEdicion) {
            if (usuarioSeleccionado == null) {
                mostrarAlerta(
                        Alert.AlertType.WARNING,
                        "Sin selección",
                        "Selecciona un usuario para editar.");
                return;
            }

            usuarioSeleccionado.setUsuario(usuario);
            usuarioSeleccionado.setPasswordHash(
                    password.isEmpty() ? null : UsuarioDAO.hashPassword(password));
            usuarioSeleccionado.setNombre(nombre);
            usuarioSeleccionado.setApellidoPaterno(apellidoPaterno);
            usuarioSeleccionado.setApellidoMaterno(
                    apellidoMaterno.isEmpty() ? null : apellidoMaterno);
            usuarioSeleccionado.setTelefono(
                    telefono.isEmpty() ? null : telefono);
            usuarioSeleccionado.setCorreo(
                    correo.isEmpty() ? null : correo);
            usuarioSeleccionado.setIdRol(rol.getIdRol());

            if (dao.actualizar(usuarioSeleccionado)) {
                finalizarOperacion();

                mostrarAlerta(
                        Alert.AlertType.INFORMATION,
                        "Actualización exitosa",
                        "El usuario se actualizó correctamente.");
            } else {
                mostrarAlerta(
                        Alert.AlertType.ERROR,
                        "Error",
                        "No se pudo actualizar el usuario.");
            }

        } else {
            int id = dao.obtenerSiguienteId();

            if (id <= 0) {
                mostrarAlerta(
                        Alert.AlertType.ERROR,
                        "Error",
                        "No se pudo obtener el ID del usuario.");
                return;
            }

            Usuario nuevoUsuario = new Usuario(id, rol.getIdRol(), "Activo",
                    usuario, UsuarioDAO.hashPassword(password),
                    nombre, apellidoPaterno,
                    apellidoMaterno.isEmpty() ? null : apellidoMaterno,
                    telefono.isEmpty() ? null : telefono,
                    correo.isEmpty() ? null : correo);

            if (dao.insertar(nuevoUsuario)) {
                finalizarOperacion();

                mostrarAlerta(
                        Alert.AlertType.INFORMATION,
                        "Registro exitoso",
                        "El usuario se registró correctamente.");
            } else {
                mostrarAlerta(
                        Alert.AlertType.ERROR,
                        "Error",
                        "No se pudo registrar el usuario.");
            }
        }
    }

    @FXML
    private void eliminar() {
        if (usuarioSeleccionado == null) {
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);

        confirmacion.setTitle("Eliminar usuario");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText(
                "¿Deseas eliminar el usuario \""
                        + usuarioSeleccionado.getUsuario() + "\"?");

        Optional<ButtonType> respuesta = confirmacion.showAndWait();

        if (respuesta.isPresent()
                && respuesta.get() == ButtonType.OK) {

            if (dao.eliminar(usuarioSeleccionado.getIdUsuario())) {
                finalizarOperacion();

                mostrarAlerta(
                        Alert.AlertType.INFORMATION,
                        "Eliminación exitosa",
                        "El usuario se eliminó correctamente.");
            } else {
                mostrarAlerta(
                        Alert.AlertType.ERROR,
                        "No se pudo eliminar",
                        "El usuario podría estar asociado a ventas, compras o mermas.");
            }
        }
    }

    @FXML
    private void cancelar() {
        finalizarOperacion();
    }

    private void finalizarOperacion() {
        modoEdicion = false;
        usuarioSeleccionado = null;

        limpiarFormulario();
        bloquearFormulario(true);

        cargarUsuarios();

        tablaUsuarios.getSelectionModel().clearSelection();

        btnEliminar.setDisable(true);
    }

    private void limpiarFormulario() {
        txtUsuario.clear();
        txtPassword.clear();
        txtPassword.setPromptText(PROMPT_PASSWORD_NUEVO);
        txtNombre.clear();
        txtApellidoPaterno.clear();
        txtApellidoMaterno.clear();
        txtTelefono.clear();
        txtCorreo.clear();
        cmbRol.setValue(null);
    }

    private void bloquearFormulario(boolean bloquear) {
        txtUsuario.setDisable(bloquear);
        txtPassword.setDisable(bloquear);
        txtNombre.setDisable(bloquear);
        txtApellidoPaterno.setDisable(bloquear);
        txtApellidoMaterno.setDisable(bloquear);
        txtTelefono.setDisable(bloquear);
        txtCorreo.setDisable(bloquear);
        cmbRol.setDisable(bloquear);
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
                getClass().getResource("/com/laroca/vista/menu.fxml"));

        Parent vista = loader.load();

        Stage ventanaActual = (Stage) txtNombre.getScene().getWindow();

        ventanaActual.setScene(new Scene(vista));
        ventanaActual.setTitle("Sistema Abarrotes La Roca");
        ventanaActual.setResizable(false);
    }
}
