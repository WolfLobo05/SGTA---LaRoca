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

// Cambio de pantalla con el tamaño estándar de la aplicación.
// El menú y las 7 pantallas CRUD usan la misma ventana de 900x600,
// fija, para que no cambie de tamaño al navegar entre ellas.
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

        // Después de setResizable(false), para que la ventana se ajuste
        // exactamente a la escena (en Windows puede quedar un margen).
        ventana.sizeToScene();
    }

    // Ventas y compras se registran a nombre del empleado con sesión.
    // Si no hay sesión, avisa y regresa al inicio de sesión.
    // Se llama desde initialize(); el cambio de pantalla se hace cuando
    // la vista ya está en su ventana.
    public static boolean verificarSesion(Node nodo) {
        if (Sesion.haySesion()) {
            return true;
        }

        Platform.runLater(() -> {
            Alert alerta = new Alert(Alert.AlertType.WARNING);
            alerta.setTitle("Sesión requerida");
            alerta.setHeaderText(null);
            alerta.setContentText(
                    "Inicia sesión para registrar ventas y compras.");
            alerta.showAndWait();

            try {
                Parent login = new FXMLLoader(Navegacion.class.getResource(
                        "/com/laroca/vista/login.fxml")).load();

                // El inicio de sesión conserva su propio tamaño.
                Stage ventana = (Stage) nodo.getScene().getWindow();
                ventana.setScene(new Scene(login));
                ventana.setTitle("Sistema Abarrotes \"La Roca\" — Inicio de Sesión");
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
