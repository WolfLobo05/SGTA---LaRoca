package com.laroca.controlador;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.laroca.dao.DevolucionClienteDAO;
import com.laroca.dao.VentaDAO;
import com.laroca.modelo.DevolucionCliente;
import com.laroca.modelo.DetalleDevolucionCliente;
import com.laroca.modelo.Estados;
import com.laroca.modelo.ProductoVentaDevolucion;
import com.laroca.modelo.Sesion;
import com.laroca.modelo.Venta;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;

public class NuevaDevolucionClienteController {

    // =========================================================
    // COMPONENTES FXML
    // =========================================================

    @FXML private Button btnNuevaDevolucion;
    @FXML private Button btnCancelar;
    @FXML private Label lblFolio;
    @FXML private Label lblFecha;
    @FXML private TextField txtFolioVenta;
    @FXML private Button btnBuscarVenta;

    @FXML private TableView<ProductoVentaDevolucion> tableProductosVenta;
    @FXML private TableColumn<ProductoVentaDevolucion, String> colProducto;
    @FXML private TableColumn<ProductoVentaDevolucion, Integer> colVendidos;
    @FXML private TableColumn<ProductoVentaDevolucion, Integer> colDevueltos;
    @FXML private TableColumn<ProductoVentaDevolucion, Integer> colDisponibles;
    @FXML private TableColumn<ProductoVentaDevolucion, String> colPrecio;

    @FXML private TextField txtCantidad;
    @FXML private TextField txtMotivo;
    @FXML private Button btnAgregar;

    @FXML private TableView<DetalleDevolucionCliente> tableCarrito;
    @FXML private TableColumn<DetalleDevolucionCliente, String> colCarritoProducto;
    @FXML private TableColumn<DetalleDevolucionCliente, Integer> colCarritoCantidad;
    @FXML private TableColumn<DetalleDevolucionCliente, String> colCarritoPrecio;
    @FXML private TableColumn<DetalleDevolucionCliente, String> colCarritoSubtotal;
    @FXML private TableColumn<DetalleDevolucionCliente, String> colCarritoMotivo;
    @FXML private TableColumn<DetalleDevolucionCliente, Void> colQuitar;

    @FXML private TextField txtEmpleado;
    @FXML private TextField txtCliente;
    @FXML private TextField txtFecha;
    @FXML private TextField txtVenta;

    @FXML private Label lblNumProductos;
    @FXML private Label lblArticulos;
    @FXML private Label lblTotal;
    @FXML private Button btnRegistrar;
    @FXML private Label lblEstadoFooter;

    // =========================================================
    // DAO, LISTAS Y BUSCADOR
    // =========================================================

    private final DevolucionClienteDAO devolucionDAO = new DevolucionClienteDAO();
    private final VentaDAO ventaDAO = new VentaDAO();

    private final ObservableList<ProductoVentaDevolucion> productosVenta =
            FXCollections.observableArrayList();

    private final ObservableList<DetalleDevolucionCliente> carrito =
            FXCollections.observableArrayList();

    private Venta ventaSeleccionada;

    private final Popup popupVentas = new Popup();
    private final ListView<Venta> listaSugerencias = new ListView<>();
    private boolean seleccionandoSugerencia = false;

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // =========================================================
    // INICIALIZACIÓN
    // =========================================================

    @FXML
    public void initialize() {

        configurarTablaProductos();
        configurarTablaCarrito();

        tableProductosVenta.setItems(productosVenta);
        tableCarrito.setItems(carrito);

        configurarBuscadorVentas();

        tableProductosVenta.setPlaceholder(
                new Label("Busca una venta para consultar sus productos."));

        tableCarrito.setPlaceholder(
                new Label("Agrega los productos que deseas devolver."));

        txtCantidad.setTextFormatter(
                new TextFormatter<String>(cambio ->
                        cambio.getControlNewText().matches("\\d{0,4}")
                                ? cambio : null));

        txtEmpleado.setText(
                Sesion.haySesion() ? Sesion.getUsuario() : "Sin sesión iniciada");

        String hoy = Formato.fecha(LocalDate.now());
        txtFecha.setText(hoy);
        lblFecha.setText("·  " + hoy);

        int siguienteId = devolucionDAO.obtenerSiguienteId();
        lblFolio.setText(siguienteId > 0
                ? String.format("DC-%03d", siguienteId) : "—");

        carrito.addListener(
                (ListChangeListener<DetalleDevolucionCliente>)
                        cambio -> actualizarResumen());

        tableProductosVenta.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, anterior, actual) -> {
                    if (actual != null) txtCantidad.setText("1");
                });

        btnRegistrar.setDisable(true);
        Navegacion.verificarSesion(tableCarrito);
    }

    // =========================================================
    // CONFIGURACIÓN DE TABLAS
    // =========================================================

    private void configurarTablaProductos() {
        colProducto.setCellValueFactory(new PropertyValueFactory<>("nombreProducto"));
        colVendidos.setCellValueFactory(new PropertyValueFactory<>("cantidadVendida"));
        colDevueltos.setCellValueFactory(new PropertyValueFactory<>("cantidadDevuelta"));
        colDisponibles.setCellValueFactory(new PropertyValueFactory<>("cantidadDisponible"));

        colPrecio.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        Formato.dinero(c.getValue().getPrecioUnitario())));
    }

    private void configurarTablaCarrito() {
        colCarritoProducto.setCellValueFactory(
                new PropertyValueFactory<>("nombreProducto"));
        colCarritoCantidad.setCellValueFactory(
                new PropertyValueFactory<>("cantidad"));

        colCarritoPrecio.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        Formato.dinero(c.getValue().getPrecio())));

        colCarritoSubtotal.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        Formato.dinero(c.getValue().getSubtotal())));

        colCarritoMotivo.setCellValueFactory(
                new PropertyValueFactory<>("motivo"));

        colQuitar.setCellFactory(columna ->
                new CeldaQuitar<>(carrito::remove));

        tableCarrito.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE) {
                DetalleDevolucionCliente seleccionado =
                        tableCarrito.getSelectionModel().getSelectedItem();
                if (seleccionado != null) carrito.remove(seleccionado);
            }
        });
    }

    // =========================================================
    // BUSCADOR DE VENTAS CON SUGERENCIAS
    // =========================================================

    private void configurarBuscadorVentas() {

        listaSugerencias.setPrefHeight(180);
        listaSugerencias.setCellFactory(lista -> new ListCell<>() {
            @Override
            protected void updateItem(Venta venta, boolean vacio) {
                super.updateItem(venta, vacio);

                if (vacio || venta == null) {
                    setText(null);
                    return;
                }

                String folio = String.format("V-%03d", venta.getIdVenta());
                String cliente = venta.getNombreCliente() == null
                        ? "Cliente sin nombre" : venta.getNombreCliente();
                String fecha = venta.getFecha() == null
                        ? "Sin fecha"
                        : venta.getFecha().toLocalDate().format(FORMATO_FECHA);

                setText(folio + "  |  " + cliente + "  |  " + fecha);
            }
        });

        popupVentas.getContent().setAll(listaSugerencias);
        popupVentas.setAutoHide(true);
        popupVentas.setAutoFix(true);
        popupVentas.setHideOnEscape(true);

        txtFolioVenta.textProperty().addListener((obs, anterior, actual) -> {
            if (seleccionandoSugerencia) return;

            if (ventaSeleccionada != null) {
                String folioCargado = String.format(
                        "V-%03d", ventaSeleccionada.getIdVenta());

                if (!folioCargado.equalsIgnoreCase(actual.trim())) {
                    limpiarVenta();
                }
            }

            actualizarSugerencias(actual);
        });

        listaSugerencias.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                Venta seleccionada = listaSugerencias
                        .getSelectionModel().getSelectedItem();
                if (seleccionada != null) seleccionarVentaSugerida(seleccionada);
            }
        });

        listaSugerencias.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                Venta seleccionada = listaSugerencias
                        .getSelectionModel().getSelectedItem();
                if (seleccionada != null) seleccionarVentaSugerida(seleccionada);
            }
        });

        txtFolioVenta.focusedProperty().addListener((obs, anterior, enfocado) -> {
            if (!enfocado) {
                Platform.runLater(() -> {
                    if (!listaSugerencias.isFocused()) popupVentas.hide();
                });
            }
        });
    }

    private void actualizarSugerencias(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            popupVentas.hide();
            listaSugerencias.getItems().clear();
            return;
        }

        List<Venta> resultados = ventaDAO.buscarVentasActivas(texto);
        listaSugerencias.getItems().setAll(resultados);

        if (resultados.isEmpty()) {
            popupVentas.hide();
            return;
        }

        Bounds limites = txtFolioVenta.localToScreen(
                txtFolioVenta.getBoundsInLocal());

        if (limites == null || txtFolioVenta.getScene() == null) return;

        listaSugerencias.setPrefWidth(
                Math.max(txtFolioVenta.getWidth(), 300));

        if (!popupVentas.isShowing()) {
            popupVentas.show(
                    txtFolioVenta.getScene().getWindow(),
                    limites.getMinX(),
                    limites.getMaxY());
        } else {
            popupVentas.setX(limites.getMinX());
            popupVentas.setY(limites.getMaxY());
        }
    }

    private void seleccionarVentaSugerida(Venta venta) {
        if (venta == null) return;

        popupVentas.hide();
        seleccionandoSugerencia = true;
        txtFolioVenta.setText(String.format("V-%03d", venta.getIdVenta()));
        seleccionandoSugerencia = false;

        cargarVentaSeleccionada(venta);
    }

    // =========================================================
    // BUSCAR Y CARGAR VENTA
    // =========================================================

    @FXML
    private void buscarVenta() {
        popupVentas.hide();

        String folio = txtFolioVenta.getText().trim().toUpperCase();

        if (folio.isEmpty()) {
            mostrarAlerta("Folio obligatorio",
                    "Escribe el folio de la venta que deseas consultar.");
            return;
        }

        Matcher matcher = Pattern.compile("^(?:V-)?(\\d+)$").matcher(folio);
        if (!matcher.matches()) {
            mostrarAlerta("Venta no seleccionada",
                    "Escribe un folio válido o selecciona una venta de las sugerencias.");
            return;
        }

        int idVenta;
        try {
            idVenta = Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException e) {
            mostrarAlerta("Folio inválido", "El número de venta no es válido.");
            return;
        }

        Venta venta = ventaDAO.obtenerVenta(idVenta);
        if (venta == null) {
            limpiarVenta();
            mostrarAlerta("Venta no encontrada",
                    "No existe una venta con ese folio.");
            return;
        }

        cargarVentaSeleccionada(venta);
    }

    private void cargarVentaSeleccionada(Venta venta) {
        if (venta == null) return;

        if (!Estados.esActivo(venta.getEstado())) {
            limpiarVenta();
            mostrarAlerta("Venta cancelada",
                    "No se pueden registrar devoluciones de una venta cancelada.");
            return;
        }

        List<ProductoVentaDevolucion> productos =
                devolucionDAO.listarProductosVenta(venta.getIdVenta());

        if (productos == null || productos.isEmpty()) {
            limpiarVenta();
            mostrarAlerta("Sin productos disponibles",
                    "Esta venta no tiene productos que puedan devolverse.");
            return;
        }

        ventaSeleccionada = venta;
        productosVenta.setAll(productos);
        carrito.clear();

        txtVenta.setText(String.format("V-%03d", venta.getIdVenta()));
        txtCliente.setText(venta.getNombreCliente());

        tableProductosVenta.refresh();
        actualizarResumen();

        lblEstadoFooter.setText(
                productos.size() + " productos encontrados en la venta");
    }

    private void limpiarVenta() {
        ventaSeleccionada = null;
        productosVenta.clear();
        carrito.clear();
        txtVenta.clear();
        txtCliente.clear();
        actualizarResumen();
    }

    // =========================================================
    // AGREGAR PRODUCTOS
    // =========================================================

    @FXML
    private void agregarProducto() {
        if (ventaSeleccionada == null) {
            mostrarAlerta("Venta no seleccionada",
                    "Primero debes buscar una venta.");
            return;
        }

        ProductoVentaDevolucion producto =
                tableProductosVenta.getSelectionModel().getSelectedItem();

        if (producto == null) {
            mostrarAlerta("Producto no seleccionado",
                    "Selecciona un producto de la venta original.");
            return;
        }

        if (txtCantidad.getText().trim().isEmpty()) {
            mostrarAlerta("Cantidad obligatoria",
                    "Escribe la cantidad que deseas devolver.");
            txtCantidad.requestFocus();
            return;
        }

        int cantidad;
        try {
            cantidad = Integer.parseInt(txtCantidad.getText().trim());
        } catch (NumberFormatException e) {
            mostrarAlerta("Cantidad inválida",
                    "Ingresa una cantidad entera válida.");
            return;
        }

        if (cantidad <= 0) {
            mostrarAlerta("Cantidad inválida",
                    "La cantidad debe ser mayor que cero.");
            return;
        }

        String motivo = txtMotivo.getText().trim();
        if (motivo.isEmpty()) {
            mostrarAlerta("Motivo obligatorio",
                    "Escribe el motivo de devolución del producto.");
            txtMotivo.requestFocus();
            return;
        }

        if (motivo.length() > 100) {
            mostrarAlerta("Motivo demasiado largo",
                    "El motivo no puede superar los 100 caracteres.");
            return;
        }

        int cantidadEnCarrito = carrito.stream()
                .filter(d -> d.getIdProducto() == producto.getIdProducto())
                .mapToInt(DetalleDevolucionCliente::getCantidad)
                .sum();

        int restante = producto.getCantidadDisponible() - cantidadEnCarrito;

        if (cantidad > restante) {
            mostrarAlerta("Cantidad excedida",
                    "Solo puedes agregar " + restante
                            + " unidades adicionales de este producto.");
            return;
        }

        if (producto.getPrecioUnitario() == null
                || producto.getPrecioUnitario().signum() <= 0) {
            mostrarAlerta("Precio inválido",
                    "El producto no tiene un precio de venta válido.");
            return;
        }

        DetalleDevolucionCliente detalle = new DetalleDevolucionCliente(
                producto.getIdProducto(),
                producto.getNombreProducto(),
                cantidad,
                producto.getPrecioUnitario(),
                motivo);

        carrito.add(detalle);
        txtCantidad.setText("1");
        txtMotivo.clear();

        tableProductosVenta.refresh();
        tableCarrito.getSelectionModel().select(detalle);
        tableCarrito.scrollTo(detalle);
        actualizarResumen();
    }

    // =========================================================
    // RESUMEN
    // =========================================================

    private void actualizarResumen() {
        int articulos = 0;
        for (DetalleDevolucionCliente detalle : carrito) {
            articulos += detalle.getCantidad();
        }

        int productos = carrito.size();
        lblNumProductos.setText(String.valueOf(productos));
        lblArticulos.setText(String.valueOf(articulos));
        lblTotal.setText(Formato.dinero(calcularTotal()));

        boolean valido = ventaSeleccionada != null
                && !carrito.isEmpty()
                && Sesion.haySesion()
                && calcularTotal().signum() > 0;

        btnRegistrar.setDisable(!valido);

        lblEstadoFooter.setText(productos
                + (productos == 1
                    ? " producto en la devolución"
                    : " productos en la devolución"));
    }

    private BigDecimal calcularTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (DetalleDevolucionCliente detalle : carrito) {
            total = total.add(detalle.getSubtotal());
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    // =========================================================
    // REGISTRAR DEVOLUCIÓN
    // =========================================================

    @FXML
    private void registrarDevolucion() {
        if (!Sesion.haySesion()) {
            mostrarAlerta("Sin sesión",
                    "Inicia sesión para registrar una devolución.");
            return;
        }

        if (ventaSeleccionada == null || carrito.isEmpty()) {
            mostrarAlerta("Devolución incompleta",
                    "Selecciona una venta y agrega al menos un producto.");
            return;
        }

        if (!confirmar(
                "Registrar devolución",
                "¿Deseas registrar esta devolución?\n\n"
                + "Total devuelto: " + Formato.dinero(calcularTotal())
                + "\n\nLas unidades se reintegrarán al inventario.")) {
            return;
        }

        DevolucionCliente devolucion = new DevolucionCliente();
        devolucion.setIdVenta(ventaSeleccionada.getIdVenta());
        devolucion.setIdEmpleado(Sesion.getIdEmpleado());
        devolucion.setFecha(LocalDate.now());
        devolucion.setEstado(Estados.ACTIVO);

        int idDevolucion = devolucionDAO.registrar(
                devolucion, new ArrayList<>(carrito));

        if (idDevolucion <= 0) {
            mostrarAlerta("No se pudo registrar la devolución",
                    devolucionDAO.getMensajeError());
            return;
        }

        mostrarInformacion(
                "Devolución registrada",
                "La devolución " + devolucion.getFolio()
                + " se registró correctamente.\n\n"
                + "El inventario fue actualizado.");

        volverAGestion(idDevolucion);
    }

    // =========================================================
    // CANCELAR Y REGRESAR A GESTIÓN
    // =========================================================

    @FXML
    private void cancelar() {
        popupVentas.hide();

        if (!carrito.isEmpty()
                && !confirmar(
                        "Cancelar operación",
                        "¿Deseas salir? Se descartarán los productos agregados.")) {
            return;
        }

        volverAGestion(0);
    }

    private void volverAGestion(int idDevolucion) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/laroca/vista/devoluciones-clientes.fxml"));

            Parent root = loader.load();

            Navegacion.mostrar(
                    (Stage) ventana(),
                    root,
                    "La Roca - Devoluciones de Clientes");

            if (idDevolucion > 0) {
                DevolucionClienteController gestion = loader.getController();
                gestion.seleccionarPorId(idDevolucion);
            }

        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta("Error",
                    "No se pudo abrir la gestión de devoluciones.");
        }
    }

    // =========================================================
    // VENTANA, CONFIRMACIONES Y ALERTAS
    // =========================================================

    private Window ventana() {
        return tableCarrito.getScene().getWindow();
    }

    private boolean confirmar(String titulo, String mensaje) {
        Alert alerta = new Alert(
                Alert.AlertType.CONFIRMATION,
                mensaje,
                ButtonType.YES,
                ButtonType.NO);

        alerta.initOwner(ventana());
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.showAndWait();

        return alerta.getResult() == ButtonType.YES;
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.WARNING);
        alerta.initOwner(ventana());
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    private void mostrarInformacion(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.initOwner(ventana());
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}

