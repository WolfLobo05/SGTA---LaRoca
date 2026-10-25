
package com.laroca.controlador;

import com.laroca.dao.RolDAO;
import com.laroca.dao.UsuarioDAO;
import com.laroca.modelo.Estados;
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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class UsuarioController {

    private static final String PROMPT_PASSWORD_NUEVO = "Contraseña";
    private static final String PROMPT_PASSWORD_EDITAR =
            "Dejar vacío para no cambiarla";

    @FXML private TableView<Usuario> tablaUsuarios;
    @FXML private TableColumn<Usuario, String> columnaUsuario;
    @FXML private TableColumn<Usuario, String> columnaNombre;
    @FXML private TableColumn<Usuario, String> columnaApellidoPaterno;
    @FXML private TableColumn<Usuario, String> columnaApellidoMaterno;
    @FXML private TableColumn<Usuario, String> columnaTelefono;
    @FXML private TableColumn<Usuario, String> columnaCorreo;
    @FXML private TableColumn<Usuario, String> columnaRol;
    @FXML private TableColumn<Usuario, String> columnaEstado;

    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private TextField txtNombre;
    @FXML private TextField txtApellidoPaterno;
    @FXML private TextField txtApellidoMaterno;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtEstado;

    @FXML private ComboBox<Rol> cmbRol;
    @FXML private Button btnNuevo;
    @FXML private Button btnEliminar;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;
    @FXML private CheckBox chkMostrarInactivos;
    @FXML private Label lblFecha;
    @FXML private Label lblRegistros;

    private final UsuarioDAO dao = new UsuarioDAO();
    private final RolDAO rolDao = new RolDAO();

    private Usuario usuarioSeleccionado;
    private boolean modoEdicion = false;

    @FXML
    public void initialize() {

        lblFecha.setText(LocalDate.now().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        columnaUsuario.setCellValueFactory(
                new PropertyValueFactory<>("usuario"));
        columnaNombre.setCellValueFactory(
                new PropertyValueFactory<>("nombre"));
        columnaApellidoPaterno.setCellValueFactory(
                new PropertyValueFactory<>("apellidoPaterno"));
        columnaApellidoMaterno.setCellValueFactory(
                new PropertyValueFactory<>("apellidoMaterno"));
        columnaTelefono.setCellValueFactory(
                new PropertyValueFactory<>("telefono"));
        columnaCorreo.setCellValueFactory(
                new PropertyValueFactory<>("correo"));
        columnaRol.setCellValueFactory(
                new PropertyValueFactory<>("nombreRol"));
        columnaEstado.setCellValueFactory(
                new PropertyValueFactory<>("estado"));

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

        configurarValidacionesEntrada();
        configurarListeners();

        cargarRoles();
        cargarUsuarios();

        tablaUsuarios.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, anterior, actual) -> {
                    if (actual != null) {
                        usuarioSeleccionado = actual;

                        mostrarUsuario(actual);
                        txtPassword.setPromptText(
                                PROMPT_PASSWORD_EDITAR);

                        if (chkMostrarInactivos.isSelected()) {
                            // Los usuarios inactivos solo se consultan.
                            modoEdicion = false;
                            bloquearFormulario(true);
                            btnCancelar.setDisable(false);
                        } else {
                            modoEdicion = true;
                            bloquearFormulario(false);
                        }

                        btnEliminar.setDisable(false);
                        actualizarEstadoGuardar();
                    } else {
                        btnEliminar.setDisable(true);
                    }
                });

        btnEliminar.setDisable(true);
        bloquearFormulario(true);
    }

    // Impide caracteres inválidos desde el momento de escribir.
    private void configurarValidacionesEntrada() {

        // Nombres y apellidos: letras Unicode y espacios.
        TextFormatter<String> filtroNombre =
                new TextFormatter<>(cambio -> {
                    String texto = cambio.getControlNewText();

                    return texto.matches("[\\p{L} ]*")
                            ? cambio : null;
                });

        TextFormatter<String> filtroPaterno =
                new TextFormatter<>(cambio -> {
                    String texto = cambio.getControlNewText();

                    return texto.matches("[\\p{L} ]*")
                            ? cambio : null;
                });

        TextFormatter<String> filtroMaterno =
                new TextFormatter<>(cambio -> {
                    String texto = cambio.getControlNewText();

                    return texto.matches("[\\p{L} ]*")
                            ? cambio : null;
                });

        txtNombre.setTextFormatter(filtroNombre);
        txtApellidoPaterno.setTextFormatter(filtroPaterno);
        txtApellidoMaterno.setTextFormatter(filtroMaterno);

        // Teléfono: solo números, máximo diez dígitos.
        txtTelefono.setTextFormatter(
                new TextFormatter<String>(cambio -> {
                    String texto = cambio.getControlNewText();

                    return texto.matches("\\d{0,10}")
                            ? cambio : null;
                })
        );

        // Usuario: no permitir espacios ni más de 100 caracteres.
        txtUsuario.setTextFormatter(
                new TextFormatter<String>(cambio -> {
                    String texto = cambio.getControlNewText();

                    return texto.length() <= 100
                            && !texto.matches(".*\\s.*")
                            ? cambio : null;
                })
        );
    }

    private void configurarListeners() {
        txtUsuario.textProperty().addListener(
                (obs, anterior, actual) -> actualizarEstadoGuardar());

        txtPassword.textProperty().addListener(
                (obs, anterior, actual) -> actualizarEstadoGuardar());

        txtNombre.textProperty().addListener(
                (obs, anterior, actual) -> actualizarEstadoGuardar());

        txtApellidoPaterno.textProperty().addListener(
                (obs, anterior, actual) -> actualizarEstadoGuardar());

        txtApellidoMaterno.textProperty().addListener(
                (obs, anterior, actual) -> actualizarEstadoGuardar());

        txtTelefono.textProperty().addListener(
                (obs, anterior, actual) -> actualizarEstadoGuardar());

        txtCorreo.textProperty().addListener(
                (obs, anterior, actual) -> actualizarEstadoGuardar());

        cmbRol.valueProperty().addListener(
                (obs, anterior, actual) -> actualizarEstadoGuardar());
    }

    // Muestra solo los activos o solo los inactivos, según la casilla.
    private void cargarUsuarios() {
        boolean inactivos = chkMostrarInactivos.isSelected();

        ObservableList<Usuario> lista =
                FXCollections.observableArrayList();

        for (Usuario usuario : dao.listar()) {
            if (Estados.esActivo(usuario.getEstado()) != inactivos) {
                lista.add(usuario);
            }
        }

        tablaUsuarios.setItems(lista);

        lblRegistros.setText(
                lista.size()
                + (lista.size() == 1
                    ? (inactivos ? " usuario inactivo" : " usuario activo")
                    : (inactivos ? " usuarios inactivos" : " usuarios activos")));
    }

    private void cargarRoles() {
        ObservableList<Rol> lista =
                FXCollections.observableArrayList();

        // Solo se ofrecen los roles activos.
        for (Rol rol : rolDao.listar()) {
            if (Estados.esActivo(rol.getEstado())) {
                lista.add(rol);
            }
        }

        cmbRol.setItems(lista);
    }

    private void mostrarUsuario(Usuario usuario) {
        txtUsuario.setText(
                Objects.toString(usuario.getUsuario(), ""));
        txtPassword.clear();

        txtNombre.setText(
                Objects.toString(usuario.getNombre(), ""));
        txtApellidoPaterno.setText(
                Objects.toString(usuario.getApellidoPaterno(), ""));
        txtApellidoMaterno.setText(
                Objects.toString(usuario.getApellidoMaterno(), ""));
        txtTelefono.setText(
                Objects.toString(usuario.getTelefono(), ""));
        txtCorreo.setText(
                Objects.toString(usuario.getCorreo(), ""));
        txtEstado.setText(Estados.mostrar(usuario.getEstado()));

        seleccionarRol(usuario.getIdRol(), usuario.getNombreRol());
    }

    private void seleccionarRol(int id, String nombre) {
        for (Rol rol : cmbRol.getItems()) {
            if (rol.getIdRol() == id) {
                cmbRol.setValue(rol);
                return;
            }
        }

        // Si el rol está inactivo no aparece en la lista, así que se
        // muestra con un objeto temporal que no se agrega a las opciones.
        // Así el usuario se puede guardar sin cambiar su rol, y si se
        // cambia por otro, el rol inactivo ya no se puede volver a elegir.
        cmbRol.setValue(new Rol(id, null, nombre, null));
    }

    @FXML
    private void nuevo() {
        if (chkMostrarInactivos.isSelected()) {
            return;
        }

        modoEdicion = false;
        usuarioSeleccionado = null;

        tablaUsuarios.getSelectionModel().clearSelection();

        limpiarFormulario();
        txtEstado.setText(Estados.ACTIVO);
        txtPassword.setPromptText(PROMPT_PASSWORD_NUEVO);

        bloquearFormulario(false);
        btnEliminar.setDisable(true);

        actualizarEstadoGuardar();
        txtUsuario.requestFocus();
    }

    private boolean formularioValido() {
        String usuario = txtUsuario.getText().trim();
        String password = txtPassword.getText();
        String nombre = txtNombre.getText().trim();
        String paterno = txtApellidoPaterno.getText().trim();
        String materno = txtApellidoMaterno.getText().trim();
        String telefono = txtTelefono.getText().trim();
        String correo = txtCorreo.getText().trim();

        boolean usuarioValido =
                !usuario.isEmpty() && usuario.length() <= 100;

        boolean passwordValida =
                (modoEdicion && password.isEmpty())
                || password.length() >= 6;

        boolean nombreValido =
                !nombre.isEmpty()
                && nombre.length() <= 100
                && nombre.matches("[\\p{L} ]+");

        boolean paternoValido =
                !paterno.isEmpty()
                && paterno.length() <= 100
                && paterno.matches("[\\p{L} ]+");

        boolean maternoValido =
                materno.isEmpty()
                || (materno.length() <= 100
                    && materno.matches("[\\p{L} ]+"));

        // Teléfono y correo son obligatorios.
        boolean telefonoValido = telefono.matches("\\d{10}");

        boolean correoValido =
                correo.length() <= 150
                && correo.matches(
                    "^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

        Rol rol = cmbRol.getValue();

        boolean rolValido =
                rol != null && rol.getIdRol() > 0;

        return usuarioValido
                && passwordValida
                && nombreValido
                && paternoValido
                && maternoValido
                && telefonoValido
                && correoValido
                && rolValido;
    }

    private void actualizarEstadoGuardar() {
        boolean formularioActivo = !txtUsuario.isDisabled();

        btnGuardar.setDisable(
                !formularioActivo || !formularioValido());
    }

    @FXML
    private void guardar() {
        String usuario = txtUsuario.getText().trim();
        String password = txtPassword.getText();
        String nombre = txtNombre.getText().trim();
        String paterno = txtApellidoPaterno.getText().trim();
        String materno = txtApellidoMaterno.getText().trim();
        String telefono = txtTelefono.getText().trim();
        String correo = txtCorreo.getText().trim();
        Rol rol = cmbRol.getValue();

        // Revalidar al guardar, aunque se invoque desde otro lugar.
        if (!formularioValido()) {
            mostrarAlerta(Alert.AlertType.WARNING,
                    "Datos incompletos o no válidos",
                    "Revisa los campos obligatorios y los formatos.");
            return;
        }

        if (rol == null) {
            mostrarAlerta(Alert.AlertType.WARNING,
                    "Rol obligatorio",
                    "Selecciona un rol activo.");
            return;
        }

        if (modoEdicion && usuarioSeleccionado == null) {
            mostrarAlerta(Alert.AlertType.WARNING,
                    "Sin selección",
                    "Selecciona un usuario para editar.");
            return;
        }

        int idExcluir = modoEdicion
                ? usuarioSeleccionado.getIdUsuario()
                : 0;

        // Comprobar duplicados antes de insertar o actualizar.
        // Se compara contra todos los usuarios, activos e inactivos.
        if (dao.existeUsuario(usuario, idExcluir)) {
            if (dao.existeUsuarioInactivo(usuario, idExcluir)) {
                mostrarAlerta(Alert.AlertType.WARNING,
                        "Usuario inactivo",
                        "Ya existe un usuario con ese nombre, pero está inactivo. "
                        + "Puedes reactivarlo desde \"Mostrar inactivos\".");
            } else {
                mostrarAlerta(Alert.AlertType.WARNING,
                        "Usuario duplicado",
                        "Ya existe un usuario con ese nombre. "
                        + "Escribe uno diferente.");
            }
            txtUsuario.requestFocus();
            return;
        }

        if (modoEdicion) {
            usuarioSeleccionado.setUsuario(usuario);

            usuarioSeleccionado.setPasswordHash(
                    password.isEmpty()
                    ? null
                    : UsuarioDAO.hashPassword(password));

            usuarioSeleccionado.setNombre(nombre);
            usuarioSeleccionado.setApellidoPaterno(paterno);
            usuarioSeleccionado.setApellidoMaterno(
                    materno.isEmpty() ? null : materno);
            usuarioSeleccionado.setTelefono(telefono);
            usuarioSeleccionado.setCorreo(correo);
            usuarioSeleccionado.setIdRol(rol.getIdRol());

            if (dao.actualizar(usuarioSeleccionado)) {
                finalizarOperacion();
                mostrarAlerta(Alert.AlertType.INFORMATION,
                        "Actualización exitosa",
                        "El usuario se actualizó correctamente.");
            } else {
                mostrarAlerta(Alert.AlertType.ERROR,
                        "Error",
                        "No se pudo actualizar el usuario.");
            }

        } else {
            int id = dao.obtenerSiguienteId();

            if (id <= 0) {
                mostrarAlerta(Alert.AlertType.ERROR,
                        "Error",
                        "No se pudo obtener el ID del usuario.");
                return;
            }

            Usuario nuevoUsuario = new Usuario(
                    id,
                    rol.getIdRol(),
                    Estados.ACTIVO,
                    usuario,
                    UsuarioDAO.hashPassword(password),
                    nombre,
                    paterno,
                    materno.isEmpty() ? null : materno,
                    telefono,
                    correo
            );

            if (dao.insertar(nuevoUsuario)) {
                finalizarOperacion();
                mostrarAlerta(Alert.AlertType.INFORMATION,
                        "Registro exitoso",
                        "El usuario se registró correctamente.");
            } else {
                mostrarAlerta(Alert.AlertType.ERROR,
                        "Error",
                        "No se pudo registrar el usuario.");
            }
        }
    }

    // Elimina (desactiva) o reactiva el usuario, según la casilla.
    @FXML
    private void eliminar() {
        if (usuarioSeleccionado == null) {
            return;
        }

        boolean reactivar = chkMostrarInactivos.isSelected();

        Alert confirmacion =
                new Alert(Alert.AlertType.CONFIRMATION);

        confirmacion.setTitle(reactivar ? "Reactivar usuario" : "Eliminar usuario");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText(reactivar
                ? "¿Deseas reactivar el usuario \""
                  + usuarioSeleccionado.getUsuario() + "\"?\n\n"
                  + "Volverá a estar activo y podrá iniciar sesión."
                : "¿Deseas eliminar el usuario \""
                  + usuarioSeleccionado.getUsuario() + "\"?\n\n"
                  + "Quedará inactivo y ya no podrá iniciar sesión.");

        Optional<ButtonType> respuesta = confirmacion.showAndWait();

        if (respuesta.isPresent()
                && respuesta.get() == ButtonType.OK) {

            boolean exito = reactivar
                    ? dao.reactivar(usuarioSeleccionado.getIdUsuario())
                    : dao.desactivar(usuarioSeleccionado.getIdUsuario());

            if (exito) {
                finalizarOperacion();
                mostrarAlerta(Alert.AlertType.INFORMATION,
                        reactivar ? "Reactivación exitosa" : "Eliminación exitosa",
                        reactivar
                        ? "El usuario se reactivó correctamente."
                        : "El usuario se marcó como inactivo.");
            } else {
                mostrarAlerta(Alert.AlertType.ERROR,
                        "No se pudo cambiar el estado",
                        "No se pudo actualizar el estado del usuario.");
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
        txtEstado.clear();
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

        btnCancelar.setDisable(bloquear);
        btnGuardar.setDisable(bloquear);

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
                getClass().getResource(
                    "/com/laroca/vista/menu.fxml"));

        Parent vista = loader.load();

        Stage ventanaActual =
                (Stage) txtNombre.getScene().getWindow();

        Navegacion.mostrar(ventanaActual, vista, Navegacion.TITULO_MENU);
    }
}
