package com.laroca.controlador;

import com.laroca.dao.CategoriaDAO;
import com.laroca.modelo.Categoria;

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

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public class CategoriaController {

    @FXML
    private TableView<Categoria> tablaCategorias;
    @FXML
    private TableColumn<Categoria, String> columnaNombre;
    @FXML
    private TableColumn<Categoria, String> columnaDescripcion;
    @FXML
    private TableColumn<Categoria, String> columnaEstado;

    @FXML
    private TextField txtNombre;
    @FXML
    private TextField txtDescripcion;
    @FXML
    private ComboBox<String> cmbEstado;

    @FXML
    private Button btnEliminar;
    @FXML
    private Button btnGuardar;
    @FXML
    private Button btnCancelar;

    @FXML
    private Label lblFecha;
    @FXML
    private Label lblTotalCategorias;

    private final CategoriaDAO categoriaDAO = new CategoriaDAO();
    private final ObservableList<Categoria> listaCategorias = FXCollections.observableArrayList();

    private boolean modoEdicion = false;

    @FXML
    public void initialize() {
        columnaNombre.setCellValueFactory(
                new PropertyValueFactory<>("nombre"));
        columnaDescripcion.setCellValueFactory(
                new PropertyValueFactory<>("descripcion"));
        columnaEstado.setCellValueFactory(
                new PropertyValueFactory<>("estado"));

        cmbEstado.setItems(FXCollections.observableArrayList(
                "Activo", "Inactivo"));

        // Validar el nombre mientras se escribe.
        txtNombre.textProperty().addListener(
                (observable, anterior, actual) -> {
                    String nombreValido = actual.replaceAll("[^\\p{L} ]", "");

                    if (!actual.equals(nombreValido)) {
                        txtNombre.setText(nombreValido);
                        return;
                    }

                    actualizarEstadoGuardar();
                });

        // Actualizar Guardar cuando cambie la descripción.
        txtDescripcion.textProperty().addListener(
                (observable, anterior, actual) -> actualizarEstadoGuardar());

        // Actualizar Guardar cuando cambie el estado.
        cmbEstado.valueProperty().addListener(
                (observable, anterior, actual) -> actualizarEstadoGuardar());

        tablaCategorias.setItems(listaCategorias);

        tablaCategorias.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, anterior, seleccionada) -> {
                    if (seleccionada != null) {
                        modoEdicion = true;
                        mostrarCategoria(seleccionada);
                        desbloquearFormulario();
                        btnEliminar.setDisable(false);
                    } else {
                        btnEliminar.setDisable(true);
                    }
                });

        cargarCategorias();
        bloquearFormulario();

        lblFecha.setText(LocalDate.now().format(
                DateTimeFormatter.ofPattern(
                        "dd/MM/yyyy", Locale.getDefault())));
    }

    private void actualizarEstadoGuardar() {
        String nombre = txtNombre.getText().trim();
        String descripcion = txtDescripcion.getText().trim();
        String estado = cmbEstado.getValue();

        boolean nombreValido = !nombre.isEmpty()
                && nombre.matches("[\\p{L} ]+");

        boolean descripcionValida = !descripcion.isEmpty();

        boolean estadoValido = estado != null
                && !estado.isBlank();

        boolean formularioValido = nombreValido
                && descripcionValida
                && estadoValido;

        btnGuardar.setDisable(!formularioValido);
    }

    private void cargarCategorias() {
        listaCategorias.setAll(categoriaDAO.listar());

        int total = listaCategorias.size();
        lblTotalCategorias.setText(
                total + (total == 1
                        ? " categoría registrada"
                        : " categorías registradas"));
    }

    private void mostrarCategoria(Categoria categoria) {
        txtNombre.setText(Objects.toString(categoria.getNombre(), ""));
        txtDescripcion.setText(Objects.toString(categoria.getDescripcion(), ""));
        cmbEstado.setValue(categoria.getEstado());
    }

    @FXML
    private void nuevo() {
        modoEdicion = false;
        tablaCategorias.getSelectionModel().clearSelection();
        limpiarFormulario();
        cmbEstado.setValue("Activo");
        desbloquearFormulario();
        txtNombre.requestFocus();
    }

    @FXML
    private void guardar() {
        String nombre = txtNombre.getText().trim();
        String descripcion = txtDescripcion.getText().trim();
        String estado = cmbEstado.getValue();

        if (nombre.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING,
                    "Campo obligatorio",
                    "Escribe el nombre de la categoría.");
            txtNombre.requestFocus();
            return;
        }

        if (estado == null || estado.isBlank()) {
            mostrarAlerta(Alert.AlertType.WARNING,
                    "Estado obligatorio",
                    "Selecciona el estado de la categoría.");
            return;
        }

        Categoria seleccionada = tablaCategorias
                .getSelectionModel().getSelectedItem();

        if (modoEdicion && seleccionada == null) {
            mostrarAlerta(Alert.AlertType.WARNING,
                    "Sin selección",
                    "Selecciona la categoría que deseas actualizar.");
            return;
        }

        int idExcluir = modoEdicion
                ? seleccionada.getIdCategoria()
                : 0;

        // Validar antes de intentar insertar o actualizar.
        if (categoriaDAO.existeNombre(nombre, idExcluir)) {
            mostrarAlerta(Alert.AlertType.WARNING,
                    "Nombre duplicado",
                    "Ya existe una categoría con ese nombre. "
                            + "Escribe un nombre diferente.");
            txtNombre.requestFocus();
            return;
        }

        Categoria categoria;
        boolean guardado;

        if (modoEdicion) {
            categoria = seleccionada;
            categoria.setNombre(nombre);
            categoria.setDescripcion(
                    descripcion.isEmpty() ? null : descripcion);
            categoria.setEstado(estado);

            guardado = categoriaDAO.actualizar(categoria);
        } else {
            int siguienteId = categoriaDAO.obtenerSiguienteId();

            if (siguienteId <= 0) {
                mostrarAlerta(Alert.AlertType.ERROR,
                        "Error de base de datos",
                        "No se pudo obtener el ID para la categoría.");
                return;
            }

            categoria = new Categoria(
                    siguienteId,
                    estado,
                    nombre,
                    descripcion.isEmpty() ? null : descripcion);

            guardado = categoriaDAO.insertar(categoria);
        }

        if (guardado) {
            mostrarAlerta(Alert.AlertType.INFORMATION,
                    modoEdicion ? "Actualización exitosa" : "Registro exitoso",
                    modoEdicion
                            ? "La categoría se actualizó correctamente."
                            : "La categoría se agregó correctamente.");

            terminarEdicion();
        } else {
            mostrarAlerta(Alert.AlertType.ERROR,
                    "No se pudo guardar",
                    "No se pudo guardar la categoría. "
                            + "Comprueba la conexión y que el nombre no exista.");
        }
    }

    @FXML
    private void eliminar() {
        Categoria seleccionada = tablaCategorias
                .getSelectionModel().getSelectedItem();

        if (seleccionada == null) {
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText("¿Eliminar esta categoría?");
        confirmacion.setContentText(
                "Categoría: " + seleccionada.getNombre());

        Optional<ButtonType> resultado = confirmacion.showAndWait();

        if (resultado.isPresent()
                && resultado.get() == ButtonType.OK) {

            if (categoriaDAO.eliminar(seleccionada.getIdCategoria())) {
                mostrarAlerta(Alert.AlertType.INFORMATION,
                        "Eliminación exitosa",
                        "La categoría se eliminó correctamente.");
                terminarEdicion();
            } else {
                mostrarAlerta(Alert.AlertType.ERROR,
                        "No se pudo eliminar",
                        "Puede que esta categoría esté asociada "
                                + "a uno o más productos.");
            }
        }
    }

    @FXML
    private void cancelar() {
        terminarEdicion();
    }

    private void terminarEdicion() {
        modoEdicion = false;
        limpiarFormulario();
        bloquearFormulario();
        cargarCategorias();
        tablaCategorias.getSelectionModel().clearSelection();
    }

    private void limpiarFormulario() {
        txtNombre.clear();
        txtDescripcion.clear();
        cmbEstado.getSelectionModel().clearSelection();
    }

    private void bloquearFormulario() {
        txtNombre.setDisable(true);
        txtDescripcion.setDisable(true);
        cmbEstado.setDisable(true);

        btnGuardar.setDisable(true);
        btnCancelar.setDisable(true);
    }

    private void desbloquearFormulario() {
        txtNombre.setDisable(false);
        txtDescripcion.setDisable(false);
        cmbEstado.setDisable(false);

        // Cancelar permanece disponible durante la captura.
        btnCancelar.setDisable(false);

        // Guardar depende de que los datos sean válidos.
        actualizarEstadoGuardar();
    }

    private void mostrarAlerta(
            Alert.AlertType tipo, String titulo, String mensaje) {
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
