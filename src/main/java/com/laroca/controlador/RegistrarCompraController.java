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

    private final ObservableList<DetalleCompra> carrito = FXCollections.observableArrayList();

    // Producto identificado (por código o con Buscar); su texto queda en txtCodigo.
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

        // Botón ✕ en cada renglón para quitarlo de la compra.
        colQuitar.setCellFactory(columna -> new CeldaQuitar<>(carrito::remove));

        tableCarrito.setItems(carrito);
        tableCarrito.setPlaceholder(
                new Label("Escanea o busca un producto para agregarlo a la compra"));

        // Supr quita el renglón seleccionado (además de la ✕ de cada renglón).
        tableCarrito.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE) {
                DetalleCompra linea = tableCarrito.getSelectionModel().getSelectedItem();
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

        // Solo se ofrecen los proveedores activos.
        cmbProveedor.setItems(FXCollections.observableArrayList(
                proveedorDAO.listar().stream()
                        .filter(p -> Estados.esActivo(p.getEstado()))
                        .toList()));

        cmbProveedor.valueProperty().addListener(
                (obs, anterior, actual) -> actualizarResumen());

        txtEmpleado.setText(Sesion.haySesion()
                ? Sesion.getUsuario()
                : "Sin sesión iniciada");

        carrito.addListener((ListChangeListener<DetalleCompra>) cambio -> actualizarResumen());

        iniciarCompra();

        Navegacion.verificarSesion(tableCarrito);
    }

    // Folio y fecha con los que abre la pantalla.
    private void iniciarCompra() {
        Compra siguiente = new Compra();
        siguiente.setIdCompra(compraDAO.obtenerSiguienteId());
        lblFolio.setText(siguiente.getIdCompra() > 0 ? siguiente.getFolio() : "—");

        String hoy = Formato.fecha(LocalDate.now());
        lblFechaCompra.setText("·  " + hoy);
        txtFecha.setText(hoy);

        // Sin proveedor por defecto: se elige en cada compra.
        actualizarResumen();

        Platform.runLater(() -> txtCodigo.requestFocus());
    }

    // Enter en el código: busca el código exacto entre los productos
    // activos; si no existe, abre el buscador.
    @FXML
    private void resolverProducto() {
        String texto = txtCodigo.getText().trim();

        if (productoElegido != null && texto.equals(textoProducto(productoElegido))) {
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

        if (productoElegido != null && texto.equals(textoProducto(productoElegido))) {
            texto = "";
        }

        Producto producto = BuscadorProductos.buscar(ventana(), texto);

        if (producto != null) {
            elegirProducto(producto);
        }
    }

    // Deja el producto en el campo y sugiere su último precio de compra.
    private void elegirProducto(Producto producto) {
        productoElegido = producto;
        txtCodigo.setText(textoProducto(producto));

        BigDecimal ultimoPrecio = compraDAO.ultimoPrecioCompra(producto.getIdProducto());
        txtPrecio.setText(ultimoPrecio == null
                ? ""
                : ultimoPrecio.setScale(2, RoundingMode.HALF_UP).toPlainString());

        txtCantidad.requestFocus();
        txtCantidad.selectAll();
    }

    private String textoProducto(Producto producto) {
        return producto.getCodigoBarras() + " — " + producto.getNombre();
    }

    @FXML
    private void agregarProducto() {
        String texto = txtCodigo.getText().trim();

        // Si el producto aún no está identificado, se identifica primero.
        if (productoElegido == null || !texto.equals(textoProducto(productoElegido))) {
            String precioCapturado = txtPrecio.getText();

            resolverProducto();

            if (productoElegido == null
                    || !txtCodigo.getText().trim().equals(textoProducto(productoElegido))) {
                return;
            }

            // Sin precio escrito: revisar primero el precio sugerido.
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

        BigDecimal precio = new BigDecimal(textoPrecio).setScale(2, RoundingMode.HALF_UP);

        if (precio.compareTo(BigDecimal.ZERO) <= 0) {
            mostrarAlerta(
                    "Precio no válido",
                    "El precio de compra debe ser mayor a $0.00.");
            txtPrecio.requestFocus();
            return;
        }

        if (agregarAlCarrito(productoElegido, Integer.parseInt(txtCantidad.getText()), precio)) {
            txtCodigo.clear();
            txtCantidad.setText("1");
            txtPrecio.clear();
            productoElegido = null;
        }

        txtCodigo.requestFocus();
    }

    // Agrega el producto o suma la cantidad si ya está en la compra.
    // Las compras no tienen límite de existencia.
    private boolean agregarAlCarrito(Producto producto, int cantidad, BigDecimal precio) {
        BigDecimal precioVenta = producto.getPrecio();

        if (precioVenta != null && precio.compareTo(precioVenta) >= 0
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
                                + Formato.dinero(linea.getPrecioCompra()) + " por "
                                + Formato.dinero(precio) + "?")) {
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

    private void actualizarResumen() {
        int articulos = 0;

        for (DetalleCompra linea : carrito) {
            articulos += linea.getCantidad();
        }

        int productos = carrito.size();

        lblNumProductos.setText(String.valueOf(productos));
        lblArticulos.setText(String.valueOf(articulos));
        lblTotal.setText(Formato.dinero(calcularTotal()));

        // Guardar solo con proveedor y al menos un producto.
        btnGuardar.setDisable(carrito.isEmpty() || cmbProveedor.getValue() == null);

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

    @FXML
    private void guardarCompra() {
        Proveedor proveedor = cmbProveedor.getValue();

        if (carrito.isEmpty() || proveedor == null) {
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

        int idCompra = compraDAO.registrar(compra, new ArrayList<>(carrito));

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

    // "Cancelar compra": descarta la compra y vuelve a Gestión de Compras.
    @FXML
    private void cancelarCompra() {
        if (!carrito.isEmpty() && !confirmar(
                "Cancelar compra",
                "¿Deseas cancelar la compra? "
                        + (carrito.size() == 1
                                ? "Se descartará el producto agregado."
                                : "Se descartarán los " + carrito.size() + " productos agregados."))) {
            return;
        }

        volverAGestion(0);
    }

    // Vuelve a Gestión de Compras. Si idCompra > 0, deja esa compra seleccionada.
    private void volverAGestion(int idCompra) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/laroca/vista/compras.fxml"));

            Parent root = loader.load();

            Navegacion.mostrar((Stage) ventana(), root, "La Roca - Compras");

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
