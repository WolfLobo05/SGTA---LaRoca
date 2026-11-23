package com.laroca.controlador;

import com.laroca.dao.EmpleadoDAO;
import com.laroca.modelo.Sesion;
import com.laroca.modelo.Usuario;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private CheckBox chkRecordar;
    @FXML private Hyperlink linkOlvidado;
    @FXML private Button btnIngresar;
    @FXML private Label lblMensaje;

    private final EmpleadoDAO empleadoDAO = new EmpleadoDAO();

    @FXML
    private void ingresar(ActionEvent event) {
        String usuario = txtUsuario.getText().trim();
        String password = txtPassword.getText();

        if (usuario.isEmpty() || password.isEmpty()) {
            lblMensaje.setText("Por favor, ingresa usuario y contraseña.");
            return;
        }

        Usuario empleado = empleadoDAO.iniciarSesion(usuario, password);

        if (empleado != null) {

            // Guardar los datos del empleado y su rol en la sesión.
            Sesion.establecer(
                    empleado.getIdUsuario(),
                    empleado.getUsuario(),
                    empleado.getNombreCompleto(),
                    empleado.getNombreRol()
            );

            try {
                // Cargar el menú principal.
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/laroca/vista/menu.fxml")
                );

                Parent root = loader.load();

                Stage stage = (Stage) ((Node) event.getSource())
                        .getScene().getWindow();

                Navegacion.mostrar(stage, root, Navegacion.TITULO_MENU);
                stage.centerOnScreen();
                stage.show();

                mostrarBienvenida(stage);

            } catch (IOException e) {
                e.printStackTrace();
                Sesion.cerrar();
                lblMensaje.setText("Error al cargar la pantalla del menú.");
            }

        } else {
            lblMensaje.setText("Usuario o contraseña incorrectos.");
        }
    }

    // Informa al usuario con qué rol ha iniciado sesión.
    private void mostrarBienvenida(Stage stage) {
        String rol = Sesion.getRolParaMostrar();

        Alert bienvenida = new Alert(Alert.AlertType.INFORMATION);
        bienvenida.initOwner(stage);
        bienvenida.setTitle("Inicio de sesión");
        bienvenida.setHeaderText(
                "¡Bienvenido, " + Sesion.getNombreParaMostrar() + "!"
        );
        bienvenida.setContentText(
                "Has iniciado sesión como " + rol + "."
        );
        bienvenida.showAndWait();
    }

    @FXML
    private void olvidarPassword() {
        lblMensaje.setText(
                "Contacta al administrador para restablecer tu acceso."
        );
    }
}
