package com.laroca.controlador;

import java.util.List;
import java.util.function.Function;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

// Ventana modal con una tabla filtrable para elegir un registro
// (productos o clientes en Nueva Venta).
// Doble clic, Enter o "Seleccionar" devuelven el registro elegido.
class DialogoSeleccion<T> {

    private final Stage ventana = new Stage();
    private final TextField txtFiltro = new TextField();
    private final TableView<T> tabla = new TableView<>();
    private final Function<String, List<T>> buscar;

    private T seleccionado;

    // buscar recibe el texto del filtro y devuelve los registros que coinciden.
    DialogoSeleccion(Window duenio, String titulo, Function<String, List<T>> buscar) {
        this.buscar = buscar;

        Label lblTitulo = new Label("Sistema abarrotes \"La Roca\" — " + titulo);
        lblTitulo.getStyleClass().add("titulo");
        HBox barraTitulo = new HBox(lblTitulo);
        barraTitulo.getStyleClass().add("barra-titulo");

        Label lblFiltro = new Label("Buscar");
        lblFiltro.getStyleClass().add("etiqueta");
        txtFiltro.setPromptText("Escribe para filtrar");
        VBox campoFiltro = new VBox(lblFiltro, txtFiltro);
        campoFiltro.getStyleClass().addAll("campo", "formulario-compacto");

        tabla.getStyleClass().add("tabla");
        tabla.setPlaceholder(new Label("Sin resultados"));
        VBox.setVgrow(tabla, Priority.ALWAYS);

        Button btnSeleccionar = new Button("Seleccionar");
        btnSeleccionar.getStyleClass().add("boton-principal");
        btnSeleccionar.disableProperty().bind(
                tabla.getSelectionModel().selectedItemProperty().isNull());
        btnSeleccionar.setOnAction(e -> aceptar());

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().add("boton-secundario");
        btnCancelar.setOnAction(e -> ventana.close());

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox acciones = new HBox(8, espacio, btnSeleccionar, btnCancelar);
        acciones.setAlignment(Pos.CENTER_RIGHT);

        VBox contenido = new VBox(campoFiltro, tabla, acciones);
        contenido.getStyleClass().add("contenido");

        BorderPane raiz = new BorderPane(contenido);
        raiz.setTop(barraTitulo);
        raiz.getStyleClass().add("ventana");

        Scene escena = new Scene(raiz, 560, 420);
        escena.getStylesheets().add(
                getClass().getResource("/com/laroca/vista/laroca.css").toExternalForm());

        ventana.setScene(escena);
        ventana.setTitle("La Roca - " + titulo);
        ventana.initOwner(duenio);
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.setResizable(false);

        txtFiltro.textProperty().addListener((obs, anterior, actual) -> filtrar());

        // Enter en el filtro: elegir si solo hay un resultado.
        txtFiltro.setOnAction(e -> {
            if (tabla.getItems().size() == 1) {
                tabla.getSelectionModel().selectFirst();
                aceptar();
            } else if (!tabla.getItems().isEmpty()) {
                tabla.requestFocus();
                tabla.getSelectionModel().selectFirst();
            }
        });

        tabla.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                aceptar();
            }
        });

        tabla.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                aceptar();
            }
        });
    }

    void agregarColumna(String titulo, Function<T, String> valor,
                        double ancho, boolean numero) {

        TableColumn<T, String> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(
                c -> new ReadOnlyStringWrapper(valor.apply(c.getValue())));
        columna.setPrefWidth(ancho);
        columna.setMinWidth(Math.min(ancho, 70));

        if (numero) {
            columna.getStyleClass().add("columna-numero");
        }

        tabla.getColumns().add(columna);
    }

    // Muestra la ventana y espera. Devuelve el registro elegido o null.
    T mostrar(String filtroInicial) {
        seleccionado = null;
        txtFiltro.setText(filtroInicial == null ? "" : filtroInicial);
        filtrar();
        txtFiltro.requestFocus();
        txtFiltro.end();
        ventana.showAndWait();
        return seleccionado;
    }

    private void filtrar() {
        tabla.setItems(FXCollections.observableArrayList(
                buscar.apply(txtFiltro.getText().trim())));
    }

    private void aceptar() {
        T elegido = tabla.getSelectionModel().getSelectedItem();

        if (elegido != null) {
            seleccionado = elegido;
            ventana.close();
        }
    }
}
