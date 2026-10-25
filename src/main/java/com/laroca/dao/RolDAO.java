package com.laroca.dao;

import com.laroca.conexion.Conexion;
import com.laroca.modelo.Estados;
import com.laroca.modelo.Rol;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RolDAO {

    // Un registro es inactivo si su estado no es Activo (NULL cuenta como Activo).
    private static final String ES_INACTIVO =
            "UPPER(COALESCE(TRIM(estado), '')) NOT IN ('', 'ACTIVO')";

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

    // Indica si el valor duplicado pertenece a un registro inactivo.
    public boolean existeNombreInactivo(String nombre, int idExcluir) {
        String sql = "SELECT COUNT(*) FROM Roles "
                   + "WHERE LOWER(TRIM(nombre)) = LOWER(TRIM(?)) "
                   + "AND id_rol <> ? "
                   + "AND " + ES_INACTIVO;

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                return false;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setString(1, nombre);
                ps.setInt(2, idExcluir);

                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            }
        } catch (Exception e) {
            System.err.println("Error al comprobar el estado de el rol:");
            e.printStackTrace();
            return false;
        }
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
                   + "SET nombre = ?, descripcion = ? "
                   + "WHERE id_rol = ?";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                return false;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setString(1, rol.getNombre());
                ps.setString(2, rol.getDescripcion());
                ps.setInt(3, rol.getIdRol());

                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al actualizar rol:");
            e.printStackTrace();
            return false;
        }
    }

    // Ya no se borran registros: solo se cambia el estado.
    public boolean desactivar(int idRol) {
        return cambiarEstado(idRol, Estados.INACTIVO);
    }

    public boolean reactivar(int idRol) {
        return cambiarEstado(idRol, Estados.ACTIVO);
    }

    private boolean cambiarEstado(int idRol, String estado) {
        String sql = "UPDATE Roles SET estado = ? WHERE id_rol = ?";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                return false;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setString(1, estado);
                ps.setInt(2, idRol);
                return ps.executeUpdate() > 0;
            }
        } catch (Exception e) {
            System.err.println("Error al cambiar el estado de el rol:");
            e.printStackTrace();
            return false;
        }
    }
}