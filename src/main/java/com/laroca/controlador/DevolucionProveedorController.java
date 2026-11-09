package com.laroca.controlador;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.laroca.dao.DevolucionProveedorDAO;
import com.laroca.modelo.DevolucionProveedor;
import com.laroca.modelo.DetalleDevolucionProveedor;
import com.laroca.modelo.Estados;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
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
import javafx.stage.Stage;

public class DevolucionProveedorController {

    // =====================================================
    // COMPONENTES FXML
    // =====================================================

    @FXML private Button btnNuevo;
    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;
    @FXML private Button btnVolverMenu;

    @FXML private CheckBox chkMostrarCanceladas;
    @FXML private Label lblFechaActual;

    @FXML private TableView<DevolucionProveedor> tableDevoluciones;

    @FXML private TableColumn<DevolucionProveedor, Integer> colId;
    @FXML private TableColumn<DevolucionProveedor, String> colFolio;
    @FXML private TableColumn<DevolucionProveedor, String> colFolioCompra;
    @FXML private TableColumn<DevolucionProveedor, String> colProveedor;
    @FXML private TableColumn<DevolucionProveedor, String> colEmpleado;
    @FXML private TableColumn<DevolucionProveedor, String> colFecha;
    @FXML private TableColumn<DevolucionProveedor, String> colEstado;

    @FXML private TextField txtFolio;
    @FXML private TextField txtFolioCompra;
    @FXML private TextField txtProveedor;
    @FXML private TextField txtEmpleado;
    @FXML private TextField txtFecha;
    @FXML private TextField txtEstado;

    @FXML private TableView<DetalleDevolucionProveedor> tableDetalle;

    @FXML private TableColumn<DetalleDevolucionProveedor, String> colProducto;
    @FXML private TableColumn<DetalleDevolucionProveedor, Integer> colCantidad;
    @FXML private TableColumn<DetalleDevolucionProveedor, BigDecimal> colPrecio;
    @FXML private TableColumn<DetalleDevolucionProveedor, BigDecimal> colSubtotal;
    @FXML private TableColumn<DetalleDevolucionProveedor, String> colMotivo;

    @FXML private Label lblTotal;
    @FXML private Label lblEstadoFooter;

    // =====================================================
    // DATOS
    // =====================================================

    private final DevolucionProveedorDAO dao =
            new DevolucionProveedorDAO();

    private final ObservableList<DevolucionProveedor> devoluciones =
            FXCollections.observableArrayList();

    private final ObservableList<DetalleDevolucionProveedor> detalles =
            FXCollections.observableArrayList();

    private final NumberFormat formatoDinero =
            NumberFormat.getCurrencyInstance(
                    Locale.forLanguageTag("es-MX"));

    // =====================================================
    // INICIALIZACIÓN
    // =====================================================

    @FXML
    private void initialize() {

        configurarTabla();

        tableDevoluciones.setItems(devoluciones);
        tableDetalle.setItems(detalles);

        lblFechaActual.setText(
                LocalDate.now().format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        lblTotal.setText(
                formatoDinero.format(BigDecimal.ZERO));

        btnEditar.setDisable(true);
        btnEliminar.setDisable(true);

        tableDevoluciones.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, anterior, actual) -> {

                    mostrarDetalle(actual);
                    actualizarBotonCancelar(actual);
                });

        cargarDevoluciones();
    }

    // =====================================================
    // CONFIGURACIÓN DE TABLAS
    // =====================================================

    private void configurarTabla() {

        colId.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(
                        c.getValue().getIdDevolucionProveedor()));

        colFolio.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        c.getValue().getFolio()));

        colFolioCompra.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        Objects.toString(
                                c.getValue().getFolioCompra(), "")));

        colProveedor.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        Objects.toString(
                                c.getValue().getNombreProveedor(), "")));

        colEmpleado.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        Objects.toString(
                                c.getValue().getNombreEmpleado(), "")));

        colFecha.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        Formato.fecha(c.getValue().getFecha())));

        // Columna de estado: Activa o Cancelada.
        colEstado.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        Estados.esActivo(c.getValue().getEstado())
                                ? "Activa"
                                : "Cancelada"));

        colProducto.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        c.getValue().getNombreProducto()));

        colCantidad.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(
                        c.getValue().getCantidad()));

        colPrecio.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(
                        c.getValue().getPrecio()));

        colSubtotal.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(
                        c.getValue().getSubtotal()));

        colMotivo.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        Objects.toString(
                                c.getValue().getMotivo(), "")));
    }

    // =====================================================
    // ACTUALIZAR BOTÓN CANCELAR
    // =====================================================

    private void actualizarBotonCancelar(
            DevolucionProveedor devolucion) {

        boolean puedeCancelar =
                devolucion != null
                && Estados.esActivo(devolucion.getEstado());

        btnEliminar.setDisable(!puedeCancelar);
    }

    // =====================================================
    // CARGAR DEVOLUCIONES
    // =====================================================

    private void cargarDevoluciones() {

        try {

            Integer idSeleccionado = null;

            DevolucionProveedor seleccionada =
                    tableDevoluciones.getSelectionModel()
                            .getSelectedItem();

            if (seleccionada != null) {
                idSeleccionado =
                        seleccionada.getIdDevolucionProveedor();
            }

            List<DevolucionProveedor> lista = dao.listar();

            boolean mostrarCanceladas =
                    chkMostrarCanceladas.isSelected();

            // Desmarcado: solamente activas.
            // Marcado: solamente canceladas.
            devoluciones.setAll(
                    lista.stream()
                            .filter(d ->
                                    Estados.esActivo(d.getEstado())
                                            != mostrarCanceladas)
                            .toList()
            );

            boolean encontrada = false;

            // Conservar selección cuando el registro sigue
            // perteneciendo al filtro actual.
            if (idSeleccionado != null) {

                for (DevolucionProveedor d : devoluciones) {

                    if (d.getIdDevolucionProveedor()
                            == idSeleccionado) {

                        tableDevoluciones.getSelectionModel()
                                .select(d);

                        tableDevoluciones.scrollTo(d);

                        encontrada = true;
                        break;
                    }
                }
            }

            if (!encontrada) {

                tableDevoluciones.getSelectionModel()
                        .clearSelection();

                if (!devoluciones.isEmpty()) {
                    tableDevoluciones.getSelectionModel()
                            .selectFirst();
                } else {
                    limpiarDetalle();
                }
            }

            actualizarBotonCancelar(
                    tableDevoluciones.getSelectionModel()
                            .getSelectedItem());

            actualizarPie();

        } catch (Exception e) {

            e.printStackTrace();

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error",
                    "No se pudieron cargar las devoluciones.\n"
                            + e.getMessage());
        }
    }

    private void actualizarPie() {

        boolean canceladas = chkMostrarCanceladas.isSelected();

        int total = devoluciones.size();

        lblEstadoFooter.setText(
                total + (total == 1
                        ? (canceladas
                                ? " devolución cancelada"
                                : " devolución registrada")
                        : (canceladas
                                ? " devoluciones canceladas"
                                : " devoluciones registradas")));
    }

    @FXML
    private void cambiarMostrarCanceladas() {

        tableDevoluciones.getSelectionModel()
                .clearSelection();

        cargarDevoluciones();
    }

    // =====================================================
    // NUEVA DEVOLUCIÓN
    // =====================================================

    @FXML
    private void nuevaDevolucion() {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/laroca/vista/nueva-devolucion-proveedor.fxml"));

            Parent root = loader.load();

            Stage ventana = (Stage) btnNuevo
                    .getScene()
                    .getWindow();

            Navegacion.mostrar(
                    ventana,
                    root,
                    "La Roca - Nueva Devolución a Proveedor");

        } catch (IOException | IllegalStateException e) {

            e.printStackTrace();

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error de navegación",
                    "No se pudo abrir el registro de devolución a proveedor.\n"
                            + e.getMessage());
        }
    }

    // =====================================================
    // CANCELAR DEVOLUCIÓN
    // =====================================================

    @FXML
    private void eliminarDevolucion() {

        DevolucionProveedor seleccionada =
                tableDevoluciones.getSelectionModel()
                        .getSelectedItem();

        if (seleccionada == null) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Selecciona una devolución",
                    "Selecciona una devolución para cancelarla.");

            return;
        }

        if (!Estados.esActivo(seleccionada.getEstado())) {

            mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Devolución cancelada",
                    "La devolución seleccionada ya está cancelada.");

            return;
        }

        Alert confirmacion = new Alert(
                Alert.AlertType.CONFIRMATION,
                "¿Deseas cancelar la devolución "
                        + seleccionada.getFolio()
                        + "?\n\n"
                        + "Se restaurarán las existencias al inventario.",
                ButtonType.YES,
                ButtonType.NO);

        confirmacion.setTitle("Confirmar cancelación");
        confirmacion.setHeaderText(null);

        if (confirmacion.showAndWait()
                .orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }

        try {

            int id = seleccionada.getIdDevolucionProveedor();

            if (dao.cancelar(id)) {

                // Actualizar el estado local del registro.
                seleccionada.setEstado(Estados.INACTIVO);

                // Recargar desde la base de datos.
                cargarDevoluciones();

                mostrarAlerta(
                        Alert.AlertType.INFORMATION,
                        "Devolución cancelada",
                        "La devolución "
                                + seleccionada.getFolio()
                                + " se canceló correctamente.\n\n"
                                + "Las existencias fueron restauradas.");

            } else {

                String mensaje = dao.getMensajeError();

                mostrarAlerta(
                        Alert.AlertType.ERROR,
                        "No se pudo cancelar",
                        mensaje == null || mensaje.isBlank()
                                ? "La devolución no pudo cancelarse."
                                : mensaje);
            }

        } catch (Exception e) {

            e.printStackTrace();

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error",
                    "Ocurrió un error al cancelar la devolución.\n"
                            + e.getMessage());
        }
    }

    // =====================================================
    // MOSTRAR DETALLE
    // =====================================================

    private void mostrarDetalle(
            DevolucionProveedor devolucion) {

        if (devolucion == null) {
            limpiarDetalle();
            return;
        }

        txtFolio.setText(
                devolucion.getFolio());

        txtFolioCompra.setText(
                devolucion.getFolioCompra());

        txtProveedor.setText(
                Objects.toString(
                        devolucion.getNombreProveedor(), ""));

        txtEmpleado.setText(
                Objects.toString(
                        devolucion.getNombreEmpleado(), ""));

        txtFecha.setText(
                Formato.fecha(devolucion.getFecha()));

        txtEstado.setText(
                Estados.esActivo(devolucion.getEstado())
                        ? "Activa"
                        : "Cancelada");

        try {

            detalles.setAll(
                    dao.listarDetalle(
                            devolucion.getIdDevolucionProveedor()));

            BigDecimal total = detalles.stream()
                    .map(DetalleDevolucionProveedor::getSubtotal)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            lblTotal.setText(
                    formatoDinero.format(total));

        } catch (Exception e) {

            e.printStackTrace();

            detalles.clear();

            lblTotal.setText(
                    formatoDinero.format(BigDecimal.ZERO));

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error",
                    "No se pudieron cargar los detalles.\n"
                            + e.getMessage());
        }
    }

    // =====================================================
    // LIMPIAR DETALLE
    // =====================================================

    private void limpiarDetalle() {

        detalles.clear();

        txtFolio.clear();
        txtFolioCompra.clear();
        txtProveedor.clear();
        txtEmpleado.clear();
        txtFecha.clear();
        txtEstado.clear();

        lblTotal.setText(
                formatoDinero.format(BigDecimal.ZERO));

        lblEstadoFooter.setText(
                "Selecciona una devolución para consultar su detalle.");

        actualizarBotonCancelar(null);
    }

    // =====================================================
    // SELECCIONAR DEVOLUCIÓN REGISTRADA
    // =====================================================

    public void seleccionarPorId(int id) {

        // Las nuevas devoluciones deben aparecer en la vista
        // de activas, no en la vista de canceladas.
        if (chkMostrarCanceladas.isSelected()) {
            chkMostrarCanceladas.setSelected(false);
            cargarDevoluciones();
        }

        for (DevolucionProveedor devolucion : devoluciones) {

            if (devolucion.getIdDevolucionProveedor() == id) {

                tableDevoluciones.getSelectionModel()
                        .select(devolucion);

                tableDevoluciones.scrollTo(devolucion);

                return;
            }
        }
    }

    // =====================================================
    // REGRESAR AL MENÚ
    // =====================================================

    @FXML
    private void volverAlMenu() {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/laroca/vista/menu.fxml"));

            Parent root = loader.load();

            Stage ventana = (Stage) btnVolverMenu
                    .getScene()
                    .getWindow();

            Navegacion.mostrar(
                    ventana,
                    root,
                    Navegacion.TITULO_MENU);

        } catch (IOException | IllegalStateException e) {

            e.printStackTrace();

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error de navegación",
                    "No se pudo regresar al menú principal.\n"
                            + e.getMessage());
        }
    }

    // =====================================================
    // ALERTAS
    // =====================================================

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
}
