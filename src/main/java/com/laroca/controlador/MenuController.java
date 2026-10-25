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
