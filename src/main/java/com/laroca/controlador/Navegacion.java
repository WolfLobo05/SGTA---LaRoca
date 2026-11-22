package com.laroca.controlador;

import java.io.IOException;

import com.laroca.modelo.Sesion;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public final class Navegacion {

    public static final double ANCHO = 900;
    public static final double ALTO = 600;

    public static final String TITULO_MENU =
            "Sistema Abarrotes \"La Roca\" — Menú Principal";

    private Navegacion() {
    }

    public static void mostrar(Stage ventana, Parent vista, String titulo) {
        ventana.setScene(new Scene(vista, ANCHO, ALTO));
        ventana.setTitle(titulo);
        ventana.setResizable(false);
        ventana.sizeToScene();
    }

    // Comprueba que exista una sesión y que el rol tenga permiso.
    public static boolean verificarPermiso(Node nodo, String permiso) {

        if (verificarSesion(nodo) && Sesion.tienePermiso(permiso)) {
            return true;
        }

        if (!Sesion.haySesion()) {
            return false;
        }

        Platform.runLater(() -> {
            Alert alerta = new Alert(Alert.AlertType.WARNING);
            alerta.setTitle("Acceso restringido");
            alerta.setHeaderText(null);
            alerta.setContentText(
                    "Tu rol (" + Sesion.getRol()
                    + ") no tiene permiso para acceder a este módulo."
            );
            alerta.showAndWait();

            try {
                Parent menu = new FXMLLoader(
                        Navegacion.class.getResource(
                                "/com/laroca/vista/menu.fxml"
                        )
                ).load();

                Stage ventana = (Stage) nodo.getScene().getWindow();

                mostrar(ventana, menu, TITULO_MENU);
                ventana.centerOnScreen();

            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        return false;
    }

    // Comprueba que exista una sesión iniciada.
    public static boolean verificarSesion(Node nodo) {

        if (Sesion.haySesion()) {
            return true;
        }

        Platform.runLater(() -> {
            Alert alerta = new Alert(Alert.AlertType.WARNING);
            alerta.setTitle("Sesión requerida");
            alerta.setHeaderText(null);
            alerta.setContentText(
                    "Inicia sesión para registrar ventas y compras."
            );
            alerta.showAndWait();

            try {
                Parent login = new FXMLLoader(
                        Navegacion.class.getResource(
                                "/com/laroca/vista/login.fxml"
                        )
                ).load();

                Stage ventana = (Stage) nodo.getScene().getWindow();

                ventana.setScene(new Scene(login));
                ventana.setTitle(
                        "Sistema Abarrotes \"La Roca\" — Inicio de Sesión"
                );
                ventana.setResizable(false);
                ventana.sizeToScene();
                ventana.centerOnScreen();

            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        return false;
    }
}
