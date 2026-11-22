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

    // Devuelve el empleado si el usuario y la contraseña son correctos.
    public Usuario iniciarSesion(String usuario, String password) {

        String sql = "SELECT e.id_empleado, e.id_rol, e.usuario, e.password_hash, e.nombre, "
                + "e.apellido_paterno, e.apellido_materno, r.nombre AS nombre_rol "
                + "FROM Empleados e INNER JOIN Roles r ON r.id_rol = e.id_rol "
                + "WHERE e.usuario = ? "
                + "AND (UPPER(e.estado) = 'ACTIVO' OR e.estado IS NULL) "
                + "AND UPPER(r.estado) = 'ACTIVO'";

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

                    // Compatibilidad con contraseñas guardadas como texto normal.
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
                    empleado.setIdRol(rs.getInt("id_rol"));
                    empleado.setNombreRol(rs.getString("nombre_rol"));
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
