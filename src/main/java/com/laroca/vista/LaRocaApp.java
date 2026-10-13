package com.laroca.vista;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class LaRocaApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource(
                        "/com/laroca/vista/unidad-medida.fxml"
                )
        );

        Parent root = loader.load();

        Scene scene = new Scene(root, 760, 372);

        stage.setTitle("Sistema Abarrotes La Roca");
        stage.setScene(scene);

        // Igual que el diseño de Figma:
        stage.setResizable(false);

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}