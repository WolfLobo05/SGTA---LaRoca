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
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;

public class NuevaVentaController {

    private static final String CLIENTE_GENERAL = "Público en general";
    private static final String METODO_EFECTIVO = "Efectivo";

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

    private final ObservableList<DetalleVenta> carrito = FXCollections.observableArrayList();

    // Existencia de cada producto agregado (id_producto -> existencia).
    private final Map<Integer, Integer> existencias = new HashMap<>();

    private Cliente clienteVenta;

    // Producto elegido con Buscar; su texto queda en txtCodigo.
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
                Formato.dinero(c.getValue().getPrecioUnitario())));
        colSubtotal.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                Formato.dinero(c.getValue().getSubtotal())));

        // Botón ✕ en cada renglón para quitarlo de la venta.
        colQuitar.setCellFactory(columna -> new CeldaQuitar<>(carrito::remove));

        tableCarrito.setItems(carrito);
        tableCarrito.setPlaceholder(
                new Label("Escanea o busca un producto para agregarlo a la venta"));

        // Supr quita el renglón seleccionado (además de la ✕ de cada renglón).
        tableCarrito.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE) {
                DetalleVenta linea = tableCarrito.getSelectionModel().getSelectedItem();
                if (linea != null) {
                    carrito.remove(linea);
                }
            }
        });

        // Cantidad: solo enteros de 1 a 9999.
        txtCantidad.setTextFormatter(new TextFormatter<String>(cambio ->
                cambio.getControlNewText().matches("([1-9]\\d{0,3})?")
                        ? cambio : null));

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
        cmbMetodo.setItems(FXCollections.observableArrayList(metodoPagoDAO.listar()));

        txtEmpleado.setText(Sesion.haySesion()
                ? Sesion.getUsuario()
                : "Sin sesión iniciada");

        carrito.addListener((ListChangeListener<DetalleVenta>) cambio -> actualizarResumen());

        tableCarrito.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, anterior, actual) -> actualizarStock());

        iniciarVenta();

        Navegacion.verificarSesion(tableCarrito);
    }

    // Folio, fecha, cliente y método de pago con los que abre la pantalla.
    private void iniciarVenta() {
        Venta siguiente = new Venta();
        siguiente.setIdVenta(ventaDAO.obtenerSiguienteId());
        lblFolio.setText(siguiente.getIdVenta() > 0 ? siguiente.getFolio() : "—");

        String hoy = Formato.fecha(LocalDate.now());
        lblFechaVenta.setText("·  " + hoy);
        txtFecha.setText(hoy);

        clienteVenta = clientePorDefecto();
        txtCliente.setText(clienteVenta == null ? "" : clienteVenta.getNombreCompleto());

        for (MetodoPago metodo : cmbMetodo.getItems()) {
            if (METODO_EFECTIVO.equalsIgnoreCase(Objects.toString(metodo.getNombre(), "").trim())) {
                cmbMetodo.setValue(metodo);
            }
        }
        if (cmbMetodo.getValue() == null && !cmbMetodo.getItems().isEmpty()) {
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

    // Enter en el código o el botón Agregar.
    // Primero busca el código exacto; si no existe, abre el buscador.
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

        if (productoElegido != null && texto.equals(textoProducto(productoElegido))) {
            producto = productoElegido;
        } else if (texto.isEmpty()) {
            producto = abrirBuscador("");
        } else {
            producto = productoDAO.buscarActivoPorCodigo(texto);

            if (producto == null) {
                producto = abrirBuscador(texto);
            }
        }

        if (producto == null) {
            return;
        }

        if (agregarAlCarrito(producto, cantidad)) {
            txtCodigo.clear();
            txtCantidad.setText("1");
            productoElegido = null;
        }

        txtCodigo.requestFocus();
    }

    // Abre el buscador y deja el producto elegido en el campo de código.
    @FXML
    private void buscarProducto() {
        String texto = txtCodigo.getText().trim();

        if (productoElegido != null && texto.equals(textoProducto(productoElegido))) {
            texto = "";
        }

        Producto producto = abrirBuscador(texto);

        if (producto != null) {
            productoElegido = producto;
            txtCodigo.setText(textoProducto(producto));
            txtCantidad.requestFocus();
            txtCantidad.selectAll();
        }
    }

    private Producto abrirBuscador(String filtro) {
        return BuscadorProductos.buscar(ventana(), filtro);
    }

    private String textoProducto(Producto producto) {
        return producto.getCodigoBarras() + " — " + producto.getNombre();
    }

    // Agrega el producto o suma la cantidad si ya está en la venta.
    // Nunca permite más de lo que hay en existencia.
    private boolean agregarAlCarrito(Producto producto, int cantidad) {
        if (producto.getPrecio() == null) {
            mostrarAlerta(
                    "Producto sin precio",
                    "\"" + producto.getNombre() + "\" no tiene precio registrado.");
            return false;
        }

        existencias.put(producto.getIdProducto(), producto.getExistencia());

        DetalleVenta linea = buscarLinea(producto.getIdProducto());
        int enVenta = linea == null ? 0 : linea.getCantidad();
        int nuevaCantidad = enVenta + cantidad;

        if (nuevaCantidad > producto.getExistencia()) {
            mostrarAlerta(
                    "Stock insuficiente",
                    "Solo hay " + producto.getExistencia() + " de \""
                            + producto.getNombre() + "\" en existencia"
                            + (enVenta > 0
                                    ? " y ya tienes " + enVenta + " en la venta."
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

    @FXML
    private void elegirCliente() {
        List<Cliente> activos = clientesActivos();

        DialogoSeleccion<Cliente> dialogo = new DialogoSeleccion<>(
                ventana(), "Seleccionar cliente",
                texto -> {
                    String filtro = texto.toLowerCase(Locale.ROOT);
                    return activos.stream()
                            .filter(c -> c.getNombreCompleto().toLowerCase(Locale.ROOT).contains(filtro)
                                    || Objects.toString(c.getTelefono(), "").contains(filtro))
                            .toList();
                });

        dialogo.agregarColumna("Nombre", Cliente::getNombreCompleto, 230, false);
        dialogo.agregarColumna("Teléfono", c -> Objects.toString(c.getTelefono(), ""), 100, false);
        dialogo.agregarColumna("Correo", c -> Objects.toString(c.getCorreo(), ""), 170, false);

        Cliente elegido = dialogo.mostrar("");

        if (elegido != null) {
            clienteVenta = elegido;
            txtCliente.setText(elegido.getNombreCompleto());
        }
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

        btnCobrar.setDisable(carrito.isEmpty());

        lblEstadoFooter.setText(
                productos + (productos == 1
                        ? " producto en la venta"
                        : " productos en la venta"));

        actualizarStock();
    }

    // Existencia menos lo que ya está en la venta.
    private void actualizarStock() {
        DetalleVenta linea = tableCarrito.getSelectionModel().getSelectedItem();

        if (linea == null) {
            lblStock.setText("Stock disponible del producto seleccionado: —");
            return;
        }

        int existencia = existencias.getOrDefault(linea.getIdProducto(), 0);

        lblStock.setText("Stock disponible del producto seleccionado: "
                + (existencia - linea.getCantidad()));
    }

    private BigDecimal calcularTotal() {
        BigDecimal total = BigDecimal.ZERO;

        for (DetalleVenta linea : carrito) {
            total = total.add(linea.getSubtotal());
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    @FXML
    private void cobrar() {
        if (carrito.isEmpty()) {
            return;
        }

        if (!Sesion.haySesion()) {
            mostrarAlerta(
                    "Sin sesión",
                    "Inicia sesión para poder registrar ventas.");
            return;
        }

        if (clienteVenta == null) {
            mostrarAlerta(
                    "Cliente obligatorio",
                    "Selecciona el cliente de la venta.");
            return;
        }

        MetodoPago metodo = cmbMetodo.getValue();

        if (metodo == null) {
            mostrarAlerta(
                    "Método de pago obligatorio",
                    "Selecciona el método de pago.");
            return;
        }

        BigDecimal total = calcularTotal();
        BigDecimal montoPagado = total;
        BigDecimal cambio = BigDecimal.ZERO.setScale(2);

        if (METODO_EFECTIVO.equalsIgnoreCase(Objects.toString(metodo.getNombre(), "").trim())) {
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

        int idVenta = ventaDAO.registrar(venta, new ArrayList<>(carrito));

        if (idVenta <= 0) {
            mostrarAlerta(
                    "No se pudo registrar la venta",
                    ventaDAO.getMensajeError());
            return;
        }

        TicketController.mostrar(ventana(), idVenta);

        volverAGestion(idVenta);
    }

    // Pide el efectivo recibido. Devuelve null si se cancela.
    // El campo empieza en 0.00 con el texto seleccionado, así lo que se
    // escribe lo reemplaza. Aceptar se habilita solo si el monto cubre el total.
    private BigDecimal pedirMontoRecibido(BigDecimal total) {
        TextInputDialog dialogo = new TextInputDialog("0.00");
        dialogo.initOwner(ventana());
        dialogo.setTitle("Cobrar en efectivo");
        dialogo.setHeaderText("Total a pagar: " + Formato.dinero(total));
        dialogo.setContentText("Monto recibido:");

        TextField editor = dialogo.getEditor();

        // Solo números con hasta dos decimales.
        editor.setTextFormatter(new TextFormatter<String>(cambio ->
                cambio.getControlNewText().matches("\\d{0,8}(\\.\\d{0,2})?")
                        ? cambio : null));

        Button btnAceptar = (Button) dialogo.getDialogPane().lookupButton(ButtonType.OK);
        btnAceptar.setDisable(true);
        editor.textProperty().addListener((obs, anterior, actual) ->
                btnAceptar.setDisable(leerMonto(actual).compareTo(total) < 0));

        // Poner el foco en el campo con todo seleccionado. En el primer
        // diálogo Windows activa la ventana después de mostrarla, así que
        // también se hace al recibir el foco, mientras siga en 0.00.
        Runnable seleccionarTodo = () -> {
            if (editor.getText().equals("0.00")) {
                editor.requestFocus();
                editor.selectAll();
            }
        };

        dialogo.setOnShown(e -> {
            Platform.runLater(seleccionarTodo);

            dialogo.getDialogPane().getScene().getWindow().focusedProperty()
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

    // Texto del monto como dinero; vacío o incompleto cuenta como 0.
    private BigDecimal leerMonto(String texto) {
        String limpio = texto == null ? "" : texto.trim();

        if (limpio.isEmpty() || limpio.equals(".")) {
            return BigDecimal.ZERO.setScale(2);
        }

        return new BigDecimal(limpio).setScale(2, RoundingMode.HALF_UP);
    }

    // "Cancelar venta": descarta la venta y vuelve a Gestión de Ventas.
    @FXML
    private void cancelarVenta() {
        if (!carrito.isEmpty() && !confirmar(
                "Cancelar venta",
                "¿Deseas cancelar la venta? "
                        + (carrito.size() == 1
                                ? "Se descartará el producto agregado."
                                : "Se descartarán los " + carrito.size() + " productos agregados."))) {
            return;
        }

        volverAGestion(0);
    }

    // Vuelve a Gestión de Ventas. Si idVenta > 0, deja esa venta seleccionada.
    private void volverAGestion(int idVenta) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/laroca/vista/ventas.fxml"));

            Parent root = loader.load();

            Navegacion.mostrar((Stage) ventana(), root, "La Roca - Ventas");

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
