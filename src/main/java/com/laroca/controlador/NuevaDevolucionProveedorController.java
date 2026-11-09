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

import com.laroca.dao.DevolucionProveedorDAO;
import com.laroca.modelo.Compra;
import com.laroca.modelo.DevolucionProveedor;
import com.laroca.modelo.DetalleDevolucionProveedor;
import com.laroca.modelo.Estados;
import com.laroca.modelo.ProductoCompraDevolucion;
import com.laroca.modelo.Sesion;

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

public class NuevaDevolucionProveedorController {

    // =========================================================
    // COMPONENTES FXML
    // =========================================================

    @FXML private Button btnNuevaDevolucion;
    @FXML private Button btnCancelar;
    @FXML private Label lblFolio;
    @FXML private Label lblFecha;

    @FXML private TextField txtFolioCompra;
    @FXML private Button btnBuscarCompra;

    @FXML private TableView<ProductoCompraDevolucion> tableProductosCompra;
    @FXML private TableColumn<ProductoCompraDevolucion, String> colProducto;
    @FXML private TableColumn<ProductoCompraDevolucion, Integer> colComprados;
    @FXML private TableColumn<ProductoCompraDevolucion, Integer> colDevueltos;
    @FXML private TableColumn<ProductoCompraDevolucion, Integer> colDisponibles;
    @FXML private TableColumn<ProductoCompraDevolucion, String> colPrecioCompra;

    @FXML private TextField txtCantidad;
    @FXML private TextField txtMotivo;
    @FXML private Button btnAgregar;

    @FXML private TableView<DetalleDevolucionProveedor> tableCarrito;
    @FXML private TableColumn<DetalleDevolucionProveedor, String> colCarritoProducto;
    @FXML private TableColumn<DetalleDevolucionProveedor, Integer> colCarritoCantidad;
    @FXML private TableColumn<DetalleDevolucionProveedor, String> colCarritoPrecio;
    @FXML private TableColumn<DetalleDevolucionProveedor, String> colCarritoSubtotal;
    @FXML private TableColumn<DetalleDevolucionProveedor, String> colCarritoMotivo;
    @FXML private TableColumn<DetalleDevolucionProveedor, Void> colQuitar;

    @FXML private TextField txtEmpleado;
    @FXML private TextField txtProveedor;
    @FXML private TextField txtFecha;
    @FXML private TextField txtCompra;

    @FXML private Label lblNumProductos;
    @FXML private Label lblArticulos;
    @FXML private Label lblTotal;
    @FXML private Button btnRegistrar;
    @FXML private Label lblEstadoFooter;

    // =========================================================
    // DAO, LISTAS Y BUSCADOR
    // =========================================================

    private final DevolucionProveedorDAO devolucionDAO =
            new DevolucionProveedorDAO();

    private final ObservableList<ProductoCompraDevolucion> productosCompra =
            FXCollections.observableArrayList();

    private final ObservableList<DetalleDevolucionProveedor> carrito =
            FXCollections.observableArrayList();

    private Compra compraSeleccionada;

    private final Popup popupCompras = new Popup();
    private final ListView<Compra> listaSugerencias = new ListView<>();
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

        tableProductosCompra.setItems(productosCompra);
        tableCarrito.setItems(carrito);

        configurarBuscadorCompras();

        tableProductosCompra.setPlaceholder(
                new Label("Busca una compra para consultar sus productos."));

        tableCarrito.setPlaceholder(
                new Label("Agrega los productos que deseas devolver al proveedor."));

        txtCantidad.setTextFormatter(
                new TextFormatter<String>(cambio ->
                        cambio.getControlNewText().matches("\\d{0,4}")
                                ? cambio : null));

        txtMotivo.setTextFormatter(
                new TextFormatter<String>(cambio ->
                        cambio.getControlNewText().length() <= 100
                                ? cambio : null));

        txtEmpleado.setText(
                Sesion.haySesion() ? Sesion.getUsuario() : "Sin sesión iniciada");

        String hoy = Formato.fecha(LocalDate.now());
        txtFecha.setText(hoy);
        lblFecha.setText("·  " + hoy);

        int siguienteId = devolucionDAO.obtenerSiguienteId();
        lblFolio.setText(siguienteId > 0
                ? String.format("DP-%03d", siguienteId) : "—");

        carrito.addListener(
                (ListChangeListener<DetalleDevolucionProveedor>)
                        cambio -> actualizarResumen());

        tableProductosCompra.getSelectionModel()
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
        colProducto.setCellValueFactory(
                new PropertyValueFactory<>("nombreProducto"));
        colComprados.setCellValueFactory(
                new PropertyValueFactory<>("cantidadComprada"));
        colDevueltos.setCellValueFactory(
                new PropertyValueFactory<>("cantidadDevuelta"));
        colDisponibles.setCellValueFactory(
                new PropertyValueFactory<>("cantidadDisponible"));

        colPrecioCompra.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        Formato.dinero(c.getValue().getPrecioCompra())));
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
                DetalleDevolucionProveedor seleccionado =
                        tableCarrito.getSelectionModel().getSelectedItem();
                if (seleccionado != null) carrito.remove(seleccionado);
            }
        });
    }

    // =========================================================
    // BUSCADOR DE COMPRAS CON SUGERENCIAS
    // =========================================================

    private void configurarBuscadorCompras() {
        listaSugerencias.setPrefHeight(180);
        listaSugerencias.setCellFactory(lista -> new ListCell<>() {
            @Override
            protected void updateItem(Compra compra, boolean vacio) {
                super.updateItem(compra, vacio);

                if (vacio || compra == null) {
                    setText(null);
                    return;
                }

                String folio = compra.getFolio();
                String proveedor = compra.getNombreProveedor() == null
                        ? "Proveedor sin nombre" : compra.getNombreProveedor();
                String fecha = compra.getFecha() == null
                        ? "Sin fecha"
                        : compra.getFecha().format(FORMATO_FECHA);

                setText(folio + "  |  " + proveedor + "  |  " + fecha);
            }
        });

        popupCompras.getContent().setAll(listaSugerencias);
        popupCompras.setAutoHide(true);
        popupCompras.setAutoFix(true);
        popupCompras.setHideOnEscape(true);

        txtFolioCompra.textProperty().addListener((obs, anterior, actual) -> {
            if (seleccionandoSugerencia) return;

            if (compraSeleccionada != null) {
                String folioCargado = compraSeleccionada.getFolio();
                if (!folioCargado.equalsIgnoreCase(actual.trim())) {
                    limpiarCompra();
                }
            }

            actualizarSugerencias(actual);
        });

        listaSugerencias.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                Compra seleccionada = listaSugerencias
                        .getSelectionModel().getSelectedItem();
                if (seleccionada != null) seleccionarCompraSugerida(seleccionada);
            }
        });

        listaSugerencias.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                Compra seleccionada = listaSugerencias
                        .getSelectionModel().getSelectedItem();
                if (seleccionada != null) seleccionarCompraSugerida(seleccionada);
            }
        });

        txtFolioCompra.focusedProperty().addListener((obs, anterior, enfocado) -> {
            if (!enfocado) {
                Platform.runLater(() -> {
                    if (!listaSugerencias.isFocused()) popupCompras.hide();
                });
            }
        });
    }

    private void actualizarSugerencias(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            popupCompras.hide();
            listaSugerencias.getItems().clear();
            return;
        }

        List<Compra> resultados = devolucionDAO.buscarComprasActivas(texto);
        listaSugerencias.getItems().setAll(resultados);

        if (resultados.isEmpty()) {
            popupCompras.hide();
            return;
        }

        Bounds limites = txtFolioCompra.localToScreen(
                txtFolioCompra.getBoundsInLocal());

        if (limites == null || txtFolioCompra.getScene() == null) return;

        listaSugerencias.setPrefWidth(
                Math.max(txtFolioCompra.getWidth(), 320));

        if (!popupCompras.isShowing()) {
            popupCompras.show(
                    txtFolioCompra.getScene().getWindow(),
                    limites.getMinX(),
                    limites.getMaxY());
        } else {
            popupCompras.setX(limites.getMinX());
            popupCompras.setY(limites.getMaxY());
        }
    }

    private void seleccionarCompraSugerida(Compra compra) {
        if (compra == null) return;

        popupCompras.hide();
        seleccionandoSugerencia = true;
        txtFolioCompra.setText(compra.getFolio());
        seleccionandoSugerencia = false;

        cargarCompraSeleccionada(compra);
    }

    // =========================================================
    // BUSCAR Y CARGAR COMPRA
    // =========================================================

    @FXML
    private void buscarCompra() {
        popupCompras.hide();

        String folio = txtFolioCompra.getText().trim().toUpperCase();

        if (folio.isEmpty()) {
            mostrarAlerta("Folio obligatorio",
                    "Escribe el folio de la compra que deseas consultar.");
            return;
        }

        Matcher matcher = Pattern.compile("^(?:C-)?(\\d+)$").matcher(folio);
        if (!matcher.matches()) {
            mostrarAlerta("Compra no seleccionada",
                    "Escribe un folio válido o selecciona una compra de las sugerencias.");
            return;
        }

        int idCompra;
        try {
            idCompra = Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException e) {
            mostrarAlerta("Folio inválido", "El número de compra no es válido.");
            return;
        }

        List<Compra> resultados = devolucionDAO.buscarComprasActivas(
                String.format("C-%03d", idCompra));

        Compra compra = resultados.stream()
                .filter(c -> c.getIdCompra() == idCompra)
                .findFirst()
                .orElse(null);

        if (compra == null) {
            limpiarCompra();
            mostrarAlerta("Compra no encontrada",
                    "No existe una compra activa con ese folio.");
            return;
        }

        cargarCompraSeleccionada(compra);
    }

    private void cargarCompraSeleccionada(Compra compra) {
        if (compra == null) return;

        if (!Estados.esActivo(compra.getEstado())) {
            limpiarCompra();
            mostrarAlerta("Compra cancelada",
                    "No se pueden registrar devoluciones de una compra cancelada.");
            return;
        }

        List<ProductoCompraDevolucion> productos =
                devolucionDAO.listarProductosCompra(compra.getIdCompra());

        if (productos == null || productos.isEmpty()) {
            limpiarCompra();
            mostrarAlerta("Sin productos disponibles",
                    "Esta compra no tiene productos pendientes de devolución.");
            return;
        }

        compraSeleccionada = compra;
        productosCompra.setAll(productos);
        carrito.clear();

        txtCompra.setText(compra.getFolio());
        txtProveedor.setText(compra.getNombreProveedor());

        tableProductosCompra.refresh();
        actualizarResumen();

        lblEstadoFooter.setText(
                productos.size() + " productos disponibles en la compra");
    }

    private void limpiarCompra() {
        compraSeleccionada = null;
        productosCompra.clear();
        carrito.clear();
        txtCompra.clear();
        txtProveedor.clear();
        actualizarResumen();
    }

    // =========================================================
    // AGREGAR PRODUCTOS
    // =========================================================

    @FXML
    private void agregarProducto() {
        if (compraSeleccionada == null) {
            mostrarAlerta("Compra no seleccionada",
                    "Primero debes buscar una compra.");
            return;
        }

        ProductoCompraDevolucion producto =
                tableProductosCompra.getSelectionModel().getSelectedItem();

        if (producto == null) {
            mostrarAlerta("Producto no seleccionado",
                    "Selecciona un producto de la compra original.");
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
                .mapToInt(DetalleDevolucionProveedor::getCantidad)
                .sum();

        int restante = producto.getCantidadDisponible() - cantidadEnCarrito;

        if (cantidad > restante) {
            mostrarAlerta("Cantidad excedida",
                    "Solo puedes agregar " + restante
                            + " unidades adicionales de este producto.");
            return;
        }

        if (producto.getPrecioCompra() == null
                || producto.getPrecioCompra().signum() < 0) {
            mostrarAlerta("Precio inválido",
                    "El producto no tiene un precio de compra válido.");
            return;
        }

        DetalleDevolucionProveedor detalle =
                new DetalleDevolucionProveedor(
                        producto.getIdProducto(),
                        producto.getNombreProducto(),
                        cantidad,
                        producto.getPrecioCompra(),
                        motivo);

        carrito.add(detalle);
        txtCantidad.setText("1");
        txtMotivo.clear();

        tableProductosCompra.refresh();
        tableCarrito.getSelectionModel().select(detalle);
        tableCarrito.scrollTo(detalle);
        actualizarResumen();
    }

    // =========================================================
    // RESUMEN
    // =========================================================

    private void actualizarResumen() {
        int articulos = 0;
        for (DetalleDevolucionProveedor detalle : carrito) {
            articulos += detalle.getCantidad();
        }

        int productos = carrito.size();
        lblNumProductos.setText(String.valueOf(productos));
        lblArticulos.setText(String.valueOf(articulos));
        lblTotal.setText(Formato.dinero(calcularTotal()));

        boolean valido = compraSeleccionada != null
                && !carrito.isEmpty()
                && Sesion.haySesion()
                && calcularTotal().signum() >= 0;

        btnRegistrar.setDisable(!valido);

        lblEstadoFooter.setText(productos
                + (productos == 1
                    ? " producto en la devolución"
                    : " productos en la devolución"));
    }

    private BigDecimal calcularTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (DetalleDevolucionProveedor detalle : carrito) {
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

        if (compraSeleccionada == null || carrito.isEmpty()) {
            mostrarAlerta("Devolución incompleta",
                    "Selecciona una compra y agrega al menos un producto.");
            return;
        }

        if (!confirmar(
                "Registrar devolución",
                "¿Deseas registrar esta devolución al proveedor?\n\n"
                + "Total de la devolución: " + Formato.dinero(calcularTotal())
                + "\n\nLas unidades se descontarán del inventario.")) {
            return;
        }

        DevolucionProveedor devolucion = new DevolucionProveedor();
        devolucion.setIdCompra(compraSeleccionada.getIdCompra());
        devolucion.setIdEmpleado(Sesion.getIdEmpleado());
        devolucion.setFecha(LocalDate.now());
        devolucion.setEstado(Estados.ACTIVO);

        int idDevolucion = devolucionDAO.registrar(
                devolucion, new ArrayList<>(carrito));

        if (idDevolucion <= 0) {
            mostrarAlerta("No se pudo registrar la devolución",
                    devolucionDAO.getMensajeError() == null
                            ? "Ocurrió un error al guardar la devolución."
                            : devolucionDAO.getMensajeError());
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
        popupCompras.hide();

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
                            "/com/laroca/vista/devoluciones-proveedores.fxml"));

            Parent root = loader.load();

            Navegacion.mostrar(
                    (Stage) ventana(),
                    root,
                    "La Roca - Devoluciones a Proveedores");

            if (idDevolucion > 0) {
                DevolucionProveedorController gestion = loader.getController();
                gestion.seleccionarPorId(idDevolucion);
            }

        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
            mostrarAlerta("Error",
                    "La devolución se registró, pero no se pudo abrir la gestión. "
                    + "Verifica la ruta del FXML y el controlador de devoluciones.");
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
