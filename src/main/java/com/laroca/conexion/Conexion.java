package com.laroca.conexion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Conexion {

    private static final String URL = "jdbc:mysql://localhost:3306/sgta_laroca";
    private static final String USUARIO = "root";
    private static final String PASSWORD = "011125";

    public static Connection conectar() {
        Connection conexion = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            conexion = DriverManager.getConnection(URL, USUARIO, PASSWORD);

            System.out.println("Conexión exitosa a la base de datos.");

        } catch (ClassNotFoundException e) {
            System.out.println("No se encontró el driver de MySQL.");
            e.printStackTrace();

        } catch (SQLException e) {
            System.out.println("Error al conectar con la base de datos.");
            e.printStackTrace();
        }

        return conexion;
    }
}