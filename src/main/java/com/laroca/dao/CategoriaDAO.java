package com.laroca.dao;

import com.laroca.conexion.Conexion;
import com.laroca.modelo.Estados;
import com.laroca.modelo.Categoria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CategoriaDAO {

    // Un registro es inactivo si su estado no es Activo (NULL cuenta como Activo).
    private static final String ES_INACTIVO =
            "UPPER(COALESCE(TRIM(estado), '')) NOT IN ('', 'ACTIVO')";

    public List<Categoria> listar() {
        List<Categoria> categorias = new ArrayList<>();

        String sql = "SELECT id_categoria, estado, nombre, descripcion "
                   + "FROM Categoria ORDER BY id_categoria";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                return categorias;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    Categoria categoria = new Categoria();

                    categoria.setIdCategoria(rs.getInt("id_categoria"));
                    categoria.setEstado(rs.getString("estado"));
                    categoria.setNombre(rs.getString("nombre"));
                    categoria.setDescripcion(rs.getString("descripcion"));

                    categorias.add(categoria);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al listar categorías:");
            e.printStackTrace();
        }

        return categorias;
    }

    public int obtenerSiguienteId() {
        String sql = "SELECT COALESCE(MAX(id_categoria), 0) + 1 "
                   + "AS siguiente FROM Categoria";

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
            System.err.println("Error al obtener el ID de categoría:");
            e.printStackTrace();
        }

        return -1;
    }

    // Comprueba si existe el nombre, ignorando mayúsculas y espacios
    // exteriores. excluirId permite editar el registro actual.
    public boolean existeNombre(String nombre, int excluirId) {
        String sql = "SELECT 1 FROM Categoria "
                   + "WHERE LOWER(TRIM(nombre)) = LOWER(TRIM(?)) "
                   + "AND id_categoria <> ? LIMIT 1";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                return true; // No permitir guardar si no se pudo comprobar.
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setString(1, nombre);
                ps.setInt(2, excluirId);

                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al comprobar el nombre de categoría:");
            e.printStackTrace();
            return true; // Fallar de forma segura.
        }
    }

    // Indica si el valor duplicado pertenece a un registro inactivo.
    public boolean existeNombreInactivo(String nombre, int idExcluir) {
        String sql = "SELECT COUNT(*) FROM Categoria "
                   + "WHERE LOWER(TRIM(nombre)) = LOWER(TRIM(?)) "
                   + "AND id_categoria <> ? "
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
            System.err.println("Error al comprobar el estado de la categoría:");
            e.printStackTrace();
            return false;
        }
    }

    public boolean insertar(Categoria categoria) {
        if (existeNombre(categoria.getNombre(), 0)) {
            return false;
        }

        String sql = "INSERT INTO Categoria "
                   + "(id_categoria, estado, nombre, descripcion) "
                   + "VALUES (?, ?, ?, ?)";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                return false;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setInt(1, categoria.getIdCategoria());
                ps.setString(2, categoria.getEstado());
                ps.setString(3, categoria.getNombre());
                ps.setString(4, categoria.getDescripcion());

                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al insertar categoría:");
            e.printStackTrace();
            return false;
        }
    }

    public boolean actualizar(Categoria categoria) {
        if (existeNombre(
                categoria.getNombre(), categoria.getIdCategoria())) {
            return false;
        }

        String sql = "UPDATE Categoria "
                   + "SET nombre = ?, descripcion = ? "
                   + "WHERE id_categoria = ?";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                return false;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setString(1, categoria.getNombre());
                ps.setString(2, categoria.getDescripcion());
                ps.setInt(3, categoria.getIdCategoria());

                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al actualizar categoría:");
            e.printStackTrace();
            return false;
        }
    }

    // Ya no se borran registros: solo se cambia el estado.
    public boolean desactivar(int idCategoria) {
        return cambiarEstado(idCategoria, Estados.INACTIVO);
    }

    public boolean reactivar(int idCategoria) {
        return cambiarEstado(idCategoria, Estados.ACTIVO);
    }

    private boolean cambiarEstado(int idCategoria, String estado) {
        String sql = "UPDATE Categoria SET estado = ? WHERE id_categoria = ?";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                return false;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setString(1, estado);
                ps.setInt(2, idCategoria);
                return ps.executeUpdate() > 0;
            }
        } catch (Exception e) {
            System.err.println("Error al cambiar el estado de la categoría:");
            e.printStackTrace();
            return false;
        }
    }
}
