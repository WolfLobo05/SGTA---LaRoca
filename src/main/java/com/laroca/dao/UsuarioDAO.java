package com.laroca.dao;

import com.laroca.conexion.Conexion;
import com.laroca.modelo.Usuario;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

// Opera sobre la tabla Empleados.
public class UsuarioDAO {

    // No se carga password_hash para no exponerlo en la pantalla.
    public List<Usuario> listar() {
        List<Usuario> usuarios = new ArrayList<>();

        String sql = "SELECT e.id_empleado, e.id_rol, e.estado, e.usuario, "
                   + "e.nombre, e.apellido_paterno, e.apellido_materno, "
                   + "e.telefono, e.correo, r.nombre AS nombre_rol "
                   + "FROM Empleados e "
                   + "INNER JOIN Roles r ON e.id_rol = r.id_rol "
                   + "ORDER BY e.id_empleado";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                return usuarios;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    Usuario usuario = new Usuario();
                    usuario.setIdUsuario(rs.getInt("id_empleado"));
                    usuario.setIdRol(rs.getInt("id_rol"));
                    usuario.setEstado(rs.getString("estado"));
                    usuario.setUsuario(rs.getString("usuario"));
                    usuario.setNombre(rs.getString("nombre"));
                    usuario.setApellidoPaterno(rs.getString("apellido_paterno"));
                    usuario.setApellidoMaterno(rs.getString("apellido_materno"));
                    usuario.setTelefono(rs.getString("telefono"));
                    usuario.setCorreo(rs.getString("correo"));
                    usuario.setNombreRol(rs.getString("nombre_rol"));
                    usuarios.add(usuario);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar usuarios:");
            e.printStackTrace();
        }

        return usuarios;
    }

    public int obtenerSiguienteId() {
        String sql = "SELECT COALESCE(MAX(id_empleado), 0) + 1 AS siguiente "
                   + "FROM Empleados";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                return -1;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("siguiente");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener el siguiente ID:");
            e.printStackTrace();
        }

        return -1;
    }

    // Comprueba si existe el usuario, ignorando mayúsculas,
    // minúsculas y espacios al inicio o al final.
    public boolean existeUsuario(String usuario, int idExcluir) {
        String sql = "SELECT COUNT(*) FROM Empleados "
                   + "WHERE LOWER(TRIM(usuario)) = LOWER(TRIM(?)) "
                   + "AND id_empleado <> ?";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                return true; // No permitir guardar si falla la conexión.
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setString(1, usuario);
                ps.setInt(2, idExcluir);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1) > 0;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al verificar el nombre de usuario:");
            e.printStackTrace();
            return true; // Por seguridad, impedir el guardado.
        }

        return true;
    }

    public boolean insertar(Usuario usuario) {
        String sql = "INSERT INTO Empleados "
                   + "(id_empleado, id_rol, estado, usuario, password_hash, nombre, "
                   + "apellido_paterno, apellido_materno, telefono, correo) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                return false;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setInt(1, usuario.getIdUsuario());
                ps.setInt(2, usuario.getIdRol());
                ps.setString(3, usuario.getEstado());
                ps.setString(4, usuario.getUsuario());
                ps.setString(5, usuario.getPasswordHash());
                ps.setString(6, usuario.getNombre());
                ps.setString(7, usuario.getApellidoPaterno());
                ps.setString(8, usuario.getApellidoMaterno());
                ps.setString(9, usuario.getTelefono());
                ps.setString(10, usuario.getCorreo());

                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al insertar usuario:");
            e.printStackTrace();
            return false;
        }
    }

    // Si passwordHash es null se conserva la contraseña actual.
    public boolean actualizar(Usuario usuario) {
        boolean cambiarPassword = usuario.getPasswordHash() != null;

        String sql = "UPDATE Empleados "
                   + "SET id_rol = ?, estado = ?, usuario = ?, nombre = ?, "
                   + "apellido_paterno = ?, apellido_materno = ?, "
                   + "telefono = ?, correo = ? "
                   + (cambiarPassword ? ", password_hash = ? " : "")
                   + "WHERE id_empleado = ?";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                return false;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setInt(1, usuario.getIdRol());
                ps.setString(2, usuario.getEstado());
                ps.setString(3, usuario.getUsuario());
                ps.setString(4, usuario.getNombre());
                ps.setString(5, usuario.getApellidoPaterno());
                ps.setString(6, usuario.getApellidoMaterno());
                ps.setString(7, usuario.getTelefono());
                ps.setString(8, usuario.getCorreo());

                if (cambiarPassword) {
                    ps.setString(9, usuario.getPasswordHash());
                    ps.setInt(10, usuario.getIdUsuario());
                } else {
                    ps.setInt(9, usuario.getIdUsuario());
                }

                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al actualizar usuario:");
            e.printStackTrace();
            return false;
        }
    }

    public boolean eliminar(int idUsuario) {
        String sql = "DELETE FROM Empleados WHERE id_empleado = ?";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                return false;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setInt(1, idUsuario);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al eliminar usuario:");
            e.printStackTrace();
            return false;
        }
    }

    // Genera el hash SHA-256 de la contraseña en hexadecimal.
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible.", e);
        }
    }
}
