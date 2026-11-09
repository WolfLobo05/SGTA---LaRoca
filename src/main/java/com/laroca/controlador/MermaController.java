package com.laroca.controlador;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import com.laroca.dao.MermaDAO;
import com.laroca.modelo.DetalleMerma;
import com.laroca.modelo.Estados;
import com.laroca.modelo.Merma;

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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class MermaController {

    @FXML private Button btnNueva;
    @FXML private Button btnCancelar;
    @FXML private CheckBox chkMostrarCanceladas;

    @FXML private TableView<Merma> tableMermas;
    @FXML private TableColumn<Merma, Integer> colId;
    @FXML private TableColumn<Merma, String> colFolio;
    @FXML private TableColumn<Merma, String> colEmpleado;
    @FXML private TableColumn<Merma, String> colFecha;
    @FXML private TableColumn<Merma, String> colEstado;

    @FXML private TextField txtFolio;
    @FXML private TextField txtFecha;
    @FXML private TextField txtEmpleado;
    @FXML private TextField txtEstado;

    @FXML private TableView<DetalleMerma> tableDetalle;
    @FXML private TableColumn<DetalleMerma, String> colProducto;
    @FXML private TableColumn<DetalleMerma, Integer> colCantidad;
    @FXML private TableColumn<DetalleMerma, String> colPrecio;
    @FXML private TableColumn<DetalleMerma, String> colSubtotal;
    @FXML private TableColumn<DetalleMerma, String> colMotivo;

    @FXML private Label lblTotal;
    @FXML private Label lblArticulos;
    @FXML private Label lblFechaActual;
    @FXML private Label lblEstadoFooter;

    private final MermaDAO mermaDAO = new MermaDAO();
    private ObservableList<Merma> listaMermas;
    private Merma mermaSeleccionada;

    @FXML
    public void initialize() {
        lblFechaActual.setText(Formato.fecha(LocalDate.now()));

        colId.setCellValueFactory(new PropertyValueFactory<>("idMerma"));
        colFolio.setCellValueFactory(new PropertyValueFactory<>("folio"));
        colEmpleado.setCellValueFactory(new PropertyValueFactory<>("nombreEmpleado"));
        colFecha.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                Formato.fecha(c.getValue().getFecha())));
        colEstado.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                Estados.esActivo(c.getValue().getEstado()) ? "Activa" : "Cancelada"));

        colProducto.setCellValueFactory(new PropertyValueFactory<>("nombreProducto"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colPrecio.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                Formato.dinero(c.getValue().getPrecio())));
        colSubtotal.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                Formato.dinero(c.getValue().getSubtotal())));
        colMotivo.setCellValueFactory(new PropertyValueFactory<>("motivo"));

        tableMermas.setPlaceholder(new Label("No hay mermas para mostrar."));
        tableDetalle.setPlaceholder(new Label("Selecciona una merma para consultar sus productos."));

        cargarTabla();
        limpiarDetalle();

        tableMermas.getSelectionModel().selectedItemProperty().addListener(
                (obs, anterior, seleccionada) -> {
                    mermaSeleccionada = seleccionada;
                    if (seleccionada == null) limpiarDetalle();
                    else mostrarMerma(seleccionada);
                });
    }

    private void cargarTabla() {
        boolean canceladas = chkMostrarCanceladas.isSelected();
        listaMermas = FXCollections.observableArrayList();

        for (Merma merma : mermaDAO.listar()) {
            if (Estados.esActivo(merma.getEstado()) != canceladas) {
                listaMermas.add(merma);
            }
        }

        tableMermas.setItems(listaMermas);
        actualizarPie();
    }

    private void actualizarPie() {
        int cantidad = listaMermas == null ? 0 : listaMermas.size();
        boolean canceladas = chkMostrarCanceladas.isSelected();
        lblEstadoFooter.setText(cantidad + (cantidad == 1
                ? (canceladas ? " merma cancelada" : " merma registrada")
                : (canceladas ? " mermas canceladas" : " mermas registradas")));
    }

    private void mostrarMerma(Merma merma) {
        txtFolio.setText(merma.getFolio());
        txtFecha.setText(Formato.fecha(merma.getFecha()));
        txtEmpleado.setText(Objects.toString(merma.getNombreEmpleado(), ""));
        txtEstado.setText(Estados.esActivo(merma.getEstado()) ? "Activa" : "Cancelada");

        List<DetalleMerma> detalles = mermaDAO.listarDetalle(merma.getIdMerma());
        tableDetalle.setItems(FXCollections.observableArrayList(detalles));

        int articulos = 0;
        BigDecimal total = BigDecimal.ZERO;
        for (DetalleMerma detalle : detalles) {
            articulos += detalle.getCantidad();
            total = total.add(detalle.getSubtotal());
        }
        lblArticulos.setText(String.valueOf(articulos));
        lblTotal.setText(Formato.dinero(total));
        btnCancelar.setDisable(!Estados.esActivo(merma.getEstado()));
    }

    private void limpiarDetalle() {
        txtFolio.clear();
        txtFecha.clear();
        txtEmpleado.clear();
        txtEstado.clear();
        tableDetalle.getItems().clear();
        lblArticulos.setText("0");
        lblTotal.setText(Formato.dinero(BigDecimal.ZERO));
        btnCancelar.setDisable(true);
    }

    @FXML
    private void nuevaMerma(ActionEvent event) {
        try {
            Parent root = new FXMLLoader(getClass().getResource(
                    "/com/laroca/vista/registrar-merma.fxml")).load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Navegacion.mostrar(stage, root, "La Roca - Registrar Merma");
        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir la pantalla para registrar mermas.");
        }
    }

    @FXML
    private void cancelarMerma() {
        if (mermaSeleccionada == null || !Estados.esActivo(mermaSeleccionada.getEstado())) return;

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                "La merma " + mermaSeleccionada.getFolio()
                + " quedará cancelada y se devolverán al inventario las cantidades registradas.\n\n"
                + "¿Deseas continuar?", ButtonType.YES, ButtonType.NO);
        confirmacion.setTitle("Confirmar cancelación de merma");
        confirmacion.setHeaderText(null);

        if (confirmacion.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        int idMerma = mermaSeleccionada.getIdMerma();
        if (mermaDAO.cancelar(idMerma)) {
            tableMermas.getSelectionModel().clearSelection();
            cargarTabla();
            mostrarInformacion("Merma cancelada",
                    "La merma se canceló y las existencias fueron restauradas.");
        } else {
            mostrarAlerta("No se pudo cancelar", Objects.toString(
                    mermaDAO.getMensajeError(), "Ocurrió un error al cancelar la merma."));
        }
    }

    @FXML
    private void cambiarMostrarCanceladas() {
        tableMermas.getSelectionModel().clearSelection();
        cargarTabla();
    }

    public void seleccionarPorId(int idMerma) {
        if (listaMermas == null) return;
        for (Merma merma : listaMermas) {
            if (merma.getIdMerma() == idMerma) {
                tableMermas.getSelectionModel().select(merma);
                tableMermas.scrollTo(merma);
                tableMermas.requestFocus();
                return;
            }
        }
    }

    @FXML
    private void volverAlMenu(ActionEvent event) {
        try {
            Parent root = new FXMLLoader(getClass().getResource(
                    "/com/laroca/vista/menu.fxml")).load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Navegacion.mostrar(stage, root, Navegacion.TITULO_MENU);
        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir el menú principal.");
        }
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
