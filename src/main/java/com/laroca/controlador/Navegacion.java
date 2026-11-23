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

    // Tamaño inicial y mínimo del área de contenido después del login.
    public static final double ANCHO = 900;
    public static final double ALTO = 600;

    public static final String TITULO_MENU =
            "Sistema Abarrotes \"La Roca\" — Menú Principal";

    public static final String TITULO_LOGIN =
            "Sistema Abarrotes \"La Roca\" — Inicio de Sesión";

    // Marca la escena principal (la que se reutiliza entre pantallas).
    private static final String ESCENA_PRINCIPAL = "laroca.escenaPrincipal";

    private Navegacion() {
    }

    // Cambia de pantalla dentro de la ventana principal. La escena se
    // reutiliza, así el tamaño y el estado maximizado se conservan.
    public static void mostrar(Stage ventana, Parent vista, String titulo) {
        Scene actual = ventana.getScene();

        if (actual != null && actual.getProperties().containsKey(ESCENA_PRINCIPAL)) {
            actual.setRoot(vista);
            ventana.setTitle(titulo);
            return;
        }

        // Primera pantalla después del login: escena nueva y ventana redimensionable.
        double bordeAncho = 0;
        double bordeAlto = 0;

        if (actual != null && ventana.isShowing()) {
            bordeAncho = Math.max(0, ventana.getWidth() - actual.getWidth());
            bordeAlto = Math.max(0, ventana.getHeight() - actual.getHeight());
        }

        Scene escena = new Scene(vista, ANCHO, ALTO);
        escena.getProperties().put(ESCENA_PRINCIPAL, Boolean.TRUE);

        ventana.setScene(escena);
        ventana.setTitle(titulo);
        ventana.setResizable(true);
        ventana.setMinWidth(ANCHO + bordeAncho);
        ventana.setMinHeight(ALTO + bordeAlto);
        ventana.sizeToScene();
    }

    // Regresa al inicio de sesión: ventana chica y de tamaño fijo.
    public static void mostrarLogin(Stage ventana, Parent login) {
        ventana.setMaximized(false);
        ventana.setMinWidth(0);
        ventana.setMinHeight(0);
        ventana.setScene(new Scene(login));
        ventana.setTitle(TITULO_LOGIN);
        ventana.setResizable(false);
        ventana.sizeToScene();
        ventana.centerOnScreen();
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

                mostrarLogin(ventana, login);

            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        return false;
    }
}
