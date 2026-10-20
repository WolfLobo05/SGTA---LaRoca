package com.laroca.dao;

import com.laroca.conexion.Conexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class EmpleadoDAO {

    public boolean validarUsuario(String usuario, String password) {
        // Valida usuario, contraseña y que el empleado esté ACTIVO
        String sql = "SELECT id_empleado FROM Empleados WHERE usuario = ? AND password_hash = ? AND (estado = 'ACTIVO' OR estado IS NULL)";

        try (Connection con = Conexion.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            if (con == null) return false;

            ps.setString(1, usuario);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // Retorna true si encontró al empleado
            }

        } catch (SQLException e) {
            System.err.println("Error al validar login: " + e.getMessage());
            return false;
        }
    }
}