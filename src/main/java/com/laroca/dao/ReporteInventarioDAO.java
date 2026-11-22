package com.laroca.dao;

import com.laroca.conexion.Conexion;
import com.laroca.modelo.FilaInventario;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReporteInventarioDAO {

    private static final String ACTIVO =
            "UPPER(COALESCE(TRIM(h.estado), '')) IN ('', 'ACTIVO')";

    private static final String SQL_BASE =
            "SELECT p.id_producto, p.codigo_barras, p.nombre, p.precio, "
          + "p.punto_reorden, p.existencia, "
          + "c.nombre AS categoria, "
          + "COALESCE(NULLIF(TRIM(u.abreviatura), ''), u.nombre) AS unidad, "
          + "COALESCE(com.cantidad, 0) AS compras, "
          + "COALESCE(dcl.cantidad, 0) AS devoluciones_clientes, "
          + "COALESCE(ven.cantidad, 0) AS ventas, "
          + "COALESCE(mer.cantidad, 0) AS mermas, "
          + "COALESCE(dpr.cantidad, 0) AS devoluciones_proveedores "
          + "FROM Productos p "
          + "INNER JOIN Categoria c ON p.id_categoria = c.id_categoria "
          + "INNER JOIN Unidad_Medida u ON p.id_unidad = u.id_unidad "

          + "LEFT JOIN (SELECT d.id_producto, SUM(d.cantidad) AS cantidad "
          + "FROM detalle_compras d "
          + "INNER JOIN Compras h ON d.id_compras = h.id_compras "
          + "WHERE h.fecha >= ? AND h.fecha < ? AND " + ACTIVO + " "
          + "GROUP BY d.id_producto) com ON com.id_producto = p.id_producto "

          + "LEFT JOIN (SELECT d.id_producto, SUM(d.cantidad) AS cantidad "
          + "FROM detalle_devolucion_clientes d "
          + "INNER JOIN Devolucion_Clientes h "
          + "ON d.id_devolucion_cliente = h.id_devolucion_cliente "
          + "WHERE h.fecha >= ? AND h.fecha < ? AND " + ACTIVO + " "
          + "GROUP BY d.id_producto) dcl ON dcl.id_producto = p.id_producto "

          + "LEFT JOIN (SELECT d.id_producto, SUM(d.cantidad) AS cantidad "
          + "FROM detalle_ventas d "
          + "INNER JOIN Ventas h ON d.id_venta = h.id_venta "
          + "WHERE h.fecha >= ? AND h.fecha < ? AND " + ACTIVO + " "
          + "GROUP BY d.id_producto) ven ON ven.id_producto = p.id_producto "

          + "LEFT JOIN (SELECT d.id_producto, SUM(d.cantidad) AS cantidad "
          + "FROM detalle_mermas d "
          + "INNER JOIN Mermas h ON d.id_merma = h.id_merma "
          + "WHERE h.fecha >= ? AND h.fecha < ? AND " + ACTIVO + " "
          + "GROUP BY d.id_producto) mer ON mer.id_producto = p.id_producto "

          + "LEFT JOIN (SELECT d.id_producto, SUM(d.cantidad) AS cantidad "
          + "FROM detalle_devolucion_proveedores d "
          + "INNER JOIN Devolucion_Proveedores h "
          + "ON d.id_devolucion_proveedor = h.id_devolucion_proveedor "
          + "WHERE h.fecha >= ? AND h.fecha < ? AND " + ACTIVO + " "
          + "GROUP BY d.id_producto) dpr ON dpr.id_producto = p.id_producto "

          + "WHERE UPPER(COALESCE(TRIM(p.estado), '')) IN ('', 'ACTIVO') ";

    private static final String SQL_PRIMER_MOVIMIENTO =
            "SELECT MIN(fecha) AS primera_fecha FROM ("

          + "SELECT MIN(h.fecha) AS fecha "
          + "FROM Compras h WHERE " + ACTIVO + " "

          + "UNION ALL "

          + "SELECT MIN(h.fecha) AS fecha "
          + "FROM Devolucion_Clientes h WHERE " + ACTIVO + " "

          + "UNION ALL "

          + "SELECT MIN(h.fecha) AS fecha "
          + "FROM Ventas h WHERE " + ACTIVO + " "

          + "UNION ALL "

          + "SELECT MIN(h.fecha) AS fecha "
          + "FROM Mermas h WHERE " + ACTIVO + " "

          + "UNION ALL "

          + "SELECT MIN(h.fecha) AS fecha "
          + "FROM Devolucion_Proveedores h WHERE " + ACTIVO + " "

          + ") movimientos";

    private String mensajeError;

    public String getMensajeError() {
        return mensajeError;
    }

    /**
     * Obtiene la fecha del primer movimiento activo registrado
     * en cualquiera de las cinco tablas de movimientos.
     *
     * Devuelve null si todavía no hay movimientos o si ocurre un error.
     * En caso de error, consultar getMensajeError().
     */
    public LocalDate obtenerFechaPrimerMovimiento() {
        mensajeError = null;

        try (Connection conexion = Conexion.conectar()) {

            if (conexion == null) {
                mensajeError = "No se pudo conectar con la base de datos.";
                return null;
            }

            try (PreparedStatement ps =
                         conexion.prepareStatement(SQL_PRIMER_MOVIMIENTO);
                 ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    Timestamp fecha = rs.getTimestamp("primera_fecha");

                    if (fecha != null) {
                        return fecha.toLocalDateTime().toLocalDate();
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println(
                    "Error al obtener el primer movimiento histórico:");
            e.printStackTrace();

            mensajeError =
                    "No se pudo consultar la fecha del primer movimiento: "
                    + e.getMessage();

            return null;
        }

        return null;
    }

    // idCategoria null = todas las categorías.
    // Devuelve null si falla (ver getMensajeError()).
    public List<FilaInventario> consultar(
            LocalDate desde,
            LocalDate hasta,
            Integer idCategoria) {

        mensajeError = null;

        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            mensajeError = "El periodo no es válido.";
            return null;
        }

        String sql = SQL_BASE
                + (idCategoria != null ? "AND p.id_categoria = ? " : "")
                + "ORDER BY p.codigo_barras, p.id_producto";

        Date inicio = Date.valueOf(desde);
        Date fin = Date.valueOf(hasta.plusDays(1));

        Timestamp inicioHora =
                Timestamp.valueOf(desde.atStartOfDay());

        Timestamp finHora =
                Timestamp.valueOf(hasta.plusDays(1).atStartOfDay());

        List<FilaInventario> filas = new ArrayList<>();

        try (Connection conexion = Conexion.conectar()) {

            if (conexion == null) {
                mensajeError =
                        "No se pudo conectar con la base de datos.";
                return null;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {

                int i = 1;

                ps.setDate(i++, inicio);
                ps.setDate(i++, fin);

                ps.setDate(i++, inicio);
                ps.setDate(i++, fin);

                ps.setTimestamp(i++, inicioHora);
                ps.setTimestamp(i++, finHora);

                ps.setDate(i++, inicio);
                ps.setDate(i++, fin);

                ps.setDate(i++, inicio);
                ps.setDate(i++, fin);

                if (idCategoria != null) {
                    ps.setInt(i, idCategoria);
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
                                rs.getInt("compras"),
                                rs.getInt("devoluciones_clientes"),
                                rs.getInt("ventas"),
                                rs.getInt("mermas"),
                                rs.getInt("devoluciones_proveedores"),
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
