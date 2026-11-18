package com.laroca.conexion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// Única forma de conectarse a la base de datos en toda la aplicación.
// El nombre de la base es el mismo que crea SGTA_LaRoca.sql.
//
// Los valores por omisión se pueden reemplazar sin tocar el código con
// propiedades del sistema (-Dlaroca.db.url=...) o variables de entorno
// (LAROCA_DB_URL, LAROCA_DB_USUARIO, LAROCA_DB_PASSWORD).
public class Conexion {

    private static final String URL = valor("laroca.db.url", "LAROCA_DB_URL",
            "jdbc:mysql://localhost:3306/SGTA_LaRoca");
    private static final String USUARIO = valor("laroca.db.usuario", "LAROCA_DB_USUARIO",
            "root");
    private static final String PASSWORD = valor("laroca.db.password", "LAROCA_DB_PASSWORD",
            "011125");

    // Devuelve null si no se pudo conectar; quien llama debe revisarlo.
    public static Connection conectar() {
        Connection conexion = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            conexion = DriverManager.getConnection(URL, USUARIO, PASSWORD);

        } catch (ClassNotFoundException e) {
            System.out.println("No se encontró el driver de MySQL.");
            e.printStackTrace();

        } catch (SQLException e) {
            System.out.println("Error al conectar con la base de datos.");
            e.printStackTrace();
        }

        return conexion;
    }

    private static String valor(String propiedad, String variable, String porOmision) {
        String valor = System.getProperty(propiedad);

        if (valor == null || valor.isBlank()) {
            valor = System.getenv(variable);
        }

        return valor == null || valor.isBlank() ? porOmision : valor;
    }
}
