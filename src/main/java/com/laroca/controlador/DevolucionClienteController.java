package com.laroca.controlador;

import com.laroca.dao.DevolucionClienteDAO;
import com.laroca.modelo.DevolucionCliente;
import com.laroca.modelo.DetalleDevolucionCliente;
import com.laroca.modelo.Estados;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
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

public class DevolucionClienteController {

    // =========================================================
    // COMPONENTES FXML
    // =========================================================

    @FXML private Button btnNuevo;
    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;

    @FXML private CheckBox chkMostrarCanceladas;

    @FXML private Label lblFechaActual;
    @FXML private Label lblTotal;
    @FXML private Label lblEstadoFooter;

    @FXML private TableView<DevolucionCliente> tableDevoluciones;

    @FXML private TableColumn<DevolucionCliente, Integer> colId;
    @FXML private TableColumn<DevolucionCliente, String> colFolio;
    @FXML private TableColumn<DevolucionCliente, String> colFolioVenta;
    @FXML private TableColumn<DevolucionCliente, String> colCliente;
    @FXML private TableColumn<DevolucionCliente, String> colEmpleado;
    @FXML private TableColumn<DevolucionCliente, String> colFecha;
    @FXML private TableColumn<DevolucionCliente, String> colEstado;

    @FXML private TextField txtFolio;
    @FXML private TextField txtFolioVenta;
    @FXML private TextField txtCliente;
    @FXML private TextField txtEmpleado;
    @FXML private TextField txtFecha;
    @FXML private TextField txtEstado;

    @FXML private TableView<DetalleDevolucionCliente> tableDetalle;

    @FXML private TableColumn<DetalleDevolucionCliente, String> colProducto;
    @FXML private TableColumn<DetalleDevolucionCliente, Integer> colCantidad;
    @FXML private TableColumn<DetalleDevolucionCliente, String> colPrecio;
    @FXML private TableColumn<DetalleDevolucionCliente, String> colSubtotal;
    @FXML private TableColumn<DetalleDevolucionCliente, String> colMotivo;

    // =========================================================
    // DATOS
    // =========================================================

    private final DevolucionClienteDAO devolucionDAO =
            new DevolucionClienteDAO();

    private final ObservableList<DevolucionCliente> listaDevoluciones =
            FXCollections.observableArrayList();

    private final ObservableList<DetalleDevolucionCliente> listaDetalles =
            FXCollections.observableArrayList();

    private DevolucionCliente devolucionSeleccionada;

    // =========================================================
    // INICIALIZACIÓN
    // =========================================================

    @FXML
    public void initialize() {

        lblFechaActual.setText(
                LocalDate.now().format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        configurarColumnas();
        configurarTablaDetalle();

        tableDevoluciones.setItems(listaDevoluciones);
        tableDetalle.setItems(listaDetalles);

        lblTotal.setText(Formato.dinero(BigDecimal.ZERO));

        btnEditar.setDisable(true);
        btnEliminar.setDisable(true);

        tableDevoluciones.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, anterior, seleccionada) -> {

                    devolucionSeleccionada = seleccionada;

                    if (seleccionada == null) {
                        limpiarDetalle();
                        return;
                    }

                    mostrarDevolucion(seleccionada);

                    // El botón depende del estado REAL del registro.
                    btnEliminar.setDisable(
                            !Estados.esActivo(seleccionada.getEstado()));

                    // La edición permanece deshabilitada.
                    btnEditar.setDisable(true);
                });

        cargarTabla();
    }

    // =========================================================
    // CONFIGURACIÓN DE TABLAS
    // =========================================================

    private void configurarColumnas() {

        colId.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(
                        c.getValue().getIdDevolucionCliente()));

        colFolio.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        c.getValue().getFolio()));

        colFolioVenta.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        Objects.toString(
                                c.getValue().getFolioVenta(), "")));

        colCliente.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        Objects.toString(
                                c.getValue().getNombreCliente(), "")));

        colEmpleado.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        Objects.toString(
                                c.getValue().getNombreEmpleado(), "")));

        colFecha.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        Formato.fecha(c.getValue().getFecha())));

        // NUEVA COLUMNA: ESTADO
        colEstado.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        obtenerTextoEstado(c.getValue().getEstado())));
    }

    private void configurarTablaDetalle() {

        colProducto.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        c.getValue().getNombreProducto()));

        colCantidad.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(
                        c.getValue().getCantidad()));

        colPrecio.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        Formato.dinero(c.getValue().getPrecio())));

        colSubtotal.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        Formato.dinero(c.getValue().getSubtotal())));

        colMotivo.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(
                        Objects.toString(
                                c.getValue().getMotivo(), "")));

        tableDetalle.setPlaceholder(
                new Label(
                        "Selecciona una devolución para consultar sus productos."));
    }

    // =========================================================
    // OBTENER TEXTO DEL ESTADO
    // =========================================================

    private String obtenerTextoEstado(String estado) {

        return Estados.esActivo(estado)
                ? "Activa"
                : "Cancelada";
    }

    // =========================================================
    // CARGAR DEVOLUCIONES
    // =========================================================

    private void cargarTabla() {

        try {

            boolean mostrarCanceladas =
                    chkMostrarCanceladas.isSelected();

            List<DevolucionCliente> registros =
                    devolucionDAO.listar();

            listaDevoluciones.setAll(
                    registros.stream()
                            .filter(devolucion -> {

                                boolean activa =
                                        Estados.esActivo(
                                                devolucion.getEstado());

                                // Checkbox desmarcado: solo activas.
                                // Checkbox marcado: solo canceladas.
                                return mostrarCanceladas
                                        ? !activa
                                        : activa;
                            })
                            .toList()
            );

            actualizarPie();

            if (!listaDevoluciones.isEmpty()) {

                tableDevoluciones.getSelectionModel()
                        .selectFirst();

            } else {

                tableDevoluciones.getSelectionModel()
                        .clearSelection();

                limpiarDetalle();
            }

        } catch (Exception e) {

            e.printStackTrace();

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error",
                    "No se pudieron cargar las devoluciones.\n"
                            + e.getMessage());
        }
    }

    // =========================================================
    // ACTUALIZAR PIE
    // =========================================================

    private void actualizarPie() {

        int total = listaDevoluciones.size();

        boolean mostrarCanceladas =
                chkMostrarCanceladas.isSelected();

        lblEstadoFooter.setText(
                total
                        + (total == 1
                                ? (mostrarCanceladas
                                        ? " devolución cancelada"
                                        : " devolución registrada")
                                : (mostrarCanceladas
                                        ? " devoluciones canceladas"
                                        : " devoluciones registradas")));
    }

    // =========================================================
    // SELECCIONAR DEVOLUCIÓN POR ID
    // =========================================================

    public void seleccionarPorId(int idDevolucion) {

        if (chkMostrarCanceladas.isSelected()) {

            chkMostrarCanceladas.setSelected(false);
            cargarTabla();
        }

        for (DevolucionCliente devolucion : listaDevoluciones) {

            if (devolucion.getIdDevolucionCliente()
                    == idDevolucion) {

                tableDevoluciones.getSelectionModel()
                        .select(devolucion);

                tableDevoluciones.scrollTo(devolucion);

                return;
            }
        }
    }

    // =========================================================
    // MOSTRAR DETALLE
    // =========================================================

    private void mostrarDevolucion(
            DevolucionCliente devolucion) {

        txtFolio.setText(devolucion.getFolio());

        txtFolioVenta.setText(
                Objects.toString(
                        devolucion.getFolioVenta(), ""));

        txtCliente.setText(
                Objects.toString(
                        devolucion.getNombreCliente(), ""));

        txtEmpleado.setText(
                Objects.toString(
                        devolucion.getNombreEmpleado(), ""));

        txtFecha.setText(
                Formato.fecha(devolucion.getFecha()));

        txtEstado.setText(
                obtenerTextoEstado(devolucion.getEstado()));

        try {

            List<DetalleDevolucionCliente> detalles =
                    devolucionDAO.listarDetalle(
                            devolucion.getIdDevolucionCliente());

            listaDetalles.setAll(detalles);

            BigDecimal total = detalles.stream()
                    .map(DetalleDevolucionCliente::getSubtotal)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            lblTotal.setText(Formato.dinero(total));

        } catch (Exception e) {

            e.printStackTrace();

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error",
                    "No se pudieron cargar los detalles.\n"
                            + e.getMessage());
        }
    }

    // =========================================================
    // LIMPIAR DETALLE
    // =========================================================

    private void limpiarDetalle() {

        devolucionSeleccionada = null;

        txtFolio.clear();
        txtFolioVenta.clear();
        txtCliente.clear();
        txtEmpleado.clear();
        txtFecha.clear();
        txtEstado.clear();

        listaDetalles.clear();

        lblTotal.setText(
                Formato.dinero(BigDecimal.ZERO));

        btnEditar.setDisable(true);
        btnEliminar.setDisable(true);
    }

    // =========================================================
    // NUEVA DEVOLUCIÓN
    // =========================================================

    @FXML
    private void nuevaDevolucion(ActionEvent event) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/laroca/vista/nueva-devolucion-cliente.fxml"));

            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene()
                    .getWindow();

            Navegacion.mostrar(
                    stage,
                    root,
                    "La Roca - Nueva Devolución de Cliente");

        } catch (IOException e) {

            e.printStackTrace();

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error de navegación",
                    "No se pudo abrir la pantalla de registro "
                            + "de devolución.");
        }
    }

    // =========================================================
    // CANCELAR DEVOLUCIÓN
    // =========================================================

    @FXML
    private void eliminarDevolucion() {

        if (devolucionSeleccionada == null) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Selecciona una devolución",
                    "Selecciona una devolución para cancelarla.");

            return;
        }

        if (!Estados.esActivo(
                devolucionSeleccionada.getEstado())) {

            mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Devolución cancelada",
                    "Esta devolución ya se encuentra cancelada.");

            return;
        }

        Alert confirmacion = new Alert(
                Alert.AlertType.CONFIRMATION,
                "¿Deseas cancelar la devolución "
                        + devolucionSeleccionada.getFolio()
                        + " correspondiente a la venta "
                        + devolucionSeleccionada.getFolioVenta()
                        + "?\n\n"
                        + "La devolución quedará cancelada y las unidades "
                        + "se descontarán nuevamente del inventario.",
                ButtonType.YES,
                ButtonType.NO);

        confirmacion.setTitle("Confirmar cancelación");
        confirmacion.setHeaderText(null);

        if (confirmacion.showAndWait()
                .orElse(ButtonType.NO) != ButtonType.YES) {

            return;
        }

        int id = devolucionSeleccionada
                .getIdDevolucionCliente();

        try {

            if (devolucionDAO.cancelar(id)) {

                // Recargar desde la base de datos.
                // Si el filtro está desmarcado, desaparecerá de la lista.
                // Si está marcado, aparecerá en canceladas.
                tableDevoluciones.getSelectionModel()
                        .clearSelection();

                cargarTabla();

                mostrarAlerta(
                        Alert.AlertType.INFORMATION,
                        "Devolución cancelada",
                        "La devolución se canceló correctamente y "
                                + "el inventario fue actualizado.");

            } else {

                String error = devolucionDAO.getMensajeError();

                mostrarAlerta(
                        Alert.AlertType.ERROR,
                        "No se pudo cancelar",
                        error == null || error.isBlank()
                                ? "La devolución no pudo cancelarse."
                                : error);
            }

        } catch (Exception e) {

            e.printStackTrace();

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error",
                    "Ocurrió un problema al cancelar la devolución.\n"
                            + e.getMessage());
        }
    }

    // =========================================================
    // MOSTRAR CANCELADAS
    // =========================================================

    @FXML
    private void cambiarMostrarCanceladas() {

        tableDevoluciones.getSelectionModel()
                .clearSelection();

        cargarTabla();
    }

    // =========================================================
    // VOLVER AL MENÚ
    // =========================================================

    @FXML
    private void volverAlMenu(ActionEvent event) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/laroca/vista/menu.fxml"));

            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene()
                    .getWindow();

            Navegacion.mostrar(
                    stage,
                    root,
                    Navegacion.TITULO_MENU);

        } catch (IOException e) {

            e.printStackTrace();

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error de navegación",
                    "No se pudo abrir el menú principal.");
        }
    }

    // =========================================================
    // ALERTAS
    // =========================================================

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
