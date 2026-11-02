package com.laroca.dao;

import com.laroca.conexion.Conexion;
import com.laroca.modelo.Compra;
import com.laroca.modelo.DetalleCompra;
import com.laroca.modelo.Estados;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CompraDAO {

    // Compra con el nombre del proveedor y el usuario del empleado.
    private static final String CONSULTA_COMPRA =
            "SELECT c.id_compras, c.id_proveedor, c.id_empleado, "
          + "c.fecha, c.total, c.estado, "
          + "p.nombre AS nombre_proveedor, "
          + "e.usuario AS nombre_empleado "
          + "FROM Compras c "
          + "INNER JOIN Proveedores p ON c.id_proveedor = p.id_proveedor "
          + "INNER JOIN Empleados e ON c.id_empleado = e.id_empleado ";

    private static final String COMPRA_ACTIVA =
            "UPPER(COALESCE(TRIM(c.estado), '')) IN ('', 'ACTIVO')";

    // Motivo del último fallo de registrar() o cancelar(), para mostrarlo.
    private String mensajeError;

    public String getMensajeError() {
        return mensajeError;
    }

    public List<Compra> listar() {
        List<Compra> compras = new ArrayList<>();

        String sql = CONSULTA_COMPRA + "ORDER BY c.id_compras";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                return compras;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    compras.add(leerCompra(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar compras:");
            e.printStackTrace();
        }

        return compras;
    }

    public List<DetalleCompra> listarDetalle(int idCompra) {
        List<DetalleCompra> detalles = new ArrayList<>();

        String sql = "SELECT d.id_producto, p.nombre, d.cantidad, d.precio_compra "
                   + "FROM detalle_compras d "
                   + "INNER JOIN Productos p ON d.id_producto = p.id_producto "
                   + "WHERE d.id_compras = ? "
                   + "ORDER BY d.id_detalle_compra";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                return detalles;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setInt(1, idCompra);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        detalles.add(new DetalleCompra(
                                rs.getInt("id_producto"),
                                rs.getString("nombre"),
                                rs.getInt("cantidad"),
                                rs.getBigDecimal("precio_compra")));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar el detalle de la compra:");
            e.printStackTrace();
        }

        return detalles;
    }

    public int obtenerSiguienteId() {
        String sql = "SELECT COALESCE(MAX(id_compras), 0) + 1 AS siguiente "
                   + "FROM Compras";

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

    // Registra la compra, su detalle y suma la existencia en una sola
    // transacción. Devuelve el id de la compra, o -1 (ver getMensajeError()).
    public int registrar(Compra compra, List<DetalleCompra> detalles) {
        mensajeError = null;

        if (detalles == null || detalles.isEmpty()) {
            mensajeError = "La compra no tiene productos.";
            return -1;
        }

        String sqlCompra = "INSERT INTO Compras "
                         + "(id_compras, id_proveedor, id_empleado, fecha, total, estado) "
                         + "VALUES (?, ?, ?, ?, ?, ?)";

        String sqlDetalle = "INSERT INTO detalle_compras "
                          + "(id_detalle_compra, id_producto, id_compras, cantidad, precio_compra) "
                          + "VALUES (?, ?, ?, ?, ?)";

        String sqlExistencia = "UPDATE Productos "
                             + "SET existencia = existencia + ? "
                             + "WHERE id_producto = ?";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                mensajeError = "No se pudo conectar con la base de datos.";
                return -1;
            }

            try {
                conexion.setAutoCommit(false);

                int idCompra = siguienteId(conexion,
                        "SELECT COALESCE(MAX(id_compras), 0) + 1 FROM Compras FOR UPDATE");
                int idDetalle = siguienteId(conexion,
                        "SELECT COALESCE(MAX(id_detalle_compra), 0) + 1 FROM detalle_compras FOR UPDATE");

                try (PreparedStatement ps = conexion.prepareStatement(sqlCompra)) {
                    ps.setInt(1, idCompra);
                    ps.setInt(2, compra.getIdProveedor());
                    ps.setInt(3, compra.getIdEmpleado());
                    ps.setDate(4, Date.valueOf(compra.getFecha()));
                    ps.setBigDecimal(5, compra.getTotal());
                    ps.setString(6, Estados.ACTIVO);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conexion.prepareStatement(sqlDetalle)) {
                    for (DetalleCompra detalle : detalles) {
                        ps.setInt(1, idDetalle++);
                        ps.setInt(2, detalle.getIdProducto());
                        ps.setInt(3, idCompra);
                        ps.setInt(4, detalle.getCantidad());
                        ps.setBigDecimal(5, detalle.getPrecioCompra());
                        ps.executeUpdate();
                    }
                }

                try (PreparedStatement ps = conexion.prepareStatement(sqlExistencia)) {
                    for (DetalleCompra detalle : detalles) {
                        ps.setInt(1, detalle.getCantidad());
                        ps.setInt(2, detalle.getIdProducto());

                        if (ps.executeUpdate() == 0) {
                            conexion.rollback();
                            mensajeError = "El producto \"" + detalle.getNombreProducto()
                                    + "\" ya no existe. La compra no se registró.";
                            return -1;
                        }
                    }
                }

                conexion.commit();
                compra.setIdCompra(idCompra);
                return idCompra;

            } catch (SQLException e) {
                conexion.rollback();
                throw e;
            } finally {
                conexion.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("Error al registrar la compra:");
            e.printStackTrace();
            mensajeError = "No se pudo registrar la compra: " + e.getMessage();
            return -1;
        }
    }

    // Marca la compra como cancelada (Inactivo) y resta del inventario la
    // mercancía que entró. Si parte de ella ya se vendió no se cancela nada.
    public boolean cancelar(int idCompra) {
        mensajeError = null;

        String sqlCompra = "UPDATE Compras c SET c.estado = ? "
                         + "WHERE c.id_compras = ? AND " + COMPRA_ACTIVA;

        String sqlDetalle = "SELECT d.id_producto, p.nombre, d.cantidad "
                          + "FROM detalle_compras d "
                          + "INNER JOIN Productos p ON d.id_producto = p.id_producto "
                          + "WHERE d.id_compras = ? "
                          + "ORDER BY d.id_detalle_compra";

        String sqlExistencia = "UPDATE Productos "
                             + "SET existencia = existencia - ? "
                             + "WHERE id_producto = ? AND existencia >= ?";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                mensajeError = "No se pudo conectar con la base de datos.";
                return false;
            }

            try {
                conexion.setAutoCommit(false);

                try (PreparedStatement ps = conexion.prepareStatement(sqlCompra)) {
                    ps.setString(1, Estados.INACTIVO);
                    ps.setInt(2, idCompra);

                    if (ps.executeUpdate() == 0) {
                        conexion.rollback();
                        mensajeError = "La compra no existe o ya estaba cancelada.";
                        return false;
                    }
                }

                List<DetalleCompra> detalles = new ArrayList<>();

                try (PreparedStatement ps = conexion.prepareStatement(sqlDetalle)) {
                    ps.setInt(1, idCompra);

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            detalles.add(new DetalleCompra(
                                    rs.getInt("id_producto"),
                                    rs.getString("nombre"),
                                    rs.getInt("cantidad"),
                                    null));
                        }
                    }
                }

                try (PreparedStatement ps = conexion.prepareStatement(sqlExistencia)) {
                    for (DetalleCompra detalle : detalles) {
                        ps.setInt(1, detalle.getCantidad());
                        ps.setInt(2, detalle.getIdProducto());
                        ps.setInt(3, detalle.getCantidad());

                        if (ps.executeUpdate() == 0) {
                            conexion.rollback();
                            mensajeError = "No se puede cancelar: parte de la mercancía de "
                                    + detalle.getNombreProducto() + " ya se vendió.";
                            return false;
                        }
                    }
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
            System.err.println("Error al cancelar la compra:");
            e.printStackTrace();
            mensajeError = "No se pudo cancelar la compra: " + e.getMessage();
            return false;
        }
    }

    // Solo el proveedor se puede corregir.
    public boolean actualizarProveedor(int idCompra, int idProveedor) {
        String sql = "UPDATE Compras SET id_proveedor = ? "
                   + "WHERE id_compras = ?";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                return false;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setInt(1, idProveedor);
                ps.setInt(2, idCompra);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al actualizar la compra:");
            e.printStackTrace();
            return false;
        }
    }

    // Precio de compra del producto en su compra activa más reciente, o null.
    public BigDecimal ultimoPrecioCompra(int idProducto) {
        String sql = "SELECT d.precio_compra "
                   + "FROM detalle_compras d "
                   + "INNER JOIN Compras c ON d.id_compras = c.id_compras "
                   + "WHERE d.id_producto = ? AND " + COMPRA_ACTIVA + " "
                   + "ORDER BY c.fecha DESC, c.id_compras DESC, d.id_detalle_compra DESC "
                   + "LIMIT 1";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                return null;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setInt(1, idProducto);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getBigDecimal("precio_compra");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar el último precio de compra:");
            e.printStackTrace();
        }

        return null;
    }

    private int siguienteId(Connection conexion, String sql) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 1;
        }
    }

    private Compra leerCompra(ResultSet rs) throws SQLException {
        Compra compra = new Compra();
        compra.setIdCompra(rs.getInt("id_compras"));
        compra.setIdProveedor(rs.getInt("id_proveedor"));
        compra.setIdEmpleado(rs.getInt("id_empleado"));

        Date fecha = rs.getDate("fecha");
        compra.setFecha(fecha == null ? null : fecha.toLocalDate());

        compra.setTotal(rs.getBigDecimal("total"));
        compra.setEstado(rs.getString("estado"));
        compra.setNombreProveedor(rs.getString("nombre_proveedor"));
        compra.setNombreEmpleado(rs.getString("nombre_empleado"));
        return compra;
    }
}
