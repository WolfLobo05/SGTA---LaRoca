package com.laroca.vista;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class LaRocaApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // CAMBIA "menu.fxml" POR "login.fxml" AQUÍ:
       FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/laroca/vista/login.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("Sistema Abarrotes \"La Roca\" — Inicio de Sesión");
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}