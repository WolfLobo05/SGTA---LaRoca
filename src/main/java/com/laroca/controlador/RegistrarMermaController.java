package com.laroca.controlador;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.laroca.dao.MermaDAO;
import com.laroca.dao.ProductoDAO;
import com.laroca.modelo.DetalleMerma;
import com.laroca.modelo.Estados;
import com.laroca.modelo.Merma;
import com.laroca.modelo.Producto;
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
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.KeyCode;
import javafx.geometry.Side;
import javafx.stage.Stage;
import javafx.stage.Window;

public class RegistrarMermaController {

    private static final int MAX_SUGERENCIAS = 8;
    private static final int MAX_MOTIVO = 100;

    @FXML private Button btnCancelarMerma;
    @FXML private Label lblFolio;
    @FXML private Label lblFechaMerma;
    @FXML private TextField txtCodigo;
    @FXML private TextField txtCantidad;
    @FXML private TextField txtMotivo;
    @FXML private Button btnBuscar;
    @FXML private Button btnAgregar;
    @FXML private TableView<DetalleMerma> tableCarrito;
    @FXML private TableColumn<DetalleMerma, Void> colNumero;
    @FXML private TableColumn<DetalleMerma, String> colProducto;
    @FXML private TableColumn<DetalleMerma, Integer> colCantidad;
    @FXML private TableColumn<DetalleMerma, String> colPrecio;
    @FXML private TableColumn<DetalleMerma, String> colSubtotal;
    @FXML private TableColumn<DetalleMerma, String> colMotivo;
    @FXML private TableColumn<DetalleMerma, Void> colQuitar;
    @FXML private Label lblStock;
    @FXML private TextField txtEmpleado;
    @FXML private TextField txtFecha;
    @FXML private Label lblNumProductos;
    @FXML private Label lblArticulos;
    @FXML private Label lblTotal;
    @FXML private Button btnRegistrar;
    @FXML private Label lblEstadoFooter;

    private final MermaDAO mermaDAO = new MermaDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final ObservableList<DetalleMerma> carrito =
            FXCollections.observableArrayList();
    private final Map<Integer, Integer> existencias = new HashMap<>();
    private final ContextMenu menuSugerencias = new ContextMenu();

    private Producto productoElegido;
    private boolean actualizandoCodigo;

    @FXML
    public void initialize() {
        colNumero.setCellFactory(columna -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : String.valueOf(getIndex() + 1));
            }
        });

        colProducto.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().getNombreProducto()));
        colCantidad.setCellValueFactory(c ->
                new javafx.beans.property.ReadOnlyObjectWrapper<>(c.getValue().getCantidad()));
        colPrecio.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(Formato.dinero(c.getValue().getPrecio())));
        colSubtotal.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(Formato.dinero(c.getValue().getSubtotal())));
        colMotivo.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().getMotivo()));

        configurarEdicionCantidad();
        colQuitar.setCellFactory(columna -> new CeldaQuitar<>(carrito::remove));
        tableCarrito.setItems(carrito);
        tableCarrito.setPlaceholder(
                new Label("Busca un producto, indica el motivo y agrégalo a la merma"));

        tableCarrito.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE) {
                DetalleMerma linea = tableCarrito.getSelectionModel().getSelectedItem();
                if (linea != null) carrito.remove(linea);
            }
        });

        txtCantidad.setTextFormatter(new TextFormatter<String>(cambio ->
                cambio.getControlNewText().matches("([1-9]\\d{0,3})?")
                        ? cambio : null));
        txtMotivo.setTextFormatter(new TextFormatter<String>(cambio ->
                cambio.getControlNewText().length() <= MAX_MOTIVO ? cambio : null));

        carrito.addListener((ListChangeListener<DetalleMerma>) cambio -> actualizarResumen());
        tableCarrito.getSelectionModel().selectedItemProperty()
                .addListener((obs, anterior, actual) -> actualizarStock());

        configurarBusquedaDinamica();
        iniciarMerma();
        Navegacion.verificarSesion(tableCarrito);
    }

    private void configurarEdicionCantidad() {
        tableCarrito.setEditable(true);
        colCantidad.setEditable(true);
        colCantidad.setCellFactory(columna -> new TableCell<DetalleMerma, Integer>() {
            private final TextField editor = new TextField();
            {
                editor.setTextFormatter(new TextFormatter<String>(cambio ->
                        cambio.getControlNewText().matches("\\d{0,4}") ? cambio : null));
                editor.setOnAction(e -> confirmarCantidad());
                editor.setOnKeyPressed(e -> {
                    if (e.getCode() == KeyCode.ESCAPE) cancelEdit();
                });
                editor.focusedProperty().addListener((obs, antes, enfocado) -> {
                    if (!enfocado && isEditing()) confirmarCantidad();
                });
            }
            @Override public void startEdit() {
                if (isEmpty()) return;
                super.startEdit();
                editor.setText(getItem() == null ? "" : getItem().toString());
                setText(null);
                setGraphic(editor);
                editor.selectAll();
                editor.requestFocus();
            }
            @Override public void cancelEdit() {
                super.cancelEdit();
                setGraphic(null);
                setText(getItem() == null ? "" : getItem().toString());
            }
            @Override protected void updateItem(Integer cantidad, boolean empty) {
                super.updateItem(cantidad, empty);
                if (empty) {
                    setText(null); setGraphic(null);
                } else if (isEditing()) {
                    setText(null); setGraphic(editor);
                } else {
                    setText(cantidad == null ? "" : cantidad.toString());
                    setGraphic(null);
                }
            }
            private void confirmarCantidad() {
                if (!isEditing()) return;
                String texto = editor.getText().trim();
                if (texto.isEmpty()) {
                    mostrarAlerta("Cantidad inválida", "La cantidad debe ser mayor que cero.");
                    cancelEdit(); return;
                }
                int nueva;
                try { nueva = Integer.parseInt(texto); }
                catch (NumberFormatException ex) {
                    mostrarAlerta("Cantidad inválida", "Ingresa un número entero válido.");
                    cancelEdit(); return;
                }
                DetalleMerma linea = getTableView().getItems().get(getIndex());
                int acumulada = cantidadEnCarrito(linea.getIdProducto())
                        - linea.getCantidad() + nueva;
                int stock = existencias.getOrDefault(linea.getIdProducto(), 0);
                if (nueva <= 0 || acumulada > stock) {
                    mostrarAlerta("Stock insuficiente",
                            "La cantidad acumulada de este producto no puede superar las "
                                    + stock + " unidades disponibles.");
                    cancelEdit(); return;
                }
                linea.setCantidad(nueva);
                commitEdit(nueva);
                tableCarrito.refresh();
                actualizarResumen();
            }
        });
    }

    private void configurarBusquedaDinamica() {
        txtCodigo.textProperty().addListener((obs, anterior, actual) -> {
            if (actualizandoCodigo) return;
            productoElegido = null;
            mostrarSugerencias(actual);
        });
        txtCodigo.setOnAction(event -> {
            if (menuSugerencias.isShowing() && !menuSugerencias.getItems().isEmpty()) {
                menuSugerencias.getItems().get(0).fire();
                event.consume();
            } else agregarProducto();
        });
        txtCodigo.focusedProperty().addListener((obs, anterior, enfocado) -> {
            if (!enfocado) Platform.runLater(menuSugerencias::hide);
        });
    }

    private void mostrarSugerencias(String texto) {
        menuSugerencias.hide();
        menuSugerencias.getItems().clear();
        String filtro = texto == null ? "" : texto.trim();
        if (filtro.isEmpty()) return;
        List<Producto> resultados;
        try { resultados = productoDAO.buscarActivos(filtro); }
        catch (Exception e) { e.printStackTrace(); return; }
        if (resultados == null || resultados.isEmpty()) return;
        int limite = Math.min(resultados.size(), MAX_SUGERENCIAS);
        for (int i = 0; i < limite; i++) {
            Producto producto = resultados.get(i);
            MenuItem sugerencia = new MenuItem(
                    producto.getNombre() + " | Código: "
                    + Objects.toString(producto.getCodigoBarras(), "Sin código")
                    + " | Stock: " + producto.getExistencia());
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
        if (producto == null) return;
        menuSugerencias.hide();
        productoElegido = producto;
        actualizandoCodigo = true;
        txtCodigo.setText(textoProducto(producto));
        actualizandoCodigo = false;
        txtCantidad.requestFocus();
        txtCantidad.selectAll();
    }

    private void iniciarMerma() {
        int siguiente = mermaDAO.obtenerSiguienteId();
        lblFolio.setText(siguiente > 0 ? String.format("M-%03d", siguiente) : "—");
        String hoy = Formato.fecha(LocalDate.now());
        lblFechaMerma.setText("·  " + hoy);
        txtFecha.setText(hoy);
        txtEmpleado.setText(Sesion.haySesion() ? Sesion.getUsuario() : "Sin sesión iniciada");
        actualizarResumen();
        Platform.runLater(() -> txtCodigo.requestFocus());
    }

    @FXML
    private void agregarProducto() {
        String texto = txtCodigo.getText() == null ? "" : txtCodigo.getText().trim();
        String motivo = txtMotivo.getText() == null ? "" : txtMotivo.getText().trim();
        if (txtCantidad.getText().isEmpty()) {
            mostrarAlerta("Cantidad obligatoria", "Escribe una cantidad mayor que cero.");
            txtCantidad.requestFocus(); return;
        }
        if (motivo.isEmpty()) {
            mostrarAlerta("Motivo obligatorio", "Indica el motivo de la merma de este producto.");
            txtMotivo.requestFocus(); return;
        }
        int cantidad = Integer.parseInt(txtCantidad.getText());
        Producto producto;
        if (productoElegido != null && texto.equals(textoProducto(productoElegido))) {
            producto = productoElegido;
        } else if (texto.isEmpty()) {
            producto = BuscadorProductos.buscar(ventana(), "");
        } else {
            producto = productoDAO.buscarActivoPorCodigo(texto);
            if (producto == null) producto = BuscadorProductos.buscar(ventana(), texto);
        }
        if (producto == null) return;
        if (agregarAlCarrito(producto, cantidad, motivo)) {
            menuSugerencias.hide();
            txtCodigo.clear();
            txtCantidad.setText("1");
            txtMotivo.clear();
            productoElegido = null;
            txtCodigo.requestFocus();
        }
    }

    @FXML
    private void buscarProducto() {
        String texto = txtCodigo.getText() == null ? "" : txtCodigo.getText().trim();
        if (productoElegido != null && texto.equals(textoProducto(productoElegido))) texto = "";
        Producto producto = BuscadorProductos.buscar(ventana(), texto);
        if (producto != null) seleccionarSugerencia(producto);
    }

    private String textoProducto(Producto producto) {
        return Objects.toString(producto.getCodigoBarras(), "") + " — " + producto.getNombre();
    }

    private boolean agregarAlCarrito(Producto producto, int cantidad, String motivo) {
        if (cantidad <= 0) {
            mostrarAlerta("Cantidad inválida", "La cantidad debe ser mayor que cero.");
            return false;
        }
        if (motivo == null || motivo.isBlank() || motivo.length() > MAX_MOTIVO) {
            mostrarAlerta("Motivo inválido", "El motivo es obligatorio y admite hasta 100 caracteres.");
            return false;
        }
        BigDecimal precio = mermaDAO.obtenerPrecioCompra(producto.getIdProducto());
        if (precio == null || precio.compareTo(BigDecimal.ZERO) <= 0) {
            mostrarAlerta("Sin historial de compra",
                    "No se puede agregar \"" + producto.getNombre()
                    + "\" porque no tiene un último precio de compra válido registrado.");
            return false;
        }
        existencias.put(producto.getIdProducto(), producto.getExistencia());
        int acumulada = cantidadEnCarrito(producto.getIdProducto()) + cantidad;
        if (acumulada > producto.getExistencia()) {
            mostrarAlerta("Stock insuficiente", "Solo hay " + producto.getExistencia()
                    + " unidades de \"" + producto.getNombre()
                    + "\" y ya tienes " + (acumulada - cantidad) + " en la merma.");
            return false;
        }
        carrito.add(new DetalleMerma(producto.getIdProducto(), producto.getNombre(),
                cantidad, precio, motivo));
        DetalleMerma agregada = carrito.get(carrito.size() - 1);
        tableCarrito.getSelectionModel().select(agregada);
        tableCarrito.scrollTo(agregada);
        return true;
    }

    private int cantidadEnCarrito(int idProducto) {
        int total = 0;
        for (DetalleMerma linea : carrito) {
            if (linea.getIdProducto() == idProducto) total += linea.getCantidad();
        }
        return total;
    }

    private boolean datosMermaValidos() {
        if (!Sesion.haySesion() || carrito.isEmpty()) return false;
        for (DetalleMerma linea : carrito) {
            if (linea.getCantidad() <= 0 || linea.getPrecio() == null
                    || linea.getPrecio().compareTo(BigDecimal.ZERO) <= 0
                    || linea.getMotivo() == null || linea.getMotivo().isBlank()
                    || linea.getMotivo().length() > MAX_MOTIVO
                    || linea.getCantidad() > existencias.getOrDefault(linea.getIdProducto(), 0)) {
                return false;
            }
        }
        return calcularTotal().compareTo(BigDecimal.ZERO) > 0;
    }

    private void actualizarResumen() {
        int articulos = 0;
        Set<Integer> productos = new HashSet<>();
        for (DetalleMerma linea : carrito) {
            articulos += linea.getCantidad();
            productos.add(linea.getIdProducto());
        }
        lblNumProductos.setText(String.valueOf(productos.size()));
        lblArticulos.setText(String.valueOf(articulos));
        lblTotal.setText(Formato.dinero(calcularTotal()));
        btnRegistrar.setDisable(!datosMermaValidos());
        lblEstadoFooter.setText(productos.size() + (productos.size() == 1
                ? " producto en la merma" : " productos en la merma"));
        actualizarStock();
    }

    private void actualizarStock() {
        DetalleMerma linea = tableCarrito.getSelectionModel().getSelectedItem();
        if (linea == null) {
            lblStock.setText("Stock restante del producto seleccionado: —");
            return;
        }
        int stock = existencias.getOrDefault(linea.getIdProducto(), 0);
        lblStock.setText("Stock restante del producto seleccionado: "
                + (stock - cantidadEnCarrito(linea.getIdProducto())));
    }

    private BigDecimal calcularTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (DetalleMerma linea : carrito) total = total.add(linea.getSubtotal());
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    @FXML
    private void registrarMerma() {
        if (!Sesion.haySesion()) {
            mostrarAlerta("Sin sesión", "Inicia sesión para registrar una merma.");
            return;
        }
        if (!datosMermaValidos()) {
            mostrarAlerta("Merma incompleta",
                    "Verifica los productos, las cantidades, los motivos y las existencias.");
            return;
        }
        if (!confirmar("Registrar merma", "Se descontarán "
                + lblArticulos.getText() + " artículos del inventario. ¿Deseas continuar?")) return;

        Merma merma = new Merma();
        merma.setIdEmpleado(Sesion.getIdEmpleado());
        merma.setFecha(LocalDate.now());
        merma.setEstado(Estados.ACTIVO);
        int idMerma = mermaDAO.registrar(merma, List.copyOf(carrito));
        if (idMerma <= 0) {
            mostrarAlerta("No se pudo registrar la merma",
                    "La operación no se completó. Verifica las existencias y la conexión con la base de datos.");
            return;
        }
        mostrarInformacion("Merma registrada", "La merma "
                + String.format("M-%03d", idMerma)
                + " se registró correctamente y el inventario fue actualizado.");
        volverAGestion();
    }

    @FXML
    private void cancelarMerma() {
        if (!carrito.isEmpty() && !confirmar("Cancelar merma",
                "¿Deseas cancelar el registro? Se descartarán los productos agregados.")) return;
        volverAGestion();
    }

    private void volverAGestion() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/laroca/vista/mermas.fxml"));
            Parent root = loader.load();
            Navegacion.mostrar((Stage) ventana(), root, "La Roca - Mermas");
        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir la gestión de mermas.");
        }
    }

    private Window ventana() {
        return tableCarrito.getScene().getWindow();
    }

    private boolean confirmar(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, mensaje,
                ButtonType.YES, ButtonType.NO);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        return alerta.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.WARNING);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    private void mostrarInformacion(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
