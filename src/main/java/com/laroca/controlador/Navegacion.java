package com.laroca.controlador;

import javafx.scene.Parent;
import javafx.scene.Scene;
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
}
