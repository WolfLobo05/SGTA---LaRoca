package com.laroca.controlador;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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

    private void abrirVentana(String ruta, String titulo)
            throws IOException {

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource(ruta));

        Parent vista = loader.load();

        Stage ventana = new Stage();
        ventana.setTitle(titulo);
        ventana.setScene(new Scene(vista));
        ventana.setResizable(false);
        ventana.show();

        // Cerrar el menú actual.
        Stage menu = (Stage) btnUnidades.getScene().getWindow();
        menu.close();
    }
}
