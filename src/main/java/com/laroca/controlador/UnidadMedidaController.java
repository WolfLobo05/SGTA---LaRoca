package com.laroca.controlador;

import com.laroca.dao.UnidadMedidaDAO;
import com.laroca.modelo.UnidadMedida;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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
    private TextField txtNombre;

    @FXML
    private TextField txtAbreviatura;

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

    // Indica si estamos creando o editando
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

        cargarUnidades();

        tablaUnidades.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, anterior, actual) -> {

                    if (actual != null) {

                        unidadSeleccionada = actual;

                        txtNombre.setText(
                                actual.getNombre()
                        );

                        txtAbreviatura.setText(
                                actual.getAbreviatura()
                        );

                        btnEditar.setDisable(false);
                        btnEliminar.setDisable(false);
                    }
                });

        btnEditar.setDisable(true);
        btnEliminar.setDisable(true);

        // Al iniciar, el formulario está en modo nuevo
        bloquearFormulario(true);
    }


    /**
     * Carga las unidades desde MySQL.
     */
    private void cargarUnidades() {

        ObservableList<UnidadMedida> lista =
                FXCollections.observableArrayList(
                        dao.listar()
                );

        tablaUnidades.setItems(lista);

        lblRegistros.setText(
                lista.size() + " unidades registradas"
        );
    }


    /**
     * Activa el modo NUEVO.
     */
    @FXML
    private void nuevo() {

        modoEdicion = false;

        unidadSeleccionada = null;

        tablaUnidades.getSelectionModel()
                .clearSelection();

        txtNombre.clear();
        txtAbreviatura.clear();

        bloquearFormulario(false);

        btnEditar.setDisable(true);
        btnEliminar.setDisable(true);

        txtNombre.requestFocus();
    }


    /**
     * Activa el modo EDICIÓN.
     */
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

        bloquearFormulario(false);

        txtNombre.requestFocus();
    }


    /**
     * Guarda una unidad nueva o actualiza una existente.
     */
    @FXML
    private void guardar() {

        String nombre = txtNombre.getText().trim();
        String abreviatura = txtAbreviatura.getText().trim();

        if (nombre.isEmpty() || abreviatura.isEmpty()) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Datos incompletos",
                    "Debes ingresar el nombre y la abreviatura."
            );

            return;
        }


        // ==========================================
        // MODO EDICIÓN
        // ==========================================

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

            if (dao.actualizar(unidadSeleccionada)) {

                cargarUnidades();

                modoEdicion = false;

                bloquearFormulario(true);

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

            return;
        }


        // ==========================================
        // MODO NUEVO
        // ==========================================

        int id = dao.obtenerSiguienteId();

        UnidadMedida unidad = new UnidadMedida();

        unidad.setIdUnidadMedida(id);
        unidad.setEstado("Activo");
        unidad.setNombre(nombre);
        unidad.setAbreviatura(abreviatura);

        if (dao.insertar(unidad)) {

            cargarUnidades();

            txtNombre.clear();
            txtAbreviatura.clear();

            bloquearFormulario(true);

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


    /**
     * Elimina la unidad seleccionada.
     */
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

        Alert confirmacion =
                new Alert(Alert.AlertType.CONFIRMATION);

        confirmacion.setTitle("Eliminar unidad");
        confirmacion.setHeaderText(null);

        confirmacion.setContentText(
                "¿Deseas eliminar la unidad \""
                + unidadSeleccionada.getNombre()
                + "\"?"
        );

        ButtonType respuesta =
                confirmacion.showAndWait()
                        .orElse(ButtonType.CANCEL);

        if (respuesta == ButtonType.OK) {

            if (dao.eliminar(
                    unidadSeleccionada.getIdUnidadMedida()
            )) {

                cargarUnidades();

                unidadSeleccionada = null;

                txtNombre.clear();
                txtAbreviatura.clear();

                btnEditar.setDisable(true);
                btnEliminar.setDisable(true);

                bloquearFormulario(true);

                mostrarAlerta(
                        Alert.AlertType.INFORMATION,
                        "Eliminación exitosa",
                        "La unidad se eliminó correctamente."
                );

            } else {

                mostrarAlerta(
                        Alert.AlertType.ERROR,
                        "Error",
                        "No se pudo eliminar la unidad."
                );
            }
        }
    }


    /**
     * Cancela la operación actual.
     */
    @FXML
    private void cancelar() {

        modoEdicion = false;

        unidadSeleccionada = null;

        tablaUnidades.getSelectionModel()
                .clearSelection();

        txtNombre.clear();
        txtAbreviatura.clear();

        btnEditar.setDisable(true);
        btnEliminar.setDisable(true);

        bloquearFormulario(true);
    }


    /**
     * Bloquea o desbloquea los campos.
     */
    private void bloquearFormulario(boolean bloquear) {

        txtNombre.setDisable(bloquear);
        txtAbreviatura.setDisable(bloquear);
        btnGuardar.setDisable(bloquear);
        btnCancelar.setDisable(bloquear);
    }


    /**
     * Muestra mensajes.
     */
    private void mostrarAlerta(
            Alert.AlertType tipo,
            String titulo,
            String mensaje
    ) {

        Alert alerta = new Alert(tipo);

        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);

        alerta.showAndWait();
    }
}