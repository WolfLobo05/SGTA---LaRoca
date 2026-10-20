package com.laroca.controlador;

import com.laroca.dao.EmpleadoDAO;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
        String password = txtPassword.getText().trim();

        if (usuario.isEmpty() || password.isEmpty()) {
            lblMensaje.setText("Por favor, ingresa usuario y contraseña.");
            return;
        }

        if (empleadoDAO.validarUsuario(usuario, password)) {
            try {
                // Cargar el Menú Principal
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/laroca/vista/menu.fxml"));
                Parent root = loader.load();

                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("Sistema Abarrotes \"La Roca\" — Menú Principal");
                stage.centerOnScreen();
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
                lblMensaje.setText("Error al cargar la pantalla del menú.");
            }
        } else {
            lblMensaje.setText("Usuario o contraseña incorrectos.");
        }
    }

    @FXML
    private void olvidarPassword() {
        lblMensaje.setText("Contacta al administrador para restablecer tu acceso.");
    }
}
