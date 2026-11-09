package com.laroca.controlador;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class MenuController {

    @FXML
    private Button btnUnidades;

    @FXML
    private Button btnCategorias;

    @FXML
    private void abrirUnidades() throws IOException {
        abrirVentana(
                "/com/laroca/vista/unidad-medida.fxml",
                "La Roca - Unidades de Medida");
    }

    @FXML
    private void abrirCategorias() throws IOException {
        abrirVentana(
                "/com/laroca/vista/categorias.fxml",
                "La Roca - Categorías");
    }

    @FXML
    private void abrirRoles() throws IOException {
        abrirVentana(
                "/com/laroca/vista/roles.fxml",
                "La Roca - Roles");
    }

    @FXML
    private void abrirProveedores() throws IOException {
        abrirVentana(
                "/com/laroca/vista/proveedores.fxml",
                "La Roca - Proveedores");
    }

    @FXML
    private void abrirProductos() throws IOException {
        abrirVentana(
                "/com/laroca/vista/productos.fxml",
                "La Roca - Productos");
    }

    @FXML
    private void abrirUsuarios() throws IOException {
        abrirVentana(
                "/com/laroca/vista/usuarios.fxml",
                "La Roca - Usuarios");
    }

    @FXML
    private void abrirClientes() throws IOException {
        abrirVentana(
                "/com/laroca/vista/clientes.fxml",
                "La Roca - Clientes");
    }

    @FXML
    private void abrirVentas() throws IOException {
        abrirVentana(
                "/com/laroca/vista/ventas.fxml",
                "La Roca - Ventas");
    }

    @FXML
    private void abrirCompras() throws IOException {
        abrirVentana(
                "/com/laroca/vista/compras.fxml",
                "La Roca - Compras");
    }

    @FXML
    private void abrirMermas() throws IOException {
        abrirVentana(
                "/com/laroca/vista/mermas.fxml",
                "La Roca - Mermas");
    }

    @FXML
    private void abrirDevolucionesClientes() throws IOException {
        abrirVentana(
                "/com/laroca/vista/devoluciones-clientes.fxml",
                "La Roca - Devoluciones de Clientes");
    }

    @FXML
    private void abrirDevolucionesProveedores() throws IOException {
        abrirVentana(
                "/com/laroca/vista/devoluciones-proveedores.fxml",
                "La Roca - Devoluciones a Proveedores");
    }

    private void abrirVentana(String ruta, String titulo)
            throws IOException {

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource(ruta));

        Parent vista = loader.load();

        // Se usa la misma ventana del menú, con el tamaño estándar,
        // para que no cambie de tamaño ni de posición al navegar.
        Stage ventana = (Stage) btnUnidades.getScene().getWindow();
        Navegacion.mostrar(ventana, vista, titulo);
    }
}
