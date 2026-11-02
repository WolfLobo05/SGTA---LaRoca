package com.laroca.dao;

import com.laroca.conexion.Conexion;
import com.laroca.modelo.DetalleVenta;
import com.laroca.modelo.Estados;
import com.laroca.modelo.Venta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class VentaDAO {

    // Venta con el usuario del empleado y los nombres de cliente y método de pago.
    private static final String CONSULTA_VENTA =
            "SELECT v.id_venta, v.id_empleado, v.id_cliente, v.id_metodo, "
          + "v.fecha, v.total, v.monto_pagado, v.cambio, v.estado, "
          + "e.usuario AS nombre_empleado, "
          + "CONCAT_WS(' ', NULLIF(TRIM(c.nombre), ''), "
          + "NULLIF(TRIM(c.apellido_paterno), ''), "
          + "NULLIF(TRIM(c.apellido_materno), '')) AS nombre_cliente, "
          + "m.nombre AS nombre_metodo "
          + "FROM Ventas v "
          + "INNER JOIN Empleados e ON v.id_empleado = e.id_empleado "
          + "INNER JOIN Clientes c ON v.id_cliente = c.id_cliente "
          + "INNER JOIN Metodos_Pago m ON v.id_metodo = m.id_metodo ";

    // Motivo del último fallo de registrar() o cancelar(), para mostrarlo.
    private String mensajeError;

    public String getMensajeError() {
        return mensajeError;
    }

    public List<Venta> listar() {
        List<Venta> ventas = new ArrayList<>();

        String sql = CONSULTA_VENTA + "ORDER BY v.id_venta";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                return ventas;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    ventas.add(leerVenta(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar ventas:");
            e.printStackTrace();
        }

        return ventas;
    }

    public Venta obtenerVenta(int idVenta) {
        String sql = CONSULTA_VENTA + "WHERE v.id_venta = ?";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                return null;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setInt(1, idVenta);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return leerVenta(rs);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar la venta:");
            e.printStackTrace();
        }

        return null;
    }

    public List<DetalleVenta> listarDetalle(int idVenta) {
        List<DetalleVenta> detalles = new ArrayList<>();

        String sql = "SELECT d.id_producto, p.nombre, d.cantidad, d.precio_unitario "
                   + "FROM detalle_ventas d "
                   + "INNER JOIN Productos p ON d.id_producto = p.id_producto "
                   + "WHERE d.id_venta = ? "
                   + "ORDER BY d.id_detalle_venta";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                return detalles;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setInt(1, idVenta);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        detalles.add(new DetalleVenta(
                                rs.getInt("id_producto"),
                                rs.getString("nombre"),
                                rs.getInt("cantidad"),
                                rs.getBigDecimal("precio_unitario")));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar el detalle de la venta:");
            e.printStackTrace();
        }

        return detalles;
    }

    public int obtenerSiguienteId() {
        String sql = "SELECT COALESCE(MAX(id_venta), 0) + 1 AS siguiente "
                   + "FROM Ventas";

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

    // Registra la venta, su detalle y descuenta la existencia en una sola
    // transacción. Si algún producto no tiene existencia suficiente no se
    // guarda nada. Devuelve el id de la venta, o -1 (ver getMensajeError()).
    public int registrar(Venta venta, List<DetalleVenta> detalles) {
        mensajeError = null;

        if (detalles == null || detalles.isEmpty()) {
            mensajeError = "La venta no tiene productos.";
            return -1;
        }

        String sqlVenta = "INSERT INTO Ventas "
                        + "(id_venta, id_empleado, id_cliente, id_metodo, fecha, "
                        + "total, monto_pagado, cambio, estado) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        String sqlDetalle = "INSERT INTO detalle_ventas "
                          + "(id_detalle_venta, id_venta, id_producto, cantidad, precio_unitario) "
                          + "VALUES (?, ?, ?, ?, ?)";

        String sqlExistencia = "UPDATE Productos "
                             + "SET existencia = existencia - ? "
                             + "WHERE id_producto = ? AND existencia >= ?";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                mensajeError = "No se pudo conectar con la base de datos.";
                return -1;
            }

            try {
                conexion.setAutoCommit(false);

                int idVenta = siguienteId(conexion,
                        "SELECT COALESCE(MAX(id_venta), 0) + 1 FROM Ventas FOR UPDATE");
                int idDetalle = siguienteId(conexion,
                        "SELECT COALESCE(MAX(id_detalle_venta), 0) + 1 FROM detalle_ventas FOR UPDATE");

                try (PreparedStatement ps = conexion.prepareStatement(sqlVenta)) {
                    ps.setInt(1, idVenta);
                    ps.setInt(2, venta.getIdEmpleado());
                    ps.setInt(3, venta.getIdCliente());
                    ps.setInt(4, venta.getIdMetodo());
                    ps.setTimestamp(5, Timestamp.valueOf(venta.getFecha()));
                    ps.setBigDecimal(6, venta.getTotal());
                    ps.setBigDecimal(7, venta.getMontoPagado());
                    ps.setBigDecimal(8, venta.getCambio());
                    ps.setString(9, Estados.ACTIVO);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conexion.prepareStatement(sqlDetalle)) {
                    for (DetalleVenta detalle : detalles) {
                        ps.setInt(1, idDetalle++);
                        ps.setInt(2, idVenta);
                        ps.setInt(3, detalle.getIdProducto());
                        ps.setInt(4, detalle.getCantidad());
                        ps.setBigDecimal(5, detalle.getPrecioUnitario());
                        ps.executeUpdate();
                    }
                }

                try (PreparedStatement ps = conexion.prepareStatement(sqlExistencia)) {
                    for (DetalleVenta detalle : detalles) {
                        ps.setInt(1, detalle.getCantidad());
                        ps.setInt(2, detalle.getIdProducto());
                        ps.setInt(3, detalle.getCantidad());

                        if (ps.executeUpdate() == 0) {
                            conexion.rollback();
                            mensajeError = "No hay existencia suficiente de \""
                                    + detalle.getNombreProducto()
                                    + "\". La venta no se registró.";
                            return -1;
                        }
                    }
                }

                conexion.commit();
                venta.setIdVenta(idVenta);
                return idVenta;

            } catch (SQLException e) {
                conexion.rollback();
                throw e;
            } finally {
                conexion.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("Error al registrar la venta:");
            e.printStackTrace();
            mensajeError = "No se pudo registrar la venta: " + e.getMessage();
            return -1;
        }
    }

    // Marca la venta como cancelada (Inactivo) y devuelve al inventario
    // las cantidades vendidas, en una sola transacción.
    public boolean cancelar(int idVenta) {
        mensajeError = null;

        String sqlVenta = "UPDATE Ventas SET estado = ? "
                        + "WHERE id_venta = ? "
                        + "AND UPPER(COALESCE(TRIM(estado), '')) IN ('', 'ACTIVO')";

        String sqlExistencia = "UPDATE Productos p "
                             + "INNER JOIN (SELECT id_producto, SUM(cantidad) AS cantidad "
                             + "FROM detalle_ventas WHERE id_venta = ? "
                             + "GROUP BY id_producto) d ON p.id_producto = d.id_producto "
                             + "SET p.existencia = p.existencia + d.cantidad";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                mensajeError = "No se pudo conectar con la base de datos.";
                return false;
            }

            try {
                conexion.setAutoCommit(false);

                try (PreparedStatement ps = conexion.prepareStatement(sqlVenta)) {
                    ps.setString(1, Estados.INACTIVO);
                    ps.setInt(2, idVenta);

                    if (ps.executeUpdate() == 0) {
                        conexion.rollback();
                        mensajeError = "La venta no existe o ya estaba cancelada.";
                        return false;
                    }
                }

                try (PreparedStatement ps = conexion.prepareStatement(sqlExistencia)) {
                    ps.setInt(1, idVenta);
                    ps.executeUpdate();
                }

                conexion.commit();
                return true;

            } catch (SQLException e) {
                conexion.rollback();
                throw e;
            } finally {
                conexion.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("Error al cancelar la venta:");
            e.printStackTrace();
            mensajeError = "No se pudo cancelar la venta: " + e.getMessage();
            return false;
        }
    }

    // Solo el cliente y el método de pago se pueden corregir.
    public boolean actualizarDatos(int idVenta, int idCliente, int idMetodo) {
        String sql = "UPDATE Ventas SET id_cliente = ?, id_metodo = ? "
                   + "WHERE id_venta = ?";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                return false;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setInt(1, idCliente);
                ps.setInt(2, idMetodo);
                ps.setInt(3, idVenta);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al actualizar la venta:");
            e.printStackTrace();
            return false;
        }
    }

    private int siguienteId(Connection conexion, String sql) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 1;
        }
    }

    private Venta leerVenta(ResultSet rs) throws SQLException {
        Venta venta = new Venta();
        venta.setIdVenta(rs.getInt("id_venta"));
        venta.setIdEmpleado(rs.getInt("id_empleado"));
        venta.setIdCliente(rs.getInt("id_cliente"));
        venta.setIdMetodo(rs.getInt("id_metodo"));

        Timestamp fecha = rs.getTimestamp("fecha");
        venta.setFecha(fecha == null ? null : fecha.toLocalDateTime());

        venta.setTotal(rs.getBigDecimal("total"));
        venta.setMontoPagado(rs.getBigDecimal("monto_pagado"));
        venta.setCambio(rs.getBigDecimal("cambio"));
        venta.setEstado(rs.getString("estado"));
        venta.setNombreEmpleado(rs.getString("nombre_empleado"));
        venta.setNombreCliente(rs.getString("nombre_cliente"));
        venta.setNombreMetodo(rs.getString("nombre_metodo"));
        return venta;
    }
}
