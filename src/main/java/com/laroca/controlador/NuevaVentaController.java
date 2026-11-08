package com.laroca.controlador;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.laroca.dao.ClienteDAO;
import com.laroca.dao.MetodoPagoDAO;
import com.laroca.dao.ProductoDAO;
import com.laroca.dao.VentaDAO;
import com.laroca.modelo.Cliente;
import com.laroca.modelo.DetalleVenta;
import com.laroca.modelo.Estados;
import com.laroca.modelo.MetodoPago;
import com.laroca.modelo.Producto;
import com.laroca.modelo.Sesion;
import com.laroca.modelo.Venta;

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
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.geometry.Side;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;

public class NuevaVentaController {

    private static final String CLIENTE_GENERAL = "Público en general";
    private static final String METODO_EFECTIVO = "Efectivo";
    private static final int MAX_SUGERENCIAS = 8;

    @FXML
    private Button btnNuevaVenta;
    @FXML
    private Button btnCancelarVenta;
    @FXML
    private Label lblFolio;
    @FXML
    private Label lblFechaVenta;

    @FXML
    private TextField txtCodigo;
    @FXML
    private TextField txtCantidad;
    @FXML
    private Button btnBuscar;
    @FXML
    private Button btnAgregar;

    @FXML
    private TableView<DetalleVenta> tableCarrito;
    @FXML
    private TableColumn<DetalleVenta, Void> colNumero;
    @FXML
    private TableColumn<DetalleVenta, String> colProducto;
    @FXML
    private TableColumn<DetalleVenta, Integer> colCantidad;
    @FXML
    private TableColumn<DetalleVenta, String> colPrecio;
    @FXML
    private TableColumn<DetalleVenta, String> colSubtotal;
    @FXML
    private TableColumn<DetalleVenta, Void> colQuitar;

    @FXML
    private Label lblStock;

    @FXML
    private TextField txtEmpleado;
    @FXML
    private TextField txtCliente;
    @FXML
    private Button btnElegirCliente;
    @FXML
    private TextField txtFecha;
    @FXML
    private ComboBox<MetodoPago> cmbMetodo;
    @FXML
    private Label lblNumProductos;
    @FXML
    private Label lblArticulos;
    @FXML
    private Label lblTotal;
    @FXML
    private Button btnCobrar;

    @FXML
    private Label lblEstadoFooter;

    private final VentaDAO ventaDAO = new VentaDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final MetodoPagoDAO metodoPagoDAO = new MetodoPagoDAO();

    private final ObservableList<DetalleVenta> carrito =
            FXCollections.observableArrayList();

    private final Map<Integer, Integer> existencias = new HashMap<>();

    private final ContextMenu menuSugerencias = new ContextMenu();

    private Cliente clienteVenta;
    private Producto productoElegido;

    // Evita que seleccionar una sugerencia vuelva a disparar la búsqueda.
    private boolean actualizandoCodigo;

    @FXML
    public void initialize() {

        // Numeración de los renglones.
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
                Formato.dinero(c.getValue().getPrecioUnitario())));

        colSubtotal.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                Formato.dinero(c.getValue().getSubtotal())));

        configurarEdicionCantidad();

        // Botón para quitar productos del carrito.
        colQuitar.setCellFactory(columna -> new CeldaQuitar<>(carrito::remove));

        tableCarrito.setItems(carrito);
        tableCarrito.setPlaceholder(
                new Label("Escanea o busca un producto para agregarlo a la venta"));

        // Suprimir elimina el producto seleccionado.
        tableCarrito.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE) {
                DetalleVenta linea =
                        tableCarrito.getSelectionModel().getSelectedItem();

                if (linea != null) {
                    carrito.remove(linea);
                }
            }
        });

        // Cantidad para agregar: enteros de 1 a 9999.
        txtCantidad.setTextFormatter(new TextFormatter<String>(cambio ->
                cambio.getControlNewText().matches("([1-9]\\d{0,3})?")
                        ? cambio : null));

        // Configuración del método de pago.
        cmbMetodo.setConverter(new StringConverter<MetodoPago>() {
            @Override
            public String toString(MetodoPago metodo) {
                return metodo == null ? "" : metodo.getNombre();
            }

            @Override
            public MetodoPago fromString(String texto) {
                return null;
            }
        });

        cmbMetodo.setItems(
                FXCollections.observableArrayList(metodoPagoDAO.listar()));

        cmbMetodo.valueProperty().addListener(
                (obs, anterior, actual) -> actualizarResumen());

        // Empleado de la sesión.
        txtEmpleado.setText(Sesion.haySesion()
                ? Sesion.getUsuario()
                : "Sin sesión iniciada");

        // Actualizar resumen cuando cambie el carrito.
        carrito.addListener(
                (ListChangeListener<DetalleVenta>) cambio -> actualizarResumen());

        // Actualizar existencia al seleccionar un producto.
        tableCarrito.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, anterior, actual) -> actualizarStock());

        // Búsqueda dinámica.
        configurarBusquedaDinamica();

        iniciarVenta();

        Navegacion.verificarSesion(tableCarrito);
    }

    // =========================================================
    // EDICIÓN DE CANTIDAD EN LA TABLA
    // =========================================================

    private void configurarEdicionCantidad() {

        tableCarrito.setEditable(true);
        colCantidad.setEditable(true);

        colCantidad.setCellFactory(columna ->
                new TableCell<DetalleVenta, Integer>() {

            private final TextField txtEditar = new TextField();

            {
                txtEditar.setTextFormatter(new TextFormatter<String>(cambio -> {
                    String texto = cambio.getControlNewText();
                    return texto.matches("\\d{0,4}") ? cambio : null;
                }));

                txtEditar.setOnAction(event -> confirmarCantidad());

                txtEditar.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.ESCAPE) {
                        cancelEdit();
                    }
                });

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

                DetalleVenta linea = getTableView()
                        .getItems().get(getIndex());

                int stock = existencias.getOrDefault(
                        linea.getIdProducto(), 0);

                if (nuevaCantidad > stock) {
                    mostrarAlerta(
                            "Stock insuficiente",
                            "Solo hay " + stock
                                    + " unidades disponibles de este producto.");
                    cancelEdit();
                    return;
                }

                linea.setCantidad(nuevaCantidad);

                commitEdit(nuevaCantidad);

                tableCarrito.refresh();
                actualizarResumen();
            }
        });
    }

    // =========================================================
    // BÚSQUEDA DINÁMICA DE PRODUCTOS
    // =========================================================

    private void configurarBusquedaDinamica() {

        txtCodigo.textProperty().addListener((obs, anterior, actual) -> {

            if (actualizandoCodigo) {
                return;
            }

            productoElegido = null;

            mostrarSugerencias(actual);
        });

        // Si se presiona Enter con sugerencias disponibles,
        // selecciona la primera coincidencia.
        txtCodigo.setOnAction(event -> {

            if (menuSugerencias.isShowing()
                    && !menuSugerencias.getItems().isEmpty()) {

                menuSugerencias.getItems().get(0).fire();
                event.consume();
            } else {
                agregarProducto();
            }
        });

        // Ocultar sugerencias al perder el foco.
        txtCodigo.focusedProperty().addListener(
                (obs, anterior, enfocado) -> {
                    if (!enfocado) {
                        Platform.runLater(() -> menuSugerencias.hide());
                    }
                });
    }

    private void mostrarSugerencias(String texto) {

        menuSugerencias.hide();
        menuSugerencias.getItems().clear();

        String filtro = texto == null ? "" : texto.trim();

        if (filtro.isEmpty()) {
            return;
        }

        List<Producto> resultados;

        try {
            resultados = productoDAO.buscarActivos(filtro);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        if (resultados == null || resultados.isEmpty()) {
            return;
        }

        int limite = Math.min(resultados.size(), MAX_SUGERENCIAS);

        for (int i = 0; i < limite; i++) {

            Producto producto = resultados.get(i);

            String descripcion = producto.getNombre()
                    + "  |  Código: "
                    + Objects.toString(producto.getCodigoBarras(), "Sin código")
                    + "  |  "
                    + Formato.dinero(producto.getPrecio())
                    + "  |  Stock: "
                    + producto.getExistencia();

            MenuItem sugerencia = new MenuItem(descripcion);

            sugerencia.setOnAction(event -> seleccionarSugerencia(producto));

            menuSugerencias.getItems().add(sugerencia);
        }

        if (txtCodigo.getScene() != null
                && txtCodigo.getScene().getWindow().isShowing()
                && txtCodigo.isFocused()) {

            menuSugerencias.show(txtCodigo, Side.BOTTOM, 0, 0);
        }
    }

    private void seleccionarSugerencia(Producto producto) {

        if (producto == null) {
            return;
        }

        menuSugerencias.hide();

        productoElegido = producto;

        actualizandoCodigo = true;
        txtCodigo.setText(textoProducto(producto));
        actualizandoCodigo = false;

        txtCantidad.requestFocus();
        txtCantidad.selectAll();
    }

    // =========================================================
    // INICIO DE VENTA
    // =========================================================

    private void iniciarVenta() {

        Venta siguiente = new Venta();
        siguiente.setIdVenta(ventaDAO.obtenerSiguienteId());

        lblFolio.setText(
                siguiente.getIdVenta() > 0
                        ? siguiente.getFolio()
                        : "—");

        String hoy = Formato.fecha(LocalDate.now());

        lblFechaVenta.setText("·  " + hoy);
        txtFecha.setText(hoy);

        clienteVenta = clientePorDefecto();

        txtCliente.setText(
                clienteVenta == null
                        ? ""
                        : clienteVenta.getNombreCompleto());

        for (MetodoPago metodo : cmbMetodo.getItems()) {

            if (METODO_EFECTIVO.equalsIgnoreCase(
                    Objects.toString(metodo.getNombre(), "").trim())) {

                cmbMetodo.setValue(metodo);
                break;
            }
        }

        if (cmbMetodo.getValue() == null
                && !cmbMetodo.getItems().isEmpty()) {

            cmbMetodo.setValue(cmbMetodo.getItems().get(0));
        }

        actualizarResumen();

        Platform.runLater(() -> txtCodigo.requestFocus());
    }

    private Cliente clientePorDefecto() {

        for (Cliente cliente : clientesActivos()) {

            if (CLIENTE_GENERAL.equalsIgnoreCase(
                    Objects.toString(cliente.getNombre(), "").trim())) {

                return cliente;
            }
        }

        return null;
    }

    private List<Cliente> clientesActivos() {

        return clienteDAO.listar().stream()
                .filter(c -> Estados.esActivo(c.getEstado()))
                .toList();
    }

    // =========================================================
    // AGREGAR PRODUCTOS
    // =========================================================

    @FXML
    private void agregarProducto() {

        String texto = txtCodigo.getText().trim();

        if (txtCantidad.getText().isEmpty()) {
            mostrarAlerta(
                    "Cantidad obligatoria",
                    "Escribe la cantidad a agregar (1 a 9999).");

            txtCantidad.requestFocus();
            return;
        }

        int cantidad = Integer.parseInt(txtCantidad.getText());

        Producto producto;

        if (productoElegido != null
                && texto.equals(textoProducto(productoElegido))) {

            producto = productoElegido;

        } else if (texto.isEmpty()) {

            producto = abrirBuscador("");

        } else {

            // Primero se intenta localizar el código exacto.
            producto = productoDAO.buscarActivoPorCodigo(texto);

            if (producto == null) {
                // Si no es un código, se abre el buscador con el filtro.
                producto = abrirBuscador(texto);
            }
        }

        if (producto == null) {
            return;
        }

        if (agregarAlCarrito(producto, cantidad)) {

            menuSugerencias.hide();

            txtCodigo.clear();
            txtCantidad.setText("1");

            productoElegido = null;
        }

        txtCodigo.requestFocus();
    }

    @FXML
    private void buscarProducto() {

        String texto = txtCodigo.getText().trim();

        if (productoElegido != null
                && texto.equals(textoProducto(productoElegido))) {

            texto = "";
        }

        Producto producto = abrirBuscador(texto);

        if (producto != null) {
            seleccionarSugerencia(producto);
        }
    }

    private Producto abrirBuscador(String filtro) {

        return BuscadorProductos.buscar(ventana(), filtro);
    }

    private String textoProducto(Producto producto) {

        return producto.getCodigoBarras()
                + " — "
                + producto.getNombre();
    }

    private boolean agregarAlCarrito(
            Producto producto,
            int cantidad) {

        if (producto.getPrecio() == null) {
            mostrarAlerta(
                    "Producto sin precio",
                    "\"" + producto.getNombre()
                            + "\" no tiene precio registrado.");

            return false;
        }

        if (cantidad <= 0) {
            mostrarAlerta(
                    "Cantidad inválida",
                    "La cantidad debe ser mayor que cero.");

            return false;
        }

        existencias.put(
                producto.getIdProducto(),
                producto.getExistencia());

        DetalleVenta linea = buscarLinea(producto.getIdProducto());

        int enVenta = linea == null ? 0 : linea.getCantidad();

        int nuevaCantidad = enVenta + cantidad;

        if (nuevaCantidad > producto.getExistencia()) {

            mostrarAlerta(
                    "Stock insuficiente",
                    "Solo hay " + producto.getExistencia()
                            + " de \"" + producto.getNombre()
                            + "\" en existencia"
                            + (enVenta > 0
                                    ? " y ya tienes " + enVenta
                                            + " en la venta."
                                    : "."));

            return false;
        }

        if (linea == null) {

            linea = new DetalleVenta(
                    producto.getIdProducto(),
                    producto.getNombre(),
                    cantidad,
                    producto.getPrecio());

            carrito.add(linea);

        } else {

            linea.setCantidad(nuevaCantidad);

            tableCarrito.refresh();
            actualizarResumen();
        }

        tableCarrito.getSelectionModel().select(linea);
        tableCarrito.scrollTo(linea);

        return true;
    }

    private DetalleVenta buscarLinea(int idProducto) {

        for (DetalleVenta linea : carrito) {

            if (linea.getIdProducto() == idProducto) {
                return linea;
            }
        }

        return null;
    }

    // =========================================================
    // CLIENTES
    // =========================================================

    @FXML
    private void elegirCliente() {

        List<Cliente> activos = clientesActivos();

        DialogoSeleccion<Cliente> dialogo =
                new DialogoSeleccion<>(
                        ventana(),
                        "Seleccionar cliente",
                        texto -> {

                            String filtro = texto.toLowerCase(Locale.ROOT);

                            return activos.stream()
                                    .filter(c ->
                                            c.getNombreCompleto()
                                                    .toLowerCase(Locale.ROOT)
                                                    .contains(filtro)
                                            || Objects.toString(
                                                    c.getTelefono(), "")
                                                    .contains(filtro))
                                    .toList();
                        });

        dialogo.agregarColumna(
                "Nombre",
                Cliente::getNombreCompleto,
                230,
                false);

        dialogo.agregarColumna(
                "Teléfono",
                c -> Objects.toString(c.getTelefono(), ""),
                100,
                false);

        dialogo.agregarColumna(
                "Correo",
                c -> Objects.toString(c.getCorreo(), ""),
                170,
                false);

        Cliente elegido = dialogo.mostrar("");

        if (elegido != null) {

            clienteVenta = elegido;

            txtCliente.setText(elegido.getNombreCompleto());

            actualizarResumen();
        }
    }

    // =========================================================
    // VALIDACIÓN Y RESUMEN
    // =========================================================

    private boolean datosVentaValidos() {

        if (carrito.isEmpty()
                || clienteVenta == null
                || cmbMetodo.getValue() == null) {

            return false;
        }

        for (DetalleVenta linea : carrito) {

            if (linea.getCantidad() <= 0
                    || linea.getPrecioUnitario() == null
                    || linea.getPrecioUnitario()
                            .compareTo(BigDecimal.ZERO) <= 0) {

                return false;
            }

            int stock = existencias.getOrDefault(
                    linea.getIdProducto(), 0);

            if (linea.getCantidad() > stock) {
                return false;
            }
        }

        return calcularTotal().compareTo(BigDecimal.ZERO) > 0;
    }

    private void actualizarResumen() {

        int articulos = 0;

        for (DetalleVenta linea : carrito) {
            articulos += linea.getCantidad();
        }

        int productos = carrito.size();

        lblNumProductos.setText(String.valueOf(productos));
        lblArticulos.setText(String.valueOf(articulos));
        lblTotal.setText(Formato.dinero(calcularTotal()));

        btnCobrar.setDisable(!datosVentaValidos());

        lblEstadoFooter.setText(
                productos + (productos == 1
                        ? " producto en la venta"
                        : " productos en la venta"));

        actualizarStock();
    }

    private void actualizarStock() {

        DetalleVenta linea =
                tableCarrito.getSelectionModel().getSelectedItem();

        if (linea == null) {

            lblStock.setText(
                    "Stock disponible del producto seleccionado: —");

            return;
        }

        int existencia = existencias.getOrDefault(
                linea.getIdProducto(), 0);

        lblStock.setText(
                "Stock disponible del producto seleccionado: "
                        + (existencia - linea.getCantidad()));
    }

    private BigDecimal calcularTotal() {

        BigDecimal total = BigDecimal.ZERO;

        for (DetalleVenta linea : carrito) {
            total = total.add(linea.getSubtotal());
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    // =========================================================
    // COBRAR
    // =========================================================

    @FXML
    private void cobrar() {

        if (!Sesion.haySesion()) {
            mostrarAlerta(
                    "Sin sesión",
                    "Inicia sesión para poder registrar ventas.");

            return;
        }

        if (!datosVentaValidos()) {
            mostrarAlerta(
                    "Venta incompleta",
                    "Verifica que haya productos, un cliente, "
                            + "un método de pago y cantidades válidas.");

            return;
        }

        MetodoPago metodo = cmbMetodo.getValue();

        BigDecimal total = calcularTotal();
        BigDecimal montoPagado = total;
        BigDecimal cambio = BigDecimal.ZERO.setScale(2);

        if (METODO_EFECTIVO.equalsIgnoreCase(
                Objects.toString(metodo.getNombre(), "").trim())) {

            montoPagado = pedirMontoRecibido(total);

            if (montoPagado == null) {
                return;
            }

            cambio = montoPagado.subtract(total);
        }

        Venta venta = new Venta();

        venta.setIdEmpleado(Sesion.getIdEmpleado());
        venta.setIdCliente(clienteVenta.getIdCliente());
        venta.setIdMetodo(metodo.getIdMetodo());
        venta.setFecha(LocalDateTime.now().withNano(0));
        venta.setTotal(total);
        venta.setMontoPagado(montoPagado);
        venta.setCambio(cambio);
        venta.setEstado(Estados.ACTIVO);

        int idVenta = ventaDAO.registrar(
                venta,
                new ArrayList<>(carrito));

        if (idVenta <= 0) {

            mostrarAlerta(
                    "No se pudo registrar la venta",
                    ventaDAO.getMensajeError());

            return;
        }

        TicketController.mostrar(ventana(), idVenta);

        volverAGestion(idVenta);
    }

    private BigDecimal pedirMontoRecibido(BigDecimal total) {

        TextInputDialog dialogo = new TextInputDialog("0.00");

        dialogo.initOwner(ventana());
        dialogo.setTitle("Cobrar en efectivo");
        dialogo.setHeaderText(
                "Total a pagar: " + Formato.dinero(total));
        dialogo.setContentText("Monto recibido:");

        TextField editor = dialogo.getEditor();

        editor.setTextFormatter(new TextFormatter<String>(cambio ->
                cambio.getControlNewText()
                        .matches("\\d{0,8}(\\.\\d{0,2})?")
                        ? cambio : null));

        Button btnAceptar = (Button) dialogo.getDialogPane()
                .lookupButton(ButtonType.OK);

        btnAceptar.setDisable(true);

        editor.textProperty().addListener(
                (obs, anterior, actual) ->
                        btnAceptar.setDisable(
                                leerMonto(actual).compareTo(total) < 0));

        Runnable seleccionarTodo = () -> {

            if (editor.getText().equals("0.00")) {
                editor.requestFocus();
                editor.selectAll();
            }
        };

        dialogo.setOnShown(e -> {

            Platform.runLater(seleccionarTodo);

            dialogo.getDialogPane().getScene().getWindow()
                    .focusedProperty()
                    .addListener((obs, antes, enfocada) -> {

                        if (enfocada) {
                            Platform.runLater(seleccionarTodo);
                        }
                    });
        });

        Optional<String> respuesta = dialogo.showAndWait();

        if (respuesta.isEmpty()) {
            return null;
        }

        return leerMonto(respuesta.get());
    }

    private BigDecimal leerMonto(String texto) {

        String limpio = texto == null ? "" : texto.trim();

        if (limpio.isEmpty() || limpio.equals(".")) {
            return BigDecimal.ZERO.setScale(2);
        }

        return new BigDecimal(limpio)
                .setScale(2, RoundingMode.HALF_UP);
    }

    // =========================================================
    // CANCELAR Y NAVEGACIÓN
    // =========================================================

    @FXML
    private void cancelarVenta() {

        if (!carrito.isEmpty() && !confirmar(
                "Cancelar venta",
                "¿Deseas cancelar la venta? "
                        + (carrito.size() == 1
                                ? "Se descartará el producto agregado."
                                : "Se descartarán los "
                                        + carrito.size()
                                        + " productos agregados."))) {

            return;
        }

        volverAGestion(0);
    }

    private void volverAGestion(int idVenta) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/laroca/vista/ventas.fxml"));

            Parent root = loader.load();

            Navegacion.mostrar(
                    (Stage) ventana(),
                    root,
                    "La Roca - Ventas");

            if (idVenta > 0) {

                VentaController gestion = loader.getController();

                gestion.seleccionarPorId(idVenta);
            }

        } catch (IOException e) {

            e.printStackTrace();

            mostrarAlerta(
                    "Error",
                    "No se pudo abrir la gestión de ventas.");
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
