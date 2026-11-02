package com.laroca.dao;

import com.laroca.conexion.Conexion;
import com.laroca.modelo.Usuario;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class EmpleadoDAO {

    public boolean validarUsuario(String usuario, String password) {
        return iniciarSesion(usuario, password) != null;
    }

    // Devuelve el empleado (id y nombre) si el usuario y la contraseña
    // son correctos; null en cualquier otro caso.
    public Usuario iniciarSesion(String usuario, String password) {

        String sql = "SELECT id_empleado, usuario, password_hash, nombre, "
                + "apellido_paterno, apellido_materno FROM Empleados "
                + "WHERE usuario = ? "
                + "AND (UPPER(estado) = 'ACTIVO' OR estado IS NULL)";

        try (Connection con = Conexion.conectar()) {

            if (con == null) {
                return null;
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, usuario);

                try (ResultSet rs = ps.executeQuery()) {

                    if (!rs.next()) {
                        return null;
                    }

                    String passwordGuardada = rs.getString("password_hash");

                    if (passwordGuardada == null) {
                        return null;
                    }

                    // Compatibilidad con contraseñas que ya estaban guardadas
                    // como texto normal.
                    boolean valida = passwordGuardada.equals(password);

                    // Compatibilidad con contraseñas guardadas mediante SHA-256.
                    if (!valida) {
                        String passwordHash = UsuarioDAO.hashPassword(password);

                        valida = MessageDigest.isEqual(
                                passwordGuardada.getBytes(StandardCharsets.UTF_8),
                                passwordHash.getBytes(StandardCharsets.UTF_8));
                    }

                    if (!valida) {
                        return null;
                    }

                    Usuario empleado = new Usuario();
                    empleado.setIdUsuario(rs.getInt("id_empleado"));
                    empleado.setUsuario(rs.getString("usuario"));
                    empleado.setNombre(rs.getString("nombre"));
                    empleado.setApellidoPaterno(rs.getString("apellido_paterno"));
                    empleado.setApellidoMaterno(rs.getString("apellido_materno"));
                    return empleado;
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al validar login: " + e.getMessage());
            return null;
        }
    }
}
