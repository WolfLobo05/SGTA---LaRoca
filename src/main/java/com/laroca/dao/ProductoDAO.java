package com.laroca.dao;

import com.laroca.conexion.Conexion;
import com.laroca.modelo.Producto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAO {

    public List<Producto> listar() {
        List<Producto> productos = new ArrayList<>();

        String sql = "SELECT p.id_producto, p.id_unidad, p.id_categoria, "
                   + "p.codigo_barras, p.nombre, p.precio, "
                   + "u.nombre AS nombre_unidad, c.nombre AS nombre_categoria "
                   + "FROM Productos p "
                   + "INNER JOIN Unidad_Medida u ON p.id_unidad = u.id_unidad "
                   + "INNER JOIN Categoria c ON p.id_categoria = c.id_categoria "
                   + "ORDER BY p.id_producto";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                return productos;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    Producto producto = new Producto();
                    producto.setIdProducto(rs.getInt("id_producto"));
                    producto.setIdUnidad(rs.getInt("id_unidad"));
                    producto.setIdCategoria(rs.getInt("id_categoria"));
                    producto.setCodigoBarras(rs.getString("codigo_barras"));
                    producto.setNombre(rs.getString("nombre"));
                    producto.setPrecio(rs.getBigDecimal("precio"));
                    producto.setNombreUnidad(rs.getString("nombre_unidad"));
                    producto.setNombreCategoria(rs.getString("nombre_categoria"));
                    productos.add(producto);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar productos:");
            e.printStackTrace();
        }

        return productos;
    }

    public int obtenerSiguienteId() {
        String sql = "SELECT COALESCE(MAX(id_producto), 0) + 1 AS siguiente "
                   + "FROM Productos";

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

    // Comprueba si existe el código de barras, ignorando
    // espacios al inicio o al final.
    public boolean existeCodigoBarras(String codigo, int idExcluir) {
        String sql = "SELECT COUNT(*) FROM Productos "
                   + "WHERE TRIM(codigo_barras) = TRIM(?) "
                   + "AND id_producto <> ?";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                return true; // No permitir guardar si falla la conexión.
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setString(1, codigo);
                ps.setInt(2, idExcluir);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1) > 0;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al verificar el código de barras:");
            e.printStackTrace();
            return true; // Por seguridad, impedir el guardado.
        }

        return true;
    }

    // punto_reorden no se maneja en esta pantalla; queda en NULL.
    public boolean insertar(Producto producto) {
        String sql = "INSERT INTO Productos "
                   + "(id_producto, id_unidad, id_categoria, codigo_barras, nombre, precio) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                return false;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setInt(1, producto.getIdProducto());
                ps.setInt(2, producto.getIdUnidad());
                ps.setInt(3, producto.getIdCategoria());
                ps.setString(4, producto.getCodigoBarras());
                ps.setString(5, producto.getNombre());
                ps.setBigDecimal(6, producto.getPrecio());

                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al insertar producto:");
            e.printStackTrace();
            return false;
        }
    }

    // punto_reorden no se modifica.
    public boolean actualizar(Producto producto) {
        String sql = "UPDATE Productos "
                   + "SET id_unidad = ?, id_categoria = ?, codigo_barras = ?, "
                   + "nombre = ?, precio = ? "
                   + "WHERE id_producto = ?";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                return false;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setInt(1, producto.getIdUnidad());
                ps.setInt(2, producto.getIdCategoria());
                ps.setString(3, producto.getCodigoBarras());
                ps.setString(4, producto.getNombre());
                ps.setBigDecimal(5, producto.getPrecio());
                ps.setInt(6, producto.getIdProducto());

                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al actualizar producto:");
            e.printStackTrace();
            return false;
        }
    }

    public boolean eliminar(int idProducto) {
        String sql = "DELETE FROM Productos WHERE id_producto = ?";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                return false;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setInt(1, idProducto);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al eliminar producto:");
            e.printStackTrace();
            return false;
        }
    }
}
