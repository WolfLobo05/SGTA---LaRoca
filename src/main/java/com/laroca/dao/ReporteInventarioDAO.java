package com.laroca.dao;

import com.laroca.conexion.Conexion;
import com.laroca.modelo.FilaInventario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Reporte de Inventario: existencias actuales de los productos activos.
public class ReporteInventarioDAO {

    private static final String SQL_BASE =
            "SELECT p.id_producto, p.codigo_barras, p.nombre, p.precio, "
          + "p.punto_reorden, p.existencia, "
          + "c.nombre AS categoria, "
          + "COALESCE(NULLIF(TRIM(u.abreviatura), ''), u.nombre) AS unidad "
          + "FROM Productos p "
          + "INNER JOIN Categoria c ON p.id_categoria = c.id_categoria "
          + "INNER JOIN Unidad_Medida u ON p.id_unidad = u.id_unidad "
          + "WHERE UPPER(COALESCE(TRIM(p.estado), '')) IN ('', 'ACTIVO') ";

    private String mensajeError;

    public String getMensajeError() {
        return mensajeError;
    }

    // idCategoria null = todas las categorías.
    // Devuelve null si falla (ver getMensajeError()).
    public List<FilaInventario> consultar(Integer idCategoria) {

        mensajeError = null;

        String sql = SQL_BASE
                + (idCategoria != null ? "AND p.id_categoria = ? " : "")
                + "ORDER BY p.codigo_barras, p.id_producto";

        List<FilaInventario> filas = new ArrayList<>();

        try (Connection conexion = Conexion.conectar()) {

            if (conexion == null) {
                mensajeError =
                        "No se pudo conectar con la base de datos.";
                return null;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {

                if (idCategoria != null) {
                    ps.setInt(1, idCategoria);
                }

                try (ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {

                        int puntoReorden =
                                rs.getInt("punto_reorden");

                        Integer reorden = rs.wasNull()
                                ? null
                                : puntoReorden;

                        filas.add(new FilaInventario(
                                rs.getInt("id_producto"),
                                rs.getString("codigo_barras"),
                                rs.getString("nombre"),
                                rs.getString("categoria"),
                                rs.getString("unidad"),
                                rs.getInt("existencia"),
                                rs.getBigDecimal("precio"),
                                reorden));
                    }
                }
            }

        } catch (SQLException e) {

            System.err.println(
                    "Error al consultar el reporte de inventario:");

            e.printStackTrace();

            mensajeError =
                    "No se pudo consultar el inventario: "
                    + e.getMessage();

            return null;
        }

        return filas;
    }
}
