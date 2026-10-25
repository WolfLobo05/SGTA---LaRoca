package com.laroca.dao;

import com.laroca.conexion.Conexion;
import com.laroca.dao.UsuarioDAO;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class EmpleadoDAO {

    public boolean validarUsuario(String usuario, String password) {

        String sql = "SELECT password_hash FROM Empleados "
                + "WHERE usuario = ? "
                + "AND (UPPER(estado) = 'ACTIVO' OR estado IS NULL)";

        try (Connection con = Conexion.conectar()) {

            if (con == null) {
                return false;
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, usuario);

                try (ResultSet rs = ps.executeQuery()) {

                    if (!rs.next()) {
                        return false;
                    }

                    String passwordGuardada = rs.getString("password_hash");

                    if (passwordGuardada == null) {
                        return false;
                    }

                    // Compatibilidad con contraseñas que ya estaban guardadas
                    // como texto normal.
                    if (passwordGuardada.equals(password)) {
                        return true;
                    }

                    // Compatibilidad con contraseñas guardadas mediante SHA-256.
                    String passwordHash = UsuarioDAO.hashPassword(password);

                    return MessageDigest.isEqual(
                            passwordGuardada.getBytes(StandardCharsets.UTF_8),
                            passwordHash.getBytes(StandardCharsets.UTF_8));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al validar login: " + e.getMessage());
            return false;
        }
    }
}