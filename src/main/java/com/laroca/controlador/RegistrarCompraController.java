package com.laroca.controlador;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;

import com.laroca.dao.CompraDAO;
import com.laroca.dao.ProductoDAO;
import com.laroca.dao.ProveedorDAO;
import com.laroca.modelo.Compra;
import com.laroca.modelo.DetalleCompra;
import com.laroca.modelo.Estados;
import com.laroca.modelo.Producto;
import com.laroca.modelo.Proveedor;
import com.laroca.modelo.Sesion;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;

public class RegistrarCompraController {

    @FXML
    private Button btnNuevaCompra;
    @FXML
    private Button btnCancelarCompra;
    @FXML
    private Label lblFolio;
    @FXML
    private Label lblFechaCompra;

    @FXML
    private TextField txtCodigo;
    @FXML
    private TextField txtCantidad;
    @FXML
    private TextField txtPrecio;
    @FXML
    private Button btnBuscar;
    @FXML
    private Button btnAgregar;

    @FXML
    private TableView<DetalleCompra> tableCarrito;
    @FXML
    private TableColumn<DetalleCompra, Void> colNumero;
    @FXML
    private TableColumn<DetalleCompra, String> colProducto;
    @FXML
    private TableColumn<DetalleCompra, Integer> colCantidad;
    @FXML
    private TableColumn<DetalleCompra, String> colPrecio;
    @FXML
    private TableColumn<DetalleCompra, String> colSubtotal;
    @FXML
    private TableColumn<DetalleCompra, Void> colQuitar;

    @FXML
    private ComboBox<Proveedor> cmbProveedor;
    @FXML
    private TextField txtEmpleado;
    @FXML
    private TextField txtFecha;
    @FXML
    private Label lblNumProductos;
    @FXML
    private Label lblArticulos;
    @FXML
    private Label lblTotal;
    @FXML
    private Button btnGuardar;

    @FXML
    private Label lblEstadoFooter;

    private final CompraDAO compraDAO = new CompraDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final ProveedorDAO proveedorDAO = new ProveedorDAO();

    private final ObservableList<DetalleCompra> carrito =
            FXCollections.observableArrayList();

    // Producto identificado por código o mediante Buscar.
    private Producto productoElegido;

    @FXML
    public void initialize() {

        colNumero.setCellFactory(columna -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : String.valueOf(getIndex() + 1));
            }
        });

        colProducto.setCellValueFactory(
                new PropertyValueFactory<>("nombreProducto"));

        colCantidad.setCellValueFactory(
                new PropertyValueFactory<>("cantidad"));

        colPrecio.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                Formato.dinero(c.getValue().getPrecioCompra())));

        colSubtotal.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                Formato.dinero(c.getValue().getSubtotal())));

        configurarEdicionCantidad();

        // Botón para quitar productos del carrito.
        colQuitar.setCellFactory(columna ->
                new CeldaQuitar<>(carrito::remove));

        tableCarrito.setItems(carrito);

        tableCarrito.setPlaceholder(
                new Label("Escanea o busca un producto para agregarlo a la compra"));

        // Supr quita el renglón seleccionado.
        tableCarrito.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE) {
                DetalleCompra linea =
                        tableCarrito.getSelectionModel().getSelectedItem();

                if (linea != null) {
                    carrito.remove(linea);
                }
            }
        });

        // Cantidad: solo enteros de 1 a 9999.
        txtCantidad.setTextFormatter(new TextFormatter<String>(cambio ->
                cambio.getControlNewText().matches("([1-9]\\d{0,3})?")
                        ? cambio : null));

        // Precio de compra: hasta 7 enteros y 2 decimales.
        txtPrecio.setTextFormatter(new TextFormatter<String>(cambio ->
                cambio.getControlNewText().matches("^\\d{0,7}(\\.\\d{0,2})?$")
                        ? cambio : null));

        cmbProveedor.setConverter(new StringConverter<Proveedor>() {
            @Override
            public String toString(Proveedor proveedor) {
                return proveedor == null ? "" : proveedor.getNombre();
            }

            @Override
            public Proveedor fromString(String texto) {
                return null;
            }
        });

        // Solo proveedores activos.
        cmbProveedor.setItems(FXCollections.observableArrayList(
                proveedorDAO.listar().stream()
                        .filter(p -> Estados.esActivo(p.getEstado()))
                        .toList()));

        cmbProveedor.valueProperty().addListener(
                (obs, anterior, actual) -> actualizarResumen());

        txtEmpleado.setText(Sesion.haySesion()
                ? Sesion.getUsuario()
                : "Sin sesión iniciada");

        carrito.addListener(
                (ListChangeListener<DetalleCompra>) cambio ->
                        actualizarResumen());

        iniciarCompra();

        Navegacion.verificarSesion(tableCarrito);
    }

    // =========================================================
    // EDICIÓN DE CANTIDAD DIRECTAMENTE EN LA TABLA
    // =========================================================

    private void configurarEdicionCantidad() {

        tableCarrito.setEditable(true);
        colCantidad.setEditable(true);

        colCantidad.setCellFactory(columna ->
                new TableCell<DetalleCompra, Integer>() {

            private final TextField txtEditar = new TextField();

            {
                // Solo permite números enteros de hasta cuatro dígitos.
                txtEditar.setTextFormatter(new TextFormatter<String>(cambio -> {
                    String texto = cambio.getControlNewText();

                    return texto.matches("\\d{0,4}") ? cambio : null;
                }));

                // Enter confirma.
                txtEditar.setOnAction(event -> confirmarCantidad());

                // Escape cancela.
                txtEditar.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.ESCAPE) {
                        cancelEdit();
                    }
                });

                // Al perder el foco también intenta confirmar.
                txtEditar.focusedProperty().addListener(
                        (obs, anterior, enfocado) -> {
                            if (!enfocado && isEditing()) {
                                confirmarCantidad();
                            }
                        });
            }

            @Override
            public void startEdit() {
                if (isEmpty()) {
                    return;
                }

                super.startEdit();

                txtEditar.setText(
                        getItem() == null ? "" : getItem().toString());

                setText(null);
                setGraphic(txtEditar);

                txtEditar.selectAll();
                txtEditar.requestFocus();
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();

                setGraphic(null);
                setText(getItem() == null ? "" : getItem().toString());
            }

            @Override
            protected void updateItem(Integer cantidad, boolean vacio) {
                super.updateItem(cantidad, vacio);

                if (vacio) {
                    setText(null);
                    setGraphic(null);
                } else if (isEditing()) {
                    setText(null);
                    setGraphic(txtEditar);
                } else {
                    setText(cantidad == null ? "" : cantidad.toString());
                    setGraphic(null);
                }
            }

            private void confirmarCantidad() {

                if (!isEditing()) {
                    return;
                }

                String texto = txtEditar.getText().trim();

                if (texto.isEmpty()) {
                    mostrarAlerta(
                            "Cantidad inválida",
                            "Debes ingresar una cantidad mayor que cero.");

                    cancelEdit();
                    return;
                }

                int nuevaCantidad;

                try {
                    nuevaCantidad = Integer.parseInt(texto);
                } catch (NumberFormatException e) {
                    mostrarAlerta(
                            "Cantidad inválida",
                            "Ingresa un número entero válido.");

                    cancelEdit();
                    return;
                }

                if (nuevaCantidad <= 0) {
                    mostrarAlerta(
                            "Cantidad inválida",
                            "La cantidad debe ser mayor que cero.");

                    cancelEdit();
                    return;
                }

                DetalleCompra linea = getTableView()
                        .getItems().get(getIndex());

                linea.setCantidad(nuevaCantidad);

                commitEdit(nuevaCantidad);

                tableCarrito.refresh();
                actualizarResumen();
            }
        });
    }

    // =========================================================
    // INICIO DE COMPRA
    // =========================================================

    private void iniciarCompra() {

        Compra siguiente = new Compra();

        siguiente.setIdCompra(compraDAO.obtenerSiguienteId());

        lblFolio.setText(
                siguiente.getIdCompra() > 0
                        ? siguiente.getFolio()
                        : "—");

        String hoy = Formato.fecha(LocalDate.now());

        lblFechaCompra.setText("·  " + hoy);
        txtFecha.setText(hoy);

        actualizarResumen();

        Platform.runLater(() -> txtCodigo.requestFocus());
    }

    // =========================================================
    // IDENTIFICAR Y BUSCAR PRODUCTOS
    // =========================================================

    @FXML
    private void resolverProducto() {

        String texto = txtCodigo.getText().trim();

        if (productoElegido != null
                && texto.equals(textoProducto(productoElegido))) {

            txtCantidad.requestFocus();
            txtCantidad.selectAll();

            return;
        }

        Producto producto = texto.isEmpty()
                ? null
                : productoDAO.buscarActivoPorCodigo(texto);

        if (producto == null) {
            producto = BuscadorProductos.buscar(ventana(), texto);
        }

        if (producto != null) {
            elegirProducto(producto);
        }
    }

    @FXML
    private void buscarProducto() {

        String texto = txtCodigo.getText().trim();

        if (productoElegido != null
                && texto.equals(textoProducto(productoElegido))) {

            texto = "";
        }

        Producto producto = BuscadorProductos.buscar(ventana(), texto);

        if (producto != null) {
            elegirProducto(producto);
        }
    }

    private void elegirProducto(Producto producto) {

        productoElegido = producto;

        txtCodigo.setText(textoProducto(producto));

        BigDecimal ultimoPrecio =
                compraDAO.ultimoPrecioCompra(producto.getIdProducto());

        txtPrecio.setText(
                ultimoPrecio == null
                        ? ""
                        : ultimoPrecio.setScale(
                                2, RoundingMode.HALF_UP).toPlainString());

        txtCantidad.requestFocus();
        txtCantidad.selectAll();
    }

    private String textoProducto(Producto producto) {

        return producto.getCodigoBarras()
                + " — "
                + producto.getNombre();
    }

    // =========================================================
    // AGREGAR PRODUCTOS AL CARRITO
    // =========================================================

    @FXML
    private void agregarProducto() {

        String texto = txtCodigo.getText().trim();

        // Identificar el producto primero.
        if (productoElegido == null
                || !texto.equals(textoProducto(productoElegido))) {

            String precioCapturado = txtPrecio.getText();

            resolverProducto();

            if (productoElegido == null
                    || !txtCodigo.getText().trim()
                            .equals(textoProducto(productoElegido))) {

                return;
            }

            if (precioCapturado.isEmpty()) {
                txtPrecio.requestFocus();
                txtPrecio.selectAll();

                return;
            }

            txtPrecio.setText(precioCapturado);
        }

        if (txtCantidad.getText().isEmpty()) {
            mostrarAlerta(
                    "Cantidad obligatoria",
                    "Escribe la cantidad a agregar (1 a 9999).");

            txtCantidad.requestFocus();

            return;
        }

        String textoPrecio = txtPrecio.getText();

        if (textoPrecio.isEmpty() || textoPrecio.equals(".")) {
            mostrarAlerta(
                    "Precio obligatorio",
                    "Escribe el precio de compra del producto.");

            txtPrecio.requestFocus();

            return;
        }

        BigDecimal precio;

        try {
            precio = new BigDecimal(textoPrecio)
                    .setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            mostrarAlerta(
                    "Precio no válido",
                    "Escribe un precio de compra válido.");

            txtPrecio.requestFocus();

            return;
        }

        if (precio.compareTo(BigDecimal.ZERO) <= 0) {
            mostrarAlerta(
                    "Precio no válido",
                    "El precio de compra debe ser mayor a $0.00.");

            txtPrecio.requestFocus();

            return;
        }

        int cantidad = Integer.parseInt(txtCantidad.getText());

        if (agregarAlCarrito(productoElegido, cantidad, precio)) {

            txtCodigo.clear();
            txtCantidad.setText("1");
            txtPrecio.clear();

            productoElegido = null;
        }

        txtCodigo.requestFocus();
    }

    private boolean agregarAlCarrito(
            Producto producto,
            int cantidad,
            BigDecimal precio) {

        if (cantidad <= 0 || precio == null
                || precio.compareTo(BigDecimal.ZERO) <= 0) {

            mostrarAlerta(
                    "Datos inválidos",
                    "Verifica la cantidad y el precio de compra.");

            return false;
        }

        BigDecimal precioVenta = producto.getPrecio();

        if (precioVenta != null
                && precio.compareTo(precioVenta) >= 0
                && !confirmar(
                        "Precio de compra alto",
                        "El precio de compra (" + Formato.dinero(precio)
                                + ") es mayor o igual al precio de venta ("
                                + Formato.dinero(precioVenta)
                                + "). ¿Agregar de todos modos?")) {

            return false;
        }

        DetalleCompra linea = buscarLinea(producto.getIdProducto());

        if (linea == null) {

            linea = new DetalleCompra(
                    producto.getIdProducto(),
                    producto.getNombre(),
                    cantidad,
                    precio);

            carrito.add(linea);

        } else {

            if (linea.getPrecioCompra().compareTo(precio) != 0) {

                if (!confirmar(
                        "Cambiar precio de compra",
                        "¿Reemplazar el precio de compra de "
                                + Formato.dinero(linea.getPrecioCompra())
                                + " por "
                                + Formato.dinero(precio)
                                + "?")) {

                    return false;
                }

                linea.setPrecioCompra(precio);
            }

            linea.setCantidad(linea.getCantidad() + cantidad);

            tableCarrito.refresh();
            actualizarResumen();
        }

        tableCarrito.getSelectionModel().select(linea);
        tableCarrito.scrollTo(linea);

        return true;
    }

    private DetalleCompra buscarLinea(int idProducto) {

        for (DetalleCompra linea : carrito) {

            if (linea.getIdProducto() == idProducto) {
                return linea;
            }
        }

        return null;
    }

    // =========================================================
    // RESUMEN Y VALIDACIONES
    // =========================================================

    private boolean datosCompraValidos() {

        if (carrito.isEmpty() || cmbProveedor.getValue() == null) {
            return false;
        }

        for (DetalleCompra linea : carrito) {

            if (linea.getCantidad() <= 0
                    || linea.getPrecioCompra() == null
                    || linea.getPrecioCompra()
                            .compareTo(BigDecimal.ZERO) <= 0) {

                return false;
            }
        }

        return calcularTotal().compareTo(BigDecimal.ZERO) > 0;
    }

    private void actualizarResumen() {

        int articulos = 0;

        for (DetalleCompra linea : carrito) {
            articulos += linea.getCantidad();
        }

        int productos = carrito.size();

        lblNumProductos.setText(String.valueOf(productos));
        lblArticulos.setText(String.valueOf(articulos));
        lblTotal.setText(Formato.dinero(calcularTotal()));

        btnGuardar.setDisable(!datosCompraValidos());

        lblEstadoFooter.setText(
                productos + (productos == 1
                        ? " producto en la compra"
                        : " productos en la compra"));
    }

    private BigDecimal calcularTotal() {

        BigDecimal total = BigDecimal.ZERO;

        for (DetalleCompra linea : carrito) {
            total = total.add(linea.getSubtotal());
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    // =========================================================
    // GUARDAR COMPRA
    // =========================================================

    @FXML
    private void guardarCompra() {

        Proveedor proveedor = cmbProveedor.getValue();

        if (!datosCompraValidos()) {
            mostrarAlerta(
                    "Compra incompleta",
                    "Verifica que hayas seleccionado un proveedor "
                            + "y agregado productos con cantidades y precios válidos.");

            return;
        }

        if (!Sesion.haySesion()) {
            mostrarAlerta(
                    "Sin sesión",
                    "Inicia sesión para poder registrar compras.");

            return;
        }

        int articulos = 0;

        for (DetalleCompra linea : carrito) {
            articulos += linea.getCantidad();
        }

        BigDecimal total = calcularTotal();

        if (!confirmar(
                "Registrar compra",
                "¿Registrar la compra " + lblFolio.getText() + "?\n\n"
                        + "Proveedor: " + proveedor.getNombre() + "\n"
                        + "Productos: " + carrito.size() + "\n"
                        + "Artículos: " + articulos + "\n"
                        + "Total: " + Formato.dinero(total) + "\n\n"
                        + "La cantidad de cada producto se sumará al stock.")) {

            return;
        }

        Compra compra = new Compra();

        compra.setIdProveedor(proveedor.getIdProveedor());
        compra.setIdEmpleado(Sesion.getIdEmpleado());
        compra.setFecha(LocalDate.now());
        compra.setTotal(total);
        compra.setEstado(Estados.ACTIVO);

        int idCompra = compraDAO.registrar(
                compra,
                new ArrayList<>(carrito));

        if (idCompra <= 0) {

            mostrarAlerta(
                    "No se pudo registrar la compra",
                    compraDAO.getMensajeError());

            return;
        }

        mostrarAlerta(
                "Compra registrada",
                "Compra " + compra.getFolio() + " registrada.");

        volverAGestion(idCompra);
    }

    // =========================================================
    // CANCELAR COMPRA Y NAVEGACIÓN
    // =========================================================

    @FXML
    private void cancelarCompra() {

        if (!carrito.isEmpty() && !confirmar(
                "Cancelar compra",
                "¿Deseas cancelar la compra? "
                        + (carrito.size() == 1
                                ? "Se descartará el producto agregado."
                                : "Se descartarán los "
                                        + carrito.size()
                                        + " productos agregados."))) {

            return;
        }

        volverAGestion(0);
    }

    private void volverAGestion(int idCompra) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/laroca/vista/compras.fxml"));

            Parent root = loader.load();

            Navegacion.mostrar(
                    (Stage) ventana(),
                    root,
                    "La Roca - Compras");

            if (idCompra > 0) {

                CompraController gestion = loader.getController();

                gestion.seleccionarPorId(idCompra);
            }

        } catch (IOException e) {

            e.printStackTrace();

            mostrarAlerta(
                    "Error",
                    "No se pudo abrir la gestión de compras.");
        }
    }

    private Window ventana() {

        return tableCarrito.getScene().getWindow();
    }

    private boolean confirmar(String titulo, String mensaje) {

        Alert confirmacion = new Alert(
                Alert.AlertType.CONFIRMATION,
                mensaje,
                ButtonType.YES,
                ButtonType.NO);

        confirmacion.setTitle(titulo);
        confirmacion.setHeaderText(null);
        confirmacion.showAndWait();

        return confirmacion.getResult() == ButtonType.YES;
    }

    private void mostrarAlerta(String titulo, String mensaje) {

        Alert alerta = new Alert(Alert.AlertType.WARNING);

        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);

        alerta.showAndWait();
    }
}
