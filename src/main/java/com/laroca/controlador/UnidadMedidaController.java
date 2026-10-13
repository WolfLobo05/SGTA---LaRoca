
package com.laroca.controlador;

import com.laroca.dao.UnidadMedidaDAO;
import com.laroca.modelo.UnidadMedida;

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

public class UnidadMedidaController {

    @FXML
    private TableView<UnidadMedida> tablaUnidades;

    @FXML
    private TableColumn<UnidadMedida, Integer> columnaId;

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
    private ComboBox<String> cmbEstado;

    @FXML
    private Button btnNuevo;

    @FXML
    private Button btnEditar;

    @FXML
    private Button btnEliminar;

    @FXML
    private Button btnGuardar;

    @FXML
    private Button btnCancelar;

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
            LocalDate.now().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy")
            )
        );

        columnaId.setCellValueFactory(
            new PropertyValueFactory<>("idUnidadMedida")
        );

        columnaNombre.setCellValueFactory(
            new PropertyValueFactory<>("nombre")
        );

        columnaAbreviatura.setCellValueFactory(
            new PropertyValueFactory<>("abreviatura")
        );

        columnaEstado.setCellValueFactory(
            new PropertyValueFactory<>("estado")
        );

        cmbEstado.setItems(
            FXCollections.observableArrayList("Activo", "Inactivo")
        );

        cargarUnidades();

        tablaUnidades.getSelectionModel()
            .selectedItemProperty()
            .addListener((observable, anterior, actual) -> {

                if (actual != null) {
                    unidadSeleccionada = actual;

                    txtNombre.setText(actual.getNombre());
                    txtAbreviatura.setText(actual.getAbreviatura());
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

    private void cargarUnidades() {
        ObservableList<UnidadMedida> lista =
            FXCollections.observableArrayList(dao.listar());

        tablaUnidades.setItems(lista);

        int total = lista.size();

        lblRegistros.setText(
            total + (total == 1
                ? " unidad registrada"
                : " unidades registradas")
        );
    }

    @FXML
    private void nuevo() {
        modoEdicion = false;
        unidadSeleccionada = null;

        tablaUnidades.getSelectionModel().clearSelection();

        limpiarFormulario();
        cmbEstado.setValue("Activo");

        bloquearFormulario(false);

        btnEditar.setDisable(true);
        btnEliminar.setDisable(true);

        txtNombre.requestFocus();
    }

    @FXML
    private void editar() {
        if (unidadSeleccionada == null) {
            mostrarAlerta(
                Alert.AlertType.WARNING,
                "Sin selección",
                "Selecciona primero una unidad."
            );
            return;
        }

        modoEdicion = true;

        txtNombre.setText(unidadSeleccionada.getNombre());
        txtAbreviatura.setText(unidadSeleccionada.getAbreviatura());
        cmbEstado.setValue(unidadSeleccionada.getEstado());

        bloquearFormulario(false);
        txtNombre.requestFocus();
    }

    @FXML
    private void guardar() {
        String nombre = txtNombre.getText().trim();
        String abreviatura = txtAbreviatura.getText().trim();
        String estado = cmbEstado.getValue();

        if (nombre.isEmpty() || abreviatura.isEmpty()) {
            mostrarAlerta(
                Alert.AlertType.WARNING,
                "Datos incompletos",
                "Debes ingresar el nombre y la abreviatura."
            );
            return;
        }

        if (estado == null || estado.isBlank()) {
            mostrarAlerta(
                Alert.AlertType.WARNING,
                "Estado obligatorio",
                "Selecciona el estado de la unidad."
            );
            return;
        }

        if (modoEdicion) {
            if (unidadSeleccionada == null) {
                mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Sin selección",
                    "Selecciona una unidad para editar."
                );
                return;
            }

            unidadSeleccionada.setNombre(nombre);
            unidadSeleccionada.setAbreviatura(abreviatura);
            unidadSeleccionada.setEstado(estado);

            if (dao.actualizar(unidadSeleccionada)) {
                finalizarOperacion();

                mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Actualización exitosa",
                    "La unidad se actualizó correctamente."
                );
            } else {
                mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error",
                    "No se pudo actualizar la unidad."
                );
            }

        } else {
            int id = dao.obtenerSiguienteId();

            if (id <= 0) {
                mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error",
                    "No se pudo obtener el ID de la unidad."
                );
                return;
            }

            UnidadMedida unidad = new UnidadMedida();

            unidad.setIdUnidadMedida(id);
            unidad.setEstado(estado);
            unidad.setNombre(nombre);
            unidad.setAbreviatura(abreviatura);

            if (dao.insertar(unidad)) {
                finalizarOperacion();

                mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Registro exitoso",
                    "La unidad se registró correctamente."
                );
            } else {
                mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error",
                    "No se pudo registrar la unidad."
                );
            }
        }
    }

    @FXML
    private void eliminar() {
        if (unidadSeleccionada == null) {
            mostrarAlerta(
                Alert.AlertType.WARNING,
                "Sin selección",
                "Selecciona primero una unidad."
            );
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);

        confirmacion.setTitle("Eliminar unidad");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText(
            "¿Deseas eliminar la unidad \""
                + unidadSeleccionada.getNombre() + "\"?"
        );

        Optional<ButtonType> respuesta = confirmacion.showAndWait();

        if (respuesta.isPresent()
                && respuesta.get() == ButtonType.OK) {

            if (dao.eliminar(unidadSeleccionada.getIdUnidadMedida())) {
                finalizarOperacion();

                mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Eliminación exitosa",
                    "La unidad se eliminó correctamente."
                );
            } else {
                mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error",
                    "No se pudo eliminar la unidad. "
                        + "Puede estar relacionada con otros registros."
                );
            }
        }
    }

    @FXML
    private void cancelar() {
        modoEdicion = false;
        unidadSeleccionada = null;

        tablaUnidades.getSelectionModel().clearSelection();

        limpiarFormulario();
        bloquearFormulario(true);

        btnEditar.setDisable(true);
        btnEliminar.setDisable(true);
    }

    private void finalizarOperacion() {
        modoEdicion = false;
        unidadSeleccionada = null;

        limpiarFormulario();
        bloquearFormulario(true);

        cargarUnidades();

        tablaUnidades.getSelectionModel().clearSelection();

        btnEditar.setDisable(true);
        btnEliminar.setDisable(true);
    }

    private void limpiarFormulario() {
        txtNombre.clear();
        txtAbreviatura.clear();
        cmbEstado.getSelectionModel().clearSelection();
    }

    private void bloquearFormulario(boolean bloquear) {
        txtNombre.setDisable(bloquear);
        txtAbreviatura.setDisable(bloquear);
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
