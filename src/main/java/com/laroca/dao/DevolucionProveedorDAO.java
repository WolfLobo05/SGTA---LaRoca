package com.laroca.dao;

import com.laroca.conexion.Conexion;
import com.laroca.modelo.Compra;
import com.laroca.modelo.DetalleDevolucionProveedor;
import com.laroca.modelo.DevolucionProveedor;
import com.laroca.modelo.Estados;
import com.laroca.modelo.ProductoCompraDevolucion;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DevolucionProveedorDAO {

    private static final String CONSULTA_DEVOLUCION = "SELECT dp.id_devolucion_proveedor, dp.id_compras, "
            + "dp.id_empleado, dp.fecha, dp.estado, "
            + "CONCAT('C-', LPAD(c.id_compras, 3, '0')) AS folio_compra, "
            + "p.nombre AS nombre_proveedor, e.usuario AS nombre_empleado "
            + "FROM Devolucion_Proveedores dp "
            + "INNER JOIN Compras c ON dp.id_compras = c.id_compras "
            + "INNER JOIN Proveedores p ON c.id_proveedor = p.id_proveedor "
            + "INNER JOIN Empleados e ON dp.id_empleado = e.id_empleado ";

    private String mensajeError;

    public String getMensajeError() {
        return mensajeError;
    }

    public List<DevolucionProveedor> listar() {
        List<DevolucionProveedor> devoluciones = new ArrayList<>();
        String sql = CONSULTA_DEVOLUCION
                + "ORDER BY dp.id_devolucion_proveedor";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null)
                return devoluciones;

            try (PreparedStatement ps = conexion.prepareStatement(sql);
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    devoluciones.add(leerDevolucion(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar devoluciones a proveedores:");
            e.printStackTrace();
        }
        return devoluciones;
    }

    public DevolucionProveedor obtenerDevolucion(int idDevolucion) {
        String sql = CONSULTA_DEVOLUCION
                + "WHERE dp.id_devolucion_proveedor = ?";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null)
                return null;

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setInt(1, idDevolucion);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next())
                        return leerDevolucion(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar la devolución:");
            e.printStackTrace();
        }
        return null;
    }

    public List<DetalleDevolucionProveedor> listarDetalle(int idDevolucion) {
        List<DetalleDevolucionProveedor> detalles = new ArrayList<>();
        String sql = "SELECT dd.id_producto, p.nombre, dd.cantidad, "
                + "dd.precio, dd.motivo "
                + "FROM detalle_devolucion_proveedores dd "
                + "INNER JOIN Productos p ON dd.id_producto = p.id_producto "
                + "WHERE dd.id_devolucion_proveedor = ? "
                + "ORDER BY dd.id_detalle_devolucion_proveedor";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null)
                return detalles;

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setInt(1, idDevolucion);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        detalles.add(new DetalleDevolucionProveedor(
                                rs.getInt("id_producto"),
                                rs.getString("nombre"),
                                rs.getInt("cantidad"),
                                rs.getBigDecimal("precio"),
                                rs.getString("motivo")));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar el detalle de la devolución:");
            e.printStackTrace();
        }
        return detalles;
    }

    public List<Compra> buscarComprasActivas(String texto) {
        List<Compra> compras = new ArrayList<>();
        if (texto == null || texto.trim().isEmpty())
            return compras;

        String sql = "SELECT c.id_compras, c.id_proveedor, c.id_empleado, "
                + "c.fecha, c.total, c.estado, "
                + "p.nombre AS nombre_proveedor, e.usuario AS nombre_empleado "
                + "FROM Compras c "
                + "INNER JOIN Proveedores p ON c.id_proveedor = p.id_proveedor "
                + "INNER JOIN Empleados e ON c.id_empleado = e.id_empleado "
                + "WHERE UPPER(COALESCE(TRIM(c.estado), '')) IN ('', 'ACTIVO') "
                + "AND (CONCAT('C-', LPAD(c.id_compras, 3, '0')) LIKE ? "
                + "OR p.nombre LIKE ?) "
                + "ORDER BY c.id_compras DESC LIMIT 10";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null)
                return compras;

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                String busqueda = "%" + texto.trim() + "%";
                ps.setString(1, busqueda);
                ps.setString(2, busqueda);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
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
                        compras.add(compra);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar compras activas:");
            e.printStackTrace();
        }
        return compras;
    }

    public List<ProductoCompraDevolucion> listarProductosCompra(int idCompra) {
        List<ProductoCompraDevolucion> productos = new ArrayList<>();

        String sql = "SELECT dc.id_producto, p.nombre, "
                + "SUM(dc.cantidad) AS cantidad_comprada, "
                + "COALESCE(r.cantidad_devuelta, 0) AS cantidad_devuelta, "
                + "SUM(dc.cantidad) - COALESCE(r.cantidad_devuelta, 0) "
                + "AS cantidad_disponible, "
                + "MAX(dc.precio_compra) AS precio_compra "
                + "FROM detalle_compras dc "
                + "INNER JOIN Productos p ON dc.id_producto = p.id_producto "
                + "LEFT JOIN ( "
                + "SELECT dd.id_producto, SUM(dd.cantidad) AS cantidad_devuelta "
                + "FROM detalle_devolucion_proveedores dd "
                + "INNER JOIN Devolucion_Proveedores dp "
                + "ON dd.id_devolucion_proveedor = dp.id_devolucion_proveedor "
                + "WHERE dp.id_compras = ? "
                + "AND UPPER(COALESCE(TRIM(dp.estado), '')) IN ('', 'ACTIVO') "
                + "GROUP BY dd.id_producto "
                + ") r ON dc.id_producto = r.id_producto "
                + "WHERE dc.id_compras = ? "
                + "GROUP BY dc.id_producto, p.nombre, r.cantidad_devuelta "
                + "HAVING SUM(dc.cantidad) - COALESCE(r.cantidad_devuelta, 0) > 0 "
                + "ORDER BY p.nombre";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null)
                return productos;

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setInt(1, idCompra);
                ps.setInt(2, idCompra);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        productos.add(new ProductoCompraDevolucion(
                                rs.getInt("id_producto"),
                                rs.getString("nombre"),
                                rs.getInt("cantidad_comprada"),
                                rs.getInt("cantidad_devuelta"),
                                rs.getInt("cantidad_disponible"),
                                rs.getBigDecimal("precio_compra")));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar productos de la compra:");
            e.printStackTrace();
        }
        return productos;
    }

    public int obtenerSiguienteId() {
        String sql = "SELECT COALESCE(MAX(id_devolucion_proveedor), 0) + 1 "
                + "AS siguiente FROM Devolucion_Proveedores";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null)
                return -1;

            try (PreparedStatement ps = conexion.prepareStatement(sql);
                    ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt("siguiente");
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener el siguiente ID:");
            e.printStackTrace();
        }
        return -1;
    }

    public int registrar(
            DevolucionProveedor devolucion,
            List<DetalleDevolucionProveedor> detalles) {

        mensajeError = null;

        if (devolucion == null) {
            mensajeError = "Los datos de la devolución son obligatorios.";
            return -1;
        }
        if (detalles == null || detalles.isEmpty()) {
            mensajeError = "La devolución no tiene productos.";
            return -1;
        }

        String sqlCompra = "SELECT estado FROM Compras "
                + "WHERE id_compras = ? FOR UPDATE";
        String sqlEncabezado = "INSERT INTO Devolucion_Proveedores "
                + "(id_devolucion_proveedor, id_compras, id_empleado, fecha, estado) "
                + "VALUES (?, ?, ?, ?, ?)";
        String sqlDetalle = "INSERT INTO detalle_devolucion_proveedores "
                + "(id_detalle_devolucion_proveedor, id_producto, "
                + "id_devolucion_proveedor, cantidad, precio, motivo) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        String sqlDisponibles = "SELECT "
                + "COALESCE((SELECT SUM(dc.cantidad) FROM detalle_compras dc "
                + "WHERE dc.id_compras = ? AND dc.id_producto = ?), 0) "
                + "- COALESCE((SELECT SUM(dd.cantidad) "
                + "FROM detalle_devolucion_proveedores dd "
                + "INNER JOIN Devolucion_Proveedores dp "
                + "ON dd.id_devolucion_proveedor = dp.id_devolucion_proveedor "
                + "WHERE dp.id_compras = ? AND dd.id_producto = ? "
                + "AND UPPER(COALESCE(TRIM(dp.estado), '')) IN ('', 'ACTIVO')), 0) "
                + "AS disponibles";
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

                try (PreparedStatement ps = conexion.prepareStatement(sqlCompra)) {
                    ps.setInt(1, devolucion.getIdCompra());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()
                                || !Estados.esActivo(rs.getString("estado"))) {
                            conexion.rollback();
                            mensajeError = "La compra no existe o está cancelada.";
                            return -1;
                        }
                    }
                }

                // Agrupar por producto para impedir que varias líneas evadan el límite.
                java.util.Map<Integer, Integer> cantidades = new java.util.LinkedHashMap<>();
                for (DetalleDevolucionProveedor detalle : detalles) {
                    if (detalle == null || detalle.getIdProducto() <= 0
                            || detalle.getCantidad() <= 0) {
                        conexion.rollback();
                        mensajeError = "Hay un producto o una cantidad inválida.";
                        return -1;
                    }
                    if (detalle.getPrecio() == null
                            || detalle.getPrecio().signum() < 0) {
                        conexion.rollback();
                        mensajeError = "Hay un precio de compra inválido.";
                        return -1;
                    }
                    if (detalle.getMotivo() == null
                            || detalle.getMotivo().trim().isEmpty()
                            || detalle.getMotivo().trim().length() > 100) {
                        conexion.rollback();
                        mensajeError = "Cada producto debe tener un motivo de hasta 100 caracteres.";
                        return -1;
                    }
                    cantidades.merge(
                            detalle.getIdProducto(),
                            detalle.getCantidad(),
                            Integer::sum);
                }

                try (PreparedStatement ps = conexion.prepareStatement(sqlDisponibles)) {
                    for (java.util.Map.Entry<Integer, Integer> entrada : cantidades.entrySet()) {
                        ps.setInt(1, devolucion.getIdCompra());
                        ps.setInt(2, entrada.getKey());
                        ps.setInt(3, devolucion.getIdCompra());
                        ps.setInt(4, entrada.getKey());

                        try (ResultSet rs = ps.executeQuery()) {
                            int disponibles = rs.next() ? rs.getInt("disponibles") : 0;
                            if (entrada.getValue() > disponibles) {
                                conexion.rollback();
                                mensajeError = "La cantidad solicitada supera las unidades disponibles "
                                        + "para devolver del producto ID " + entrada.getKey()
                                        + ". Disponibles: " + disponibles + ".";
                                return -1;
                            }
                        }
                    }
                }

                int idDevolucion = siguienteId(conexion,
                        "SELECT COALESCE(MAX(id_devolucion_proveedor), 0) + 1 "
                                + "FROM Devolucion_Proveedores FOR UPDATE");
                int idDetalle = siguienteId(conexion,
                        "SELECT COALESCE(MAX(id_detalle_devolucion_proveedor), 0) + 1 "
                                + "FROM detalle_devolucion_proveedores FOR UPDATE");

                try (PreparedStatement ps = conexion.prepareStatement(sqlEncabezado)) {
                    ps.setInt(1, idDevolucion);
                    ps.setInt(2, devolucion.getIdCompra());
                    ps.setInt(3, devolucion.getIdEmpleado());
                    ps.setDate(4, Date.valueOf(devolucion.getFecha()));
                    ps.setString(5, Estados.ACTIVO);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conexion.prepareStatement(sqlDetalle)) {
                    for (DetalleDevolucionProveedor detalle : detalles) {
                        ps.setInt(1, idDetalle++);
                        ps.setInt(2, detalle.getIdProducto());
                        ps.setInt(3, idDevolucion);
                        ps.setInt(4, detalle.getCantidad());
                        ps.setBigDecimal(5, detalle.getPrecio());
                        ps.setString(6, detalle.getMotivo().trim());
                        ps.executeUpdate();
                    }
                }

                try (PreparedStatement ps = conexion.prepareStatement(sqlExistencia)) {
                    for (java.util.Map.Entry<Integer, Integer> entrada : cantidades.entrySet()) {
                        ps.setInt(1, entrada.getValue());
                        ps.setInt(2, entrada.getKey());
                        ps.setInt(3, entrada.getValue());

                        if (ps.executeUpdate() == 0) {
                            conexion.rollback();
                            mensajeError = "No hay existencia suficiente para devolver el producto ID "
                                    + entrada.getKey() + ".";
                            return -1;
                        }
                    }
                }

                conexion.commit();
                devolucion.setIdDevolucionProveedor(idDevolucion);
                devolucion.setEstado(Estados.ACTIVO);
                return idDevolucion;

            } catch (SQLException e) {
                conexion.rollback();
                throw e;
            } finally {
                conexion.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("Error al registrar devolución a proveedor:");
            e.printStackTrace();
            mensajeError = "No se pudo registrar la devolución: " + e.getMessage();
            return -1;
        }
    }

    public boolean cancelar(int idDevolucion) {
        mensajeError = null;

        String sqlCabecera = "SELECT estado FROM Devolucion_Proveedores "
                + "WHERE id_devolucion_proveedor = ? FOR UPDATE";
        String sqlDetalles = "SELECT id_producto, SUM(cantidad) AS cantidad "
                + "FROM detalle_devolucion_proveedores "
                + "WHERE id_devolucion_proveedor = ? GROUP BY id_producto";
        String sqlEstado = "UPDATE Devolucion_Proveedores SET estado = ? "
                + "WHERE id_devolucion_proveedor = ?";
        String sqlExistencia = "UPDATE Productos SET existencia = existencia + ? "
                + "WHERE id_producto = ?";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                mensajeError = "No se pudo conectar con la base de datos.";
                return false;
            }

            try {
                conexion.setAutoCommit(false);

                try (PreparedStatement ps = conexion.prepareStatement(sqlCabecera)) {
                    ps.setInt(1, idDevolucion);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()
                                || !Estados.esActivo(rs.getString("estado"))) {
                            conexion.rollback();
                            mensajeError = "La devolución no existe o ya estaba cancelada.";
                            return false;
                        }
                    }
                }

                try (PreparedStatement ps = conexion.prepareStatement(sqlDetalles)) {
                    ps.setInt(1, idDevolucion);
                    try (ResultSet rs = ps.executeQuery();
                            PreparedStatement psStock = conexion.prepareStatement(sqlExistencia)) {
                        while (rs.next()) {
                            psStock.setInt(1, rs.getInt("cantidad"));
                            psStock.setInt(2, rs.getInt("id_producto"));
                            if (psStock.executeUpdate() == 0) {
                                conexion.rollback();
                                mensajeError = "No se pudo restaurar la existencia del producto ID "
                                        + rs.getInt("id_producto") + ".";
                                return false;
                            }
                        }
                    }
                }

                try (PreparedStatement ps = conexion.prepareStatement(sqlEstado)) {
                    ps.setString(1, Estados.INACTIVO);
                    ps.setInt(2, idDevolucion);

                    if (ps.executeUpdate() != 1) {
                        conexion.rollback();
                        mensajeError = "No se pudo actualizar el estado de la devolución.";
                        return false;
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
            System.err.println("Error al cancelar devolución a proveedor:");
            e.printStackTrace();
            mensajeError = "No se pudo cancelar la devolución: " + e.getMessage();
            return false;
        }
    }

    private int siguienteId(Connection conexion, String sql) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 1;
        }
    }

    private DevolucionProveedor leerDevolucion(ResultSet rs) throws SQLException {
        DevolucionProveedor devolucion = new DevolucionProveedor();
        devolucion.setIdDevolucionProveedor(
                rs.getInt("id_devolucion_proveedor"));
        devolucion.setIdCompra(rs.getInt("id_compras"));
        devolucion.setIdEmpleado(rs.getInt("id_empleado"));

        Date fecha = rs.getDate("fecha");
        devolucion.setFecha(fecha == null ? null : fecha.toLocalDate());

        devolucion.setEstado(rs.getString("estado"));
        devolucion.setFolioCompra(rs.getString("folio_compra"));
        devolucion.setNombreProveedor(rs.getString("nombre_proveedor"));
        devolucion.setNombreEmpleado(rs.getString("nombre_empleado"));
        return devolucion;
    }
}
