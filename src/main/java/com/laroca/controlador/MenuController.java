package com.laroca.controlador;

import java.io.IOException;

import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;

import com.laroca.modelo.Sesion;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;

public class MenuController {

    // Íconos de trazo en una cuadrícula de 24x24.
    private static final String ICONO_CARRITO =
            "M3 4h2l2.4 10.2a1 1 0 0 0 1 .8h8.9a1 1 0 0 0 1-.8L20 8H6.2"
            + " M10.25 19.5a1.25 1.25 0 1 1-2.5 0a1.25 1.25 0 1 1 2.5 0z"
            + " M18.25 19.5a1.25 1.25 0 1 1-2.5 0a1.25 1.25 0 1 1 2.5 0z";

    private static final String ICONO_CAMION =
            "M3 6h11v9H3z M14 9h4l3 3v3h-7z"
            + " M8.5 17.5a1.5 1.5 0 1 1-3 0a1.5 1.5 0 1 1 3 0z"
            + " M18.5 17.5a1.5 1.5 0 1 1-3 0a1.5 1.5 0 1 1 3 0z";

    private static final String ICONO_CAJA =
            "M3 7.5 12 3l9 4.5v9L12 21l-9-4.5z M3 7.5 12 12l9-4.5M12 12v9";

    private static final String ICONO_ADVERTENCIA =
            "M12 3 2.5 20h19z M12 10v4 M12 17h.01";

    private static final String ICONO_GRAFICA =
            "M4 20V4M4 20h16 M8 16v-4M12 16V8M16 16v-6";

    private static final String ICONO_DEVOLUCION =
            "M9 14 4 9l5-5 M4 9h10a6 6 0 0 1 0 12h-3";

    private static final String ICONO_SALIR =
            "M15 4h4v16h-4 M10 8l-4 4 4 4M6 12h10";

    private static final String SIN_ACCESO = "Sin acceso para tu rol";

    @FXML private Button btnUnidades;
    @FXML private Button btnCategorias;
    @FXML private Button btnRoles;
    @FXML private Button btnProveedores;
    @FXML private Button btnProductos;
    @FXML private Button btnUsuarios;
    @FXML private Button btnClientes;
    @FXML private Button btnVentas;
    @FXML private Button btnCompras;
    @FXML private Button btnMermas;
    @FXML private Button btnDevolucionesClientes;
    @FXML private Button btnDevolucionesProveedores;
    @FXML private Button btnEstadisticasGenerales;
    @FXML private Button btnConsultarInventario;
    @FXML private Button btnCerrarSesion;

    @FXML private Label lblSaludo;

    // Cierra la sesión actual y regresa al inicio de sesión sin cerrar la aplicación.
    @FXML
    private void cerrarSesion(ActionEvent event) {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Cerrar sesión");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText("¿Estás seguro de que deseas cerrar tu sesión?");
        ButtonType botonSi = new ButtonType("Sí", ButtonBar.ButtonData.YES);
        ButtonType botonNo = new ButtonType("No", ButtonBar.ButtonData.NO);
        confirmacion.getButtonTypes().setAll(botonSi, botonNo);

        if (confirmacion.showAndWait().orElse(botonNo) != botonSi) {
            return;
        }

        try {
            Parent login = new FXMLLoader(
                    getClass().getResource("/com/laroca/vista/login.fxml")
            ).load();

            Stage ventana = (Stage) ((Node) event.getSource())
                    .getScene().getWindow();

            // Se limpia la sesión únicamente cuando el login ya se cargó correctamente.
            Sesion.cerrar();

            Navegacion.mostrarLogin(ventana, login);
            ventana.show();

        } catch (IOException e) {
            e.printStackTrace();

            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Error");
            error.setHeaderText(null);
            error.setContentText("No se pudo regresar al inicio de sesión.");
            error.showAndWait();
        }
    }

    @FXML
    public void initialize() {
        if (!Navegacion.verificarSesion(btnUnidades)) {
            return;
        }

        lblSaludo.setText("Hola, " + Sesion.getNombreParaMostrar()
                + " · " + Sesion.getRolParaMostrar());

        armarTarjeta(btnVentas, ICONO_CARRITO, 44,
                "Registrar una venta y consultar el historial");
        armarTarjeta(btnCompras, ICONO_CAMION, 28, "Entrada de mercancía");
        armarTarjeta(btnConsultarInventario, ICONO_CAJA, 28, "Existencias y reorden");
        armarTarjeta(btnMermas, ICONO_ADVERTENCIA, 28, "Producto dañado o caducado");
        armarTarjeta(btnEstadisticasGenerales, ICONO_GRAFICA, 28, "Resumen general");

        btnDevolucionesClientes.setGraphic(icono(ICONO_DEVOLUCION, 16));
        btnDevolucionesProveedores.setGraphic(icono(ICONO_DEVOLUCION, 16));
        btnCerrarSesion.setGraphic(icono(ICONO_SALIR, 16));

        aplicarPermisos();
    }

    // Los módulos sin permiso se muestran deshabilitados (nunca se ocultan).
    private void aplicarPermisos() {
        configurar(btnUnidades, "UNIDADES");
        configurar(btnCategorias, "CATEGORIAS");
        configurar(btnRoles, "ROLES");
        configurar(btnProveedores, "PROVEEDORES");
        configurar(btnProductos, "PRODUCTOS");
        configurar(btnUsuarios, "USUARIOS");
        configurar(btnClientes, "CLIENTES");
        configurar(btnVentas, "VENTAS");
        configurar(btnCompras, "COMPRAS");
        configurar(btnMermas, "MERMAS");
        configurar(btnDevolucionesClientes, "DEVOLUCIONES_CLIENTES");
        configurar(btnDevolucionesProveedores, "DEVOLUCIONES_PROVEEDORES");
        configurar(btnEstadisticasGenerales, "ESTADISTICAS_GENERALES");
        configurar(btnConsultarInventario, "CONSULTAR_INVENTARIO");
    }

    private void configurar(Button boton, String permiso) {
        if (Sesion.tienePermiso(permiso)) {
            return;
        }

        boton.setDisable(true);
        boton.getStyleClass().add("sin-permiso");

        if (boton.getGraphic() != null) {
            Node subtitulo = boton.getGraphic().lookup(".tarjeta-menu-subtitulo");

            if (subtitulo instanceof Label etiqueta) {
                etiqueta.setText(SIN_ACCESO);
            }
        }
    }

    // Tarjeta: ícono arriba a la izquierda; título y subtítulo abajo.
    private static void armarTarjeta(Button boton, String trazo,
            double tamanoIcono, String textoSubtitulo) {

        Label titulo = new Label(boton.getText());
        titulo.getStyleClass().add("tarjeta-menu-titulo");

        Label subtitulo = new Label(textoSubtitulo);
        subtitulo.getStyleClass().add("tarjeta-menu-subtitulo");

        Region espacio = new Region();
        VBox.setVgrow(espacio, Priority.ALWAYS);

        VBox contenido = new VBox(icono(trazo, tamanoIcono), espacio, titulo, subtitulo);
        contenido.setAlignment(Pos.TOP_LEFT);
        contenido.setMinSize(0, 0);

        // El botón coloca el gráfico a su tamaño preferido: se iguala al
        // espacio interior del botón para que el contenido lo llene.
        contenido.prefWidthProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(0, boton.getWidth() - ancho(boton.getInsets())),
                boton.widthProperty(), boton.insetsProperty()));

        contenido.prefHeightProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(0, boton.getHeight() - alto(boton.getInsets())),
                boton.heightProperty(), boton.insetsProperty()));

        boton.setGraphic(contenido);
        boton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        boton.setAlignment(Pos.TOP_LEFT);
    }

    private static double ancho(Insets margen) {
        return margen.getLeft() + margen.getRight();
    }

    private static double alto(Insets margen) {
        return margen.getTop() + margen.getBottom();
    }

    // Ícono de trazo escalado desde la cuadrícula de 24x24. El Group
    // exterior hace que el diseño ocupe el tamaño ya escalado.
    private static Node icono(String trazo, double tamano) {
        SVGPath figura = new SVGPath();
        figura.setContent(trazo);
        figura.getStyleClass().add("icono-menu");

        Rectangle marco = new Rectangle(24, 24, Color.TRANSPARENT);

        Group dibujo = new Group(marco, figura);
        double escala = tamano / 24;
        dibujo.getTransforms().add(new Scale(escala, escala, 0, 0));

        return new Group(dibujo);
    }

    // Comprueba nuevamente el permiso antes de abrir una pantalla.
    private void abrir(String permiso, String ruta, String titulo)
            throws IOException {

        if (!Sesion.tienePermiso(permiso)) {
            Alert alerta = new Alert(Alert.AlertType.WARNING);
            alerta.setTitle("Acceso restringido");
            alerta.setHeaderText(null);
            alerta.setContentText(
                    "Tu rol no tiene permiso para acceder a este módulo."
            );
            alerta.showAndWait();
            return;
        }

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource(ruta)
        );

        Parent vista = loader.load();

        Stage ventana = (Stage) btnUnidades.getScene().getWindow();

        Navegacion.mostrar(ventana, vista, titulo);
    }

    @FXML
    private void abrirUnidades() throws IOException {
        abrir("UNIDADES",
                "/com/laroca/vista/unidad-medida.fxml",
                "La Roca - Unidades de Medida");
    }

    @FXML
    private void abrirCategorias() throws IOException {
        abrir("CATEGORIAS",
                "/com/laroca/vista/categorias.fxml",
                "La Roca - Categorías");
    }

    @FXML
    private void abrirRoles() throws IOException {
        abrir("ROLES",
                "/com/laroca/vista/roles.fxml",
                "La Roca - Roles");
    }

    @FXML
    private void abrirProveedores() throws IOException {
        abrir("PROVEEDORES",
                "/com/laroca/vista/proveedores.fxml",
                "La Roca - Proveedores");
    }

    @FXML
    private void abrirProductos() throws IOException {
        abrir("PRODUCTOS",
                "/com/laroca/vista/productos.fxml",
                "La Roca - Productos");
    }

    @FXML
    private void abrirUsuarios() throws IOException {
        abrir("USUARIOS",
                "/com/laroca/vista/usuarios.fxml",
                "La Roca - Usuarios");
    }

    @FXML
    private void abrirClientes() throws IOException {
        abrir("CLIENTES",
                "/com/laroca/vista/clientes.fxml",
                "La Roca - Clientes");
    }

    @FXML
    private void abrirVentas() throws IOException {
        abrir("VENTAS",
                "/com/laroca/vista/ventas.fxml",
                "La Roca - Ventas");
    }

    @FXML
    private void abrirCompras() throws IOException {
        abrir("COMPRAS",
                "/com/laroca/vista/compras.fxml",
                "La Roca - Compras");
    }

    @FXML
    private void abrirMermas() throws IOException {
        abrir("MERMAS",
                "/com/laroca/vista/mermas.fxml",
                "La Roca - Mermas");
    }

    @FXML
    private void abrirDevolucionesClientes() throws IOException {
        abrir("DEVOLUCIONES_CLIENTES",
                "/com/laroca/vista/devoluciones-clientes.fxml",
                "La Roca - Devoluciones de Clientes");
    }

    @FXML
    private void abrirDevolucionesProveedores() throws IOException {
        abrir("DEVOLUCIONES_PROVEEDORES",
                "/com/laroca/vista/devoluciones-proveedores.fxml",
                "La Roca - Devoluciones a Proveedores");
    }

    @FXML
    private void abrirEstadisticasStock() throws IOException {
        abrir("CONSULTAR_INVENTARIO",
                "/com/laroca/vista/estadisticas-stock.fxml",
                "La Roca - Consultar Inventario");
    }

    @FXML
    private void abrirEstadisticasGenerales() throws IOException {
        abrir("ESTADISTICAS_GENERALES",
                "/com/laroca/vista/estadisticas-generales.fxml",
                "La Roca - Estadísticas Generales");
    }
}
