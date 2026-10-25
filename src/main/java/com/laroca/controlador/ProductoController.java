package com.laroca.controlador;

import com.laroca.dao.CategoriaDAO;
import com.laroca.dao.ProductoDAO;
import com.laroca.dao.UnidadMedidaDAO;
import com.laroca.modelo.Categoria;
import com.laroca.modelo.Producto;
import com.laroca.modelo.UnidadMedida;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class ProductoController {

    private static final BigDecimal PRECIO_MAXIMO = new BigDecimal("99999999.99");

    @FXML
    private TableView<Producto> tablaProductos;

    @FXML
    private TableColumn<Producto, String> columnaCodigo;

    @FXML
    private TableColumn<Producto, String> columnaNombre;

    @FXML
    private TableColumn<Producto, String> columnaCategoria;

    @FXML
    private TableColumn<Producto, String> columnaUnidad;

    @FXML
    private TableColumn<Producto, BigDecimal> columnaPrecio;

    @FXML
    private TextField txtCodigo;

    @FXML
    private TextField txtNombre;

    @FXML
    private ComboBox<Categoria> cmbCategoria;

    @FXML
    private ComboBox<UnidadMedida> cmbUnidad;

    @FXML
    private TextField txtPrecio;

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

    private final ProductoDAO dao = new ProductoDAO();

    private final CategoriaDAO categoriaDao = new CategoriaDAO();

    private final UnidadMedidaDAO unidadDao = new UnidadMedidaDAO();

    private Producto productoSeleccionado;

    private boolean modoEdicion = false;

    @FXML
    public void initialize() {

        lblFecha.setText(
                LocalDate.now().format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        columnaCodigo.setCellValueFactory(
                new PropertyValueFactory<>("codigoBarras"));

        columnaNombre.setCellValueFactory(
                new PropertyValueFactory<>("nombre"));

        columnaCategoria.setCellValueFactory(
                new PropertyValueFactory<>("nombreCategoria"));

        columnaUnidad.setCellValueFactory(
                new PropertyValueFactory<>("nombreUnidad"));

        columnaPrecio.setCellValueFactory(
                new PropertyValueFactory<>("precio"));

        // Los ComboBox muestran el nombre de cada objeto.
        cmbCategoria.setConverter(new StringConverter<Categoria>() {
            @Override
            public String toString(Categoria categoria) {
                return categoria == null ? "" : categoria.getNombre();
            }

            @Override
            public Categoria fromString(String texto) {
                return null;
            }
        });

        cmbUnidad.setConverter(new StringConverter<UnidadMedida>() {
            @Override
            public String toString(UnidadMedida unidad) {
                return unidad == null ? "" : unidad.getNombre();
            }

            @Override
            public UnidadMedida fromString(String texto) {
                return null;
            }
        });

        cargarCategorias();
        cargarUnidades();
        cargarProductos();

        // Código de barras: únicamente números, máximo 50 dígitos.
        txtCodigo.textProperty().addListener(
                (observable, anterior, actual) -> {

                    if (!actual.matches("\\d{0,50}")) {
                        txtCodigo.setText(anterior);
                        return;
                    }

                    actualizarEstadoGuardar();
                });

        // Nombre del producto: máximo 50 caracteres.
        txtNombre.textProperty().addListener(
                (observable, anterior, actual) -> {

                    if (actual.length() > 50) {
                        txtNombre.setText(anterior);
                        return;
                    }

                    actualizarEstadoGuardar();
                });

        // Precio: únicamente números y hasta dos decimales.
        txtPrecio.textProperty().addListener(
                (observable, anterior, actual) -> {

                    if (!actual.matches("\\d*(\\.\\d{0,2})?")) {
                        txtPrecio.setText(anterior);
                        return;
                    }

                    actualizarEstadoGuardar();
                });

        // Categoría obligatoria.
        cmbCategoria.valueProperty().addListener(
                (observable, anterior, actual) -> actualizarEstadoGuardar());

        // Unidad de medida obligatoria.
        cmbUnidad.valueProperty().addListener(
                (observable, anterior, actual) -> actualizarEstadoGuardar());

        tablaProductos.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, anterior, actual) -> {

                    if (actual != null) {
                        productoSeleccionado = actual;
                        modoEdicion = true;

                        mostrarProducto(actual);

                        bloquearFormulario(false);
                        btnEliminar.setDisable(false);
                    } else {
                        btnEliminar.setDisable(true);
                    }
                });

        btnEliminar.setDisable(true);

        bloquearFormulario(true);
    }

    private void actualizarEstadoGuardar() {
        String codigo = txtCodigo.getText().trim();
        String nombre = txtNombre.getText().trim();
        String textoPrecio = txtPrecio.getText().trim();

        Categoria categoria = cmbCategoria.getValue();
        UnidadMedida unidad = cmbUnidad.getValue();

        boolean codigoValido = codigo.matches("\\d{1,50}");

        boolean nombreValido = !nombre.isEmpty()
                && nombre.length() <= 50;

        boolean categoriaValida = categoria != null;

        boolean unidadValida = unidad != null;

        boolean precioValido = false;

        try {
            BigDecimal precio = new BigDecimal(textoPrecio);

            precioValido = textoPrecio.matches("\\d+(\\.\\d{1,2})?")
                    && precio.compareTo(BigDecimal.ZERO) > 0
                    && precio.compareTo(PRECIO_MAXIMO) <= 0;

        } catch (NumberFormatException e) {
            precioValido = false;
        }

        boolean formularioValido = codigoValido
                && nombreValido
                && categoriaValida
                && unidadValida
                && precioValido;

        btnGuardar.setDisable(!formularioValido);
    }

    private void cargarProductos() {
        ObservableList<Producto> lista = FXCollections.observableArrayList(dao.listar());

        tablaProductos.setItems(lista);

        int total = lista.size();

        lblRegistros.setText(
                total + (total == 1
                        ? " producto registrado"
                        : " productos registrados"));
    }

    // Solo se ofrecen las categorías activas.
    private void cargarCategorias() {
        ObservableList<Categoria> lista = FXCollections.observableArrayList();

        for (Categoria categoria : categoriaDao.listar()) {
            if ("Activo".equals(categoria.getEstado())) {
                lista.add(categoria);
            }
        }

        cmbCategoria.setItems(lista);
    }

    // Solo se ofrecen las unidades activas.
    private void cargarUnidades() {
        ObservableList<UnidadMedida> lista = FXCollections.observableArrayList();

        for (UnidadMedida unidad : unidadDao.listar()) {
            if ("Activo".equals(unidad.getEstado())) {
                lista.add(unidad);
            }
        }

        cmbUnidad.setItems(lista);
    }

    private void mostrarProducto(Producto producto) {
        txtCodigo.setText(Objects.toString(producto.getCodigoBarras(), ""));
        txtNombre.setText(Objects.toString(producto.getNombre(), ""));
        txtPrecio.setText(Objects.toString(producto.getPrecio(), ""));

        seleccionarCategoria(producto.getIdCategoria(), producto.getNombreCategoria());
        seleccionarUnidad(producto.getIdUnidad(), producto.getNombreUnidad());
    }

    // Si la categoría está inactiva no aparece en la lista,
    // así que se muestra con un objeto temporal.
    private void seleccionarCategoria(int id, String nombre) {
        for (Categoria categoria : cmbCategoria.getItems()) {
            if (categoria.getIdCategoria() == id) {
                cmbCategoria.setValue(categoria);
                return;
            }
        }

        cmbCategoria.setValue(new Categoria(id, null, nombre, null));
    }

    // Si la unidad está inactiva no aparece en la lista,
    // así que se muestra con un objeto temporal.
    private void seleccionarUnidad(int id, String nombre) {
        for (UnidadMedida unidad : cmbUnidad.getItems()) {
            if (unidad.getIdUnidadMedida() == id) {
                cmbUnidad.setValue(unidad);
                return;
            }
        }

        cmbUnidad.setValue(new UnidadMedida(id, null, null, nombre));
    }

    @FXML
    private void nuevo() {
        modoEdicion = false;
        productoSeleccionado = null;

        tablaProductos.getSelectionModel().clearSelection();

        limpiarFormulario();

        bloquearFormulario(false);

        btnEliminar.setDisable(true);

        txtCodigo.requestFocus();
    }

    @FXML
    private void guardar() {
        String codigo = txtCodigo.getText().trim();
        String nombre = txtNombre.getText().trim();
        String textoPrecio = txtPrecio.getText().trim();
        Categoria categoria = cmbCategoria.getValue();
        UnidadMedida unidad = cmbUnidad.getValue();

        if (codigo.isEmpty()) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Campo obligatorio",
                    "Debes ingresar el código de barras.");
            txtCodigo.requestFocus();
            return;
        }

        if (!codigo.matches("\\d+")) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Código no válido",
                    "El código de barras solo puede contener números.");
            txtCodigo.requestFocus();
            return;
        }

        if (codigo.length() > 50) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Código no válido",
                    "El código de barras no puede tener más de 50 dígitos.");
            txtCodigo.requestFocus();
            return;
        }

        if (nombre.isEmpty()) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Campo obligatorio",
                    "Debes ingresar el nombre del producto.");
            txtNombre.requestFocus();
            return;
        }

        if (nombre.length() > 50) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Nombre no válido",
                    "El nombre del producto no puede tener más de 50 caracteres.");
            txtNombre.requestFocus();
            return;
        }

        if (categoria == null) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Categoría obligatoria",
                    "Selecciona la categoría del producto.");
            return;
        }

        if (unidad == null) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Unidad obligatoria",
                    "Selecciona la unidad de medida del producto.");
            return;
        }

        BigDecimal precio;

        try {
            precio = new BigDecimal(textoPrecio).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Precio no válido",
                    "Debes ingresar un precio numérico.");
            txtPrecio.requestFocus();
            return;
        }

        if (precio.compareTo(BigDecimal.ZERO) <= 0
                || precio.compareTo(PRECIO_MAXIMO) > 0) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Precio no válido",
                    "El precio debe ser mayor a 0 y menor o igual a 99,999,999.99.");
            txtPrecio.requestFocus();
            return;
        }

        int idExcluir = 0;

        if (modoEdicion && productoSeleccionado != null) {
            idExcluir = productoSeleccionado.getIdProducto();
        }

        if (dao.existeCodigoBarras(codigo, idExcluir)) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Código duplicado",
                    "Ya existe un producto con ese código de barras. Escribe un código diferente.");

            txtCodigo.requestFocus();
            return;
        }

        if (modoEdicion) {
            if (productoSeleccionado == null) {
                mostrarAlerta(
                        Alert.AlertType.WARNING,
                        "Sin selección",
                        "Selecciona un producto para editar.");
                return;
            }

            productoSeleccionado.setCodigoBarras(codigo);
            productoSeleccionado.setNombre(nombre);
            productoSeleccionado.setIdCategoria(categoria.getIdCategoria());
            productoSeleccionado.setIdUnidad(unidad.getIdUnidadMedida());
            productoSeleccionado.setPrecio(precio);

            if (dao.actualizar(productoSeleccionado)) {
                finalizarOperacion();

                mostrarAlerta(
                        Alert.AlertType.INFORMATION,
                        "Actualización exitosa",
                        "El producto se actualizó correctamente.");
            } else {
                mostrarAlerta(
                        Alert.AlertType.ERROR,
                        "Error",
                        "No se pudo actualizar el producto.");
            }

        } else {
            int id = dao.obtenerSiguienteId();

            if (id <= 0) {
                mostrarAlerta(
                        Alert.AlertType.ERROR,
                        "Error",
                        "No se pudo obtener el ID del producto.");
                return;
            }

            Producto producto = new Producto(id, unidad.getIdUnidadMedida(),
                    categoria.getIdCategoria(), codigo, nombre, precio);

            if (dao.insertar(producto)) {
                finalizarOperacion();

                mostrarAlerta(
                        Alert.AlertType.INFORMATION,
                        "Registro exitoso",
                        "El producto se registró correctamente.");
            } else {
                mostrarAlerta(
                        Alert.AlertType.ERROR,
                        "Error",
                        "No se pudo registrar el producto.");
            }
        }
    }

    @FXML
    private void eliminar() {
        if (productoSeleccionado == null) {
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);

        confirmacion.setTitle("Eliminar producto");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText(
                "¿Deseas eliminar el producto \""
                        + productoSeleccionado.getNombre() + "\"?");

        Optional<ButtonType> respuesta = confirmacion.showAndWait();

        if (respuesta.isPresent()
                && respuesta.get() == ButtonType.OK) {

            if (dao.eliminar(productoSeleccionado.getIdProducto())) {
                finalizarOperacion();

                mostrarAlerta(
                        Alert.AlertType.INFORMATION,
                        "Eliminación exitosa",
                        "El producto se eliminó correctamente.");
            } else {
                mostrarAlerta(
                        Alert.AlertType.ERROR,
                        "No se pudo eliminar",
                        "El producto podría estar asociado a compras, ventas o mermas.");
            }
        }
    }

    @FXML
    private void cancelar() {
        finalizarOperacion();
    }

    private void finalizarOperacion() {
        modoEdicion = false;
        productoSeleccionado = null;

        limpiarFormulario();
        bloquearFormulario(true);

        cargarProductos();

        tablaProductos.getSelectionModel().clearSelection();

        btnEliminar.setDisable(true);
    }

    private void limpiarFormulario() {
        txtCodigo.clear();
        txtNombre.clear();
        txtPrecio.clear();
        cmbCategoria.setValue(null);
        cmbUnidad.setValue(null);
    }

    private void bloquearFormulario(boolean bloquear) {
        txtCodigo.setDisable(bloquear);
        txtNombre.setDisable(bloquear);
        cmbCategoria.setDisable(bloquear);
        cmbUnidad.setDisable(bloquear);
        txtPrecio.setDisable(bloquear);

        // Guardar siempre empieza deshabilitado.
        btnGuardar.setDisable(true);

        // Cancelar se habilita durante la captura.
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

        ventanaActual.setScene(new Scene(vista));
        ventanaActual.setTitle("Sistema Abarrotes La Roca");
        ventanaActual.setResizable(false);
    }
}
