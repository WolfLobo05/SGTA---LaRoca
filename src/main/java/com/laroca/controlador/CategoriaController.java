package com.laroca.controlador;

import com.laroca.dao.CategoriaDAO;
import com.laroca.modelo.Categoria;
import com.laroca.modelo.Estados;

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

import java.io.IOException;
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

        tablaCategorias.setItems(listaCategorias);

        tablaCategorias.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, anterior, seleccionada) -> {
                    if (seleccionada != null) {
                        mostrarCategoria(seleccionada);
                        btnEliminar.setDisable(false);

                        if (chkMostrarInactivos.isSelected()) {
                            // Las categorías inactivas solo se consultan.
                            modoEdicion = false;
                            bloquearFormulario();
                            btnCancelar.setDisable(false);
                        } else {
                            modoEdicion = true;
                            desbloquearFormulario();
                        }
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

    // Muestra solo las activas o solo las inactivas, según la casilla.
    private void cargarCategorias() {
        boolean inactivos = chkMostrarInactivos.isSelected();

        listaCategorias.setAll(categoriaDAO.listar().stream()
                .filter(c -> Estados.esActivo(c.getEstado()) != inactivos)
                .toList());

        int total = listaCategorias.size();
        lblTotalCategorias.setText(
                total + (total == 1
                        ? (inactivos ? " categoría inactiva" : " categoría activa")
                        : (inactivos ? " categorías inactivas" : " categorías activas")));
    }

    private void mostrarCategoria(Categoria categoria) {
        txtNombre.setText(Objects.toString(categoria.getNombre(), ""));
        txtDescripcion.setText(Objects.toString(categoria.getDescripcion(), ""));
        txtEstado.setText(Estados.mostrar(categoria.getEstado()));
    }

    @FXML
    private void nuevo() {
        if (chkMostrarInactivos.isSelected()) {
            return;
        }

        modoEdicion = false;
        tablaCategorias.getSelectionModel().clearSelection();
        limpiarFormulario();
        txtEstado.setText(Estados.ACTIVO);
        desbloquearFormulario();
        txtNombre.requestFocus();
    }

    @FXML
    private void guardar() {
        String nombre = txtNombre.getText().trim();
        String descripcion = txtDescripcion.getText().trim();

        if (nombre.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING,
                    "Campo obligatorio",
                    "Escribe el nombre de la categoría.");
            txtNombre.requestFocus();
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
        // Se compara contra todas las categorías, activas e inactivas.
        if (categoriaDAO.existeNombre(nombre, idExcluir)) {
            if (categoriaDAO.existeNombreInactivo(nombre, idExcluir)) {
                mostrarAlerta(Alert.AlertType.WARNING,
                        "Categoría inactiva",
                        "Ya existe una categoría con ese nombre, pero está inactiva. "
                                + "Puedes reactivarla desde \"Mostrar inactivos\".");
            } else {
                mostrarAlerta(Alert.AlertType.WARNING,
                        "Nombre duplicado",
                        "Ya existe una categoría con ese nombre. "
                                + "Escribe un nombre diferente.");
            }
            txtNombre.requestFocus();
            return;
        }

        Categoria categoria;
        boolean guardado;

        if (modoEdicion) {
            // El estado no se modifica al editar.
            categoria = seleccionada;
            categoria.setNombre(nombre);
            categoria.setDescripcion(
                    descripcion.isEmpty() ? null : descripcion);

            guardado = categoriaDAO.actualizar(categoria);
        } else {
            int siguienteId = categoriaDAO.obtenerSiguienteId();

            if (siguienteId <= 0) {
                mostrarAlerta(Alert.AlertType.ERROR,
                        "Error de base de datos",
                        "No se pudo obtener el ID para la categoría.");
                return;
            }

            // Toda categoría nueva se registra como Activo.
            categoria = new Categoria(
                    siguienteId,
                    Estados.ACTIVO,
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

    // Elimina (desactiva) o reactiva la categoría, según la casilla.
    @FXML
    private void eliminar() {
        Categoria seleccionada = tablaCategorias
                .getSelectionModel().getSelectedItem();

        if (seleccionada == null) {
            return;
        }

        boolean reactivar = chkMostrarInactivos.isSelected();

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle(reactivar
                ? "Confirmar reactivación"
                : "Confirmar eliminación");
        confirmacion.setHeaderText(reactivar
                ? "¿Reactivar esta categoría?"
                : "¿Eliminar esta categoría?");
        confirmacion.setContentText(
                "Categoría: " + seleccionada.getNombre() + "\n\n"
                        + (reactivar
                                ? "Volverá a estar activa y disponible para los productos."
                                : "Quedará inactiva y ya no se podrá elegir en nuevos productos. "
                                        + "Los productos que ya la usan no cambian."));

        Optional<ButtonType> resultado = confirmacion.showAndWait();

        if (resultado.isPresent()
                && resultado.get() == ButtonType.OK) {

            boolean exito = reactivar
                    ? categoriaDAO.reactivar(seleccionada.getIdCategoria())
                    : categoriaDAO.desactivar(seleccionada.getIdCategoria());

            if (exito) {
                mostrarAlerta(Alert.AlertType.INFORMATION,
                        reactivar ? "Reactivación exitosa" : "Eliminación exitosa",
                        reactivar
                                ? "La categoría se reactivó correctamente."
                                : "La categoría se marcó como inactiva.");
                terminarEdicion();
            } else {
                mostrarAlerta(Alert.AlertType.ERROR,
                        "No se pudo cambiar el estado",
                        "No se pudo actualizar el estado de la categoría. "
                                + "Comprueba la conexión.");
            }
        }
    }

    // Cambia entre la lista de activas y la de inactivas.
    @FXML
    private void cambiarMostrarInactivos() {
        boolean inactivos = chkMostrarInactivos.isSelected();

        btnNuevo.setDisable(inactivos);
        btnEliminar.setText(inactivos ? "Reactivar" : "Eliminar");

        terminarEdicion();
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
        txtEstado.clear();
    }

    private void bloquearFormulario() {
        txtNombre.setDisable(true);
        txtDescripcion.setDisable(true);

        btnGuardar.setDisable(true);
        btnCancelar.setDisable(true);
    }

    private void desbloquearFormulario() {
        txtNombre.setDisable(false);
        txtDescripcion.setDisable(false);

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

        Navegacion.mostrar(ventanaActual, vista, Navegacion.TITULO_MENU);
    }
}
