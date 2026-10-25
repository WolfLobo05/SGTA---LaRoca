package com.laroca.controlador;

import com.laroca.dao.UnidadMedidaDAO;
import com.laroca.modelo.Estados;
import com.laroca.modelo.UnidadMedida;

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

public class UnidadMedidaController {

    @FXML
    private TableView<UnidadMedida> tablaUnidades;

    @FXML
    private TableColumn<UnidadMedida, String> columnaNombre;

    @FXML
    private TableColumn<UnidadMedida, String> columnaAbreviatura;

    @FXML
    private TableColumn<UnidadMedida, String> columnaEstado;

    @FXML
    private TextField txtNombre;

    @FXML
    private TextField txtAbreviatura;

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
    private Label lblRegistros;

    @FXML
    private Label lblFecha;

    private final UnidadMedidaDAO dao = new UnidadMedidaDAO();

    private UnidadMedida unidadSeleccionada;
    private boolean modoEdicion = false;

    @FXML
    public void initialize() {
        lblFecha.setText(
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        columnaNombre.setCellValueFactory(
                new PropertyValueFactory<>("nombre"));
        columnaAbreviatura.setCellValueFactory(
                new PropertyValueFactory<>("abreviatura"));
        columnaEstado.setCellValueFactory(
                new PropertyValueFactory<>("estado"));

        // Validar automáticamente mientras se capturan los datos.
        txtNombre.textProperty().addListener(
                (observable, anterior, actual) -> {
                    if (!actual.matches("[\\p{L} ]*")) {
                        txtNombre.setText(actual.replaceAll("[^\\p{L} ]", ""));
                    }

                    actualizarEstadoGuardar();
                });

        txtAbreviatura.textProperty().addListener(
                (observable, anterior, actual) -> actualizarEstadoGuardar());

        cargarUnidades();

        tablaUnidades.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, anterior, actual) -> {
                    if (actual != null) {
                        unidadSeleccionada = actual;
                        txtNombre.setText(Objects.toString(actual.getNombre(), ""));
                        txtAbreviatura.setText(Objects.toString(actual.getAbreviatura(), ""));
                        txtEstado.setText(Estados.mostrar(actual.getEstado()));

                        if (chkMostrarInactivos.isSelected()) {
                            // Las unidades inactivas solo se consultan.
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
        String abreviatura = txtAbreviatura.getText().trim();

        boolean nombreValido = nombre.matches("[\\p{L} ]+");

        boolean abreviaturaValida = abreviatura.matches("[\\p{L}\\p{N}./²³-]+");

        boolean formularioValido = nombreValido
                && abreviaturaValida;

        btnGuardar.setDisable(!formularioValido);
    }

    // Muestra solo las activas o solo las inactivas, según la casilla.
    private void cargarUnidades() {
        boolean inactivos = chkMostrarInactivos.isSelected();

        ObservableList<UnidadMedida> lista = FXCollections.observableArrayList();

        for (UnidadMedida unidad : dao.listar()) {
            if (Estados.esActivo(unidad.getEstado()) != inactivos) {
                lista.add(unidad);
            }
        }

        tablaUnidades.setItems(lista);

        int total = lista.size();
        lblRegistros.setText(
                total + (total == 1
                        ? (inactivos ? " unidad inactiva" : " unidad activa")
                        : (inactivos ? " unidades inactivas" : " unidades activas")));
    }

    @FXML
    private void nuevo() {
        if (chkMostrarInactivos.isSelected()) {
            return;
        }

        modoEdicion = false;
        unidadSeleccionada = null;

        tablaUnidades.getSelectionModel().clearSelection();
        limpiarFormulario();
        txtEstado.setText(Estados.ACTIVO);

        bloquearFormulario(false);
        btnEliminar.setDisable(true);
        txtNombre.requestFocus();
    }

    @FXML
    private void guardar() {
        String nombre = txtNombre.getText().trim();
        String abreviatura = txtAbreviatura.getText().trim();

        if (nombre.isEmpty() || abreviatura.isEmpty()) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Datos incompletos",
                    "Debes ingresar el nombre y la abreviatura.");
            return;
        }

        // Evita nombres repetidos al insertar y al editar.
        // Se compara contra todas las unidades, activas e inactivas.
        int idExcluir = modoEdicion && unidadSeleccionada != null
                ? unidadSeleccionada.getIdUnidadMedida()
                : 0;

        if (dao.existeNombre(nombre, idExcluir)) {
            if (dao.existeNombreInactivo(nombre, idExcluir)) {
                mostrarAlerta(
                        Alert.AlertType.WARNING,
                        "Unidad inactiva",
                        "Ya existe una unidad de medida con ese nombre, pero está inactiva. "
                                + "Puedes reactivarla desde \"Mostrar inactivos\".");
            } else {
                mostrarAlerta(
                        Alert.AlertType.WARNING,
                        "Nombre duplicado",
                        "Ya existe una unidad de medida con ese nombre. "
                                + "Escribe un nombre diferente.");
            }
            txtNombre.requestFocus();
            txtNombre.selectAll();
            return;
        }

        if (modoEdicion) {
            if (unidadSeleccionada == null) {
                mostrarAlerta(
                        Alert.AlertType.WARNING,
                        "Sin selección",
                        "Selecciona una unidad para editar.");
                return;
            }

            // El estado no se modifica al editar.
            unidadSeleccionada.setNombre(nombre);
            unidadSeleccionada.setAbreviatura(abreviatura);

            if (dao.actualizar(unidadSeleccionada)) {
                finalizarOperacion();

                mostrarAlerta(
                        Alert.AlertType.INFORMATION,
                        "Actualización exitosa",
                        "La unidad se actualizó correctamente.");
            } else {
                mostrarAlerta(
                        Alert.AlertType.ERROR,
                        "Error",
                        "No se pudo actualizar la unidad. "
                                + "Verifica los datos y la conexión.");
            }
        } else {
            int id = dao.obtenerSiguienteId();

            if (id <= 0) {
                mostrarAlerta(
                        Alert.AlertType.ERROR,
                        "Error",
                        "No se pudo obtener el ID de la unidad.");
                return;
            }

            // Toda unidad nueva se registra como Activo.
            UnidadMedida unidad = new UnidadMedida();
            unidad.setIdUnidadMedida(id);
            unidad.setEstado(Estados.ACTIVO);
            unidad.setNombre(nombre);
            unidad.setAbreviatura(abreviatura);

            if (dao.insertar(unidad)) {
                finalizarOperacion();

                mostrarAlerta(
                        Alert.AlertType.INFORMATION,
                        "Registro exitoso",
                        "La unidad se registró correctamente.");
            } else {
                mostrarAlerta(
                        Alert.AlertType.ERROR,
                        "Error",
                        "No se pudo registrar la unidad. "
                                + "Verifica los datos y la conexión.");
            }
        }
    }

    // Elimina (desactiva) o reactiva la unidad, según la casilla.
    @FXML
    private void eliminar() {
        if (unidadSeleccionada == null) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Sin selección",
                    "Selecciona primero una unidad.");
            return;
        }

        boolean reactivar = chkMostrarInactivos.isSelected();

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle(reactivar ? "Reactivar unidad" : "Eliminar unidad");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText(reactivar
                ? "¿Deseas reactivar la unidad \""
                        + unidadSeleccionada.getNombre() + "\"?\n\n"
                        + "Volverá a estar activa y disponible para los productos."
                : "¿Deseas eliminar la unidad \""
                        + unidadSeleccionada.getNombre() + "\"?\n\n"
                        + "Quedará inactiva y ya no se podrá elegir en nuevos productos. "
                        + "Los productos que ya la usan no cambian.");

        Optional<ButtonType> respuesta = confirmacion.showAndWait();

        if (respuesta.isPresent() && respuesta.get() == ButtonType.OK) {
            boolean exito = reactivar
                    ? dao.reactivar(unidadSeleccionada.getIdUnidadMedida())
                    : dao.desactivar(unidadSeleccionada.getIdUnidadMedida());

            if (exito) {
                finalizarOperacion();

                mostrarAlerta(
                        Alert.AlertType.INFORMATION,
                        reactivar ? "Reactivación exitosa" : "Eliminación exitosa",
                        reactivar
                                ? "La unidad se reactivó correctamente."
                                : "La unidad se marcó como inactiva.");
            } else {
                mostrarAlerta(
                        Alert.AlertType.ERROR,
                        "Error",
                        "No se pudo actualizar el estado de la unidad. "
                                + "Verifica la conexión.");
            }
        }
    }

    // Cambia entre la lista de activas y la de inactivas.
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
        unidadSeleccionada = null;

        limpiarFormulario();
        bloquearFormulario(true);
        cargarUnidades();

        tablaUnidades.getSelectionModel().clearSelection();
        btnEliminar.setDisable(true);
    }

    private void limpiarFormulario() {
        txtNombre.clear();
        txtAbreviatura.clear();
        txtEstado.clear();
    }

    private void bloquearFormulario(boolean bloquear) {
        txtNombre.setDisable(bloquear);
        txtAbreviatura.setDisable(bloquear);

        // Guardar solo se habilita si los datos son válidos.
        btnGuardar.setDisable(true);

        // Cancelar queda disponible durante la captura.
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
