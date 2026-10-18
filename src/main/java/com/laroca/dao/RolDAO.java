package com.laroca.dao;

import com.laroca.conexion.Conexion;
import com.laroca.modelo.Rol;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RolDAO {

    public List<Rol> listar() {
        List<Rol> roles = new ArrayList<>();

        String sql = "SELECT id_rol, estado, nombre, descripcion "
                   + "FROM Roles ORDER BY id_rol";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                return roles;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    Rol rol = new Rol();
                    rol.setIdRol(rs.getInt("id_rol"));
                    rol.setEstado(rs.getString("estado"));
                    rol.setNombre(rs.getString("nombre"));
                    rol.setDescripcion(rs.getString("descripcion"));
                    roles.add(rol);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar roles:");
            e.printStackTrace();
        }

        return roles;
    }

    public int obtenerSiguienteId() {
        String sql = "SELECT COALESCE(MAX(id_rol), 0) + 1 AS siguiente "
                   + "FROM Roles";

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

    // Comprueba si existe el nombre, ignorando mayúsculas,
    // minúsculas y espacios al inicio o al final.
    public boolean existeNombre(String nombre, int idExcluir) {
        String sql = "SELECT COUNT(*) FROM Roles "
                   + "WHERE LOWER(TRIM(nombre)) = LOWER(TRIM(?)) "
                   + "AND id_rol <> ?";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                return true; // No permitir guardar si falla la conexión.
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setString(1, nombre);
                ps.setInt(2, idExcluir);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1) > 0;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al verificar el nombre del rol:");
            e.printStackTrace();
            return true; // Por seguridad, impedir el guardado.
        }

        return true;
    }

    public boolean insertar(Rol rol) {
        String sql = "INSERT INTO Roles "
                   + "(id_rol, estado, nombre, descripcion) "
                   + "VALUES (?, ?, ?, ?)";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                return false;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setInt(1, rol.getIdRol());
                ps.setString(2, rol.getEstado());
                ps.setString(3, rol.getNombre());
                ps.setString(4, rol.getDescripcion());

                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al insertar rol:");
            e.printStackTrace();
            return false;
        }
    }

    public boolean actualizar(Rol rol) {
        String sql = "UPDATE Roles "
                   + "SET estado = ?, nombre = ?, descripcion = ? "
                   + "WHERE id_rol = ?";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                return false;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setString(1, rol.getEstado());
                ps.setString(2, rol.getNombre());
                ps.setString(3, rol.getDescripcion());
                ps.setInt(4, rol.getIdRol());

                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al actualizar rol:");
            e.printStackTrace();
            return false;
        }
    }

    public boolean eliminar(int idRol) {
        String sql = "DELETE FROM Roles WHERE id_rol = ?";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                return false;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setInt(1, idRol);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al eliminar rol:");
            e.printStackTrace();
            return false;
        }
    }
}