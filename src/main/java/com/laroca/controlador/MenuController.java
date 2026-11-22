package com.laroca.controlador;

import java.io.IOException;

import javafx.event.ActionEvent;

import com.laroca.modelo.Sesion;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class MenuController {

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

            ventana.setScene(new Scene(login));
            ventana.setTitle("Sistema Abarrotes \"La Roca\" — Inicio de Sesión");
            ventana.setResizable(false);
            ventana.sizeToScene();
            ventana.centerOnScreen();
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

        aplicarPermisos();
    }

    // Muestra únicamente los módulos permitidos para cada rol.
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

    // Oculta el botón y elimina el espacio que ocupaba.
    private void configurar(Button boton, String permiso) {
        boolean permitido = Sesion.tienePermiso(permiso);

        boton.setVisible(permitido);
        boton.setManaged(permitido);
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
