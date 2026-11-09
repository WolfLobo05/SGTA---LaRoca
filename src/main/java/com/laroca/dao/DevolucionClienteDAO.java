package com.laroca.dao;

import com.laroca.conexion.Conexion;
import com.laroca.modelo.DevolucionCliente;
import com.laroca.modelo.DetalleDevolucionCliente;
import com.laroca.modelo.Estados;
import com.laroca.modelo.ProductoVentaDevolucion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DevolucionClienteDAO {

    private String mensajeError;

    private static final String CONSULTA_DEVOLUCION = "SELECT d.id_devolucion_cliente, d.id_venta, "
            + "d.id_empleado, d.fecha, d.estado, "
            + "CONCAT('V-', LPAD(v.id_venta, 3, '0')) AS folio_venta, "
            + "CONCAT_WS(' ', NULLIF(TRIM(c.nombre), ''), "
            + "NULLIF(TRIM(c.apellido_paterno), ''), "
            + "NULLIF(TRIM(c.apellido_materno), '')) AS nombre_cliente, "
            + "e.usuario AS nombre_empleado "
            + "FROM Devolucion_Clientes d "
            + "INNER JOIN Ventas v ON d.id_venta = v.id_venta "
            + "INNER JOIN Clientes c ON v.id_cliente = c.id_cliente "
            + "INNER JOIN Empleados e ON d.id_empleado = e.id_empleado ";

    public String getMensajeError() {
        return mensajeError;
    }

    // Consulta todas las devoluciones.
    public List<DevolucionCliente> listar() {

        List<DevolucionCliente> devoluciones = new ArrayList<>();

        String sql = CONSULTA_DEVOLUCION
                + "ORDER BY d.id_devolucion_cliente";

        try (Connection conexion = Conexion.conectar()) {

            if (conexion == null) {
                return devoluciones;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql);
                    ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    devoluciones.add(leerDevolucion(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al consultar devoluciones:");
            e.printStackTrace();
        }

        return devoluciones;
    }

    // Consulta una devolución específica.
    public DevolucionCliente obtenerDevolucion(int idDevolucion) {

        String sql = CONSULTA_DEVOLUCION
                + "WHERE d.id_devolucion_cliente = ?";

        try (Connection conexion = Conexion.conectar()) {

            if (conexion == null) {
                return null;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {

                ps.setInt(1, idDevolucion);

                try (ResultSet rs = ps.executeQuery()) {

                    if (rs.next()) {
                        return leerDevolucion(rs);
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al consultar la devolución:");
            e.printStackTrace();
        }

        return null;
    }

    // Consulta los productos incluidos en una devolución.
    public List<DetalleDevolucionCliente> listarDetalle(int idDevolucion) {

        List<DetalleDevolucionCliente> detalles = new ArrayList<>();

        String sql = "SELECT d.id_producto, p.nombre, d.cantidad, "
                + "d.precio, d.motivo "
                + "FROM detalle_devolucion_clientes d "
                + "INNER JOIN Productos p ON d.id_producto = p.id_producto "
                + "WHERE d.id_devolucion_cliente = ? "
                + "ORDER BY d.id_detalle_devolucion_cliente";

        try (Connection conexion = Conexion.conectar()) {

            if (conexion == null) {
                return detalles;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {

                ps.setInt(1, idDevolucion);

                try (ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {

                        detalles.add(new DetalleDevolucionCliente(
                                rs.getInt("id_producto"),
                                rs.getString("nombre"),
                                rs.getInt("cantidad"),
                                rs.getBigDecimal("precio"),
                                rs.getString("motivo")));
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al consultar el detalle:");
            e.printStackTrace();
        }

        return detalles;
    }

    // Consulta los productos vendidos y las cantidades que todavía
    // pueden devolverse. Solo cuenta devoluciones activas.
    public List<ProductoVentaDevolucion> listarProductosVenta(int idVenta) {

        List<ProductoVentaDevolucion> productos = new ArrayList<>();

        String sql = "SELECT dv.id_producto, p.nombre, "
                + "SUM(dv.cantidad) AS cantidad_vendida, "
                + "COALESCE(r.cantidad_devuelta, 0) AS cantidad_devuelta, "
                + "SUM(dv.cantidad) - COALESCE(r.cantidad_devuelta, 0) "
                + "AS cantidad_disponible, "
                + "MAX(dv.precio_unitario) AS precio_unitario "
                + "FROM detalle_ventas dv "
                + "INNER JOIN Productos p ON dv.id_producto = p.id_producto "
                + "LEFT JOIN ( "
                + "SELECT dd.id_producto, SUM(dd.cantidad) AS cantidad_devuelta "
                + "FROM detalle_devolucion_clientes dd "
                + "INNER JOIN Devolucion_Clientes dc "
                + "ON dd.id_devolucion_cliente = dc.id_devolucion_cliente "
                + "WHERE dc.id_venta = ? "
                + "AND UPPER(TRIM(dc.estado)) = 'ACTIVO' "
                + "GROUP BY dd.id_producto "
                + ") r ON dv.id_producto = r.id_producto "
                + "WHERE dv.id_venta = ? "
                + "GROUP BY dv.id_producto, p.nombre, r.cantidad_devuelta "
                + "ORDER BY p.nombre";

        try (Connection conexion = Conexion.conectar()) {

            if (conexion == null) {
                return productos;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {

                ps.setInt(1, idVenta);
                ps.setInt(2, idVenta);

                try (ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {

                        productos.add(new ProductoVentaDevolucion(
                                rs.getInt("id_producto"),
                                rs.getString("nombre"),
                                rs.getInt("cantidad_vendida"),
                                rs.getInt("cantidad_devuelta"),
                                rs.getInt("cantidad_disponible"),
                                rs.getBigDecimal("precio_unitario")));
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al consultar productos de la venta:");
            e.printStackTrace();
        }

        return productos;
    }

    // Obtiene el siguiente ID.
    public int obtenerSiguienteId() {

        String sql = "SELECT COALESCE(MAX(id_devolucion_cliente), 0) + 1 "
                + "FROM Devolucion_Clientes";

        try (Connection conexion = Conexion.conectar()) {

            if (conexion == null) {
                return -1;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql);
                    ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al obtener el siguiente ID:");
            e.printStackTrace();
        }

        return -1;
    }

    // Registra la devolución y reintegra las cantidades al inventario.
    public int registrar(
            DevolucionCliente devolucion,
            List<DetalleDevolucionCliente> detalles) {

        mensajeError = null;

        if (devolucion == null
                || devolucion.getIdVenta() <= 0
                || devolucion.getIdEmpleado() <= 0
                || devolucion.getFecha() == null) {

            mensajeError = "Los datos de la devolución están incompletos.";
            return -1;
        }

        if (detalles == null || detalles.isEmpty()) {
            mensajeError = "Agrega al menos un producto a la devolución.";
            return -1;
        }

        Map<Integer, Integer> cantidades = new HashMap<>();

        for (DetalleDevolucionCliente detalle : detalles) {

            if (detalle == null
                    || detalle.getIdProducto() <= 0
                    || detalle.getCantidad() <= 0
                    || detalle.getPrecio() == null
                    || detalle.getPrecio().signum() <= 0
                    || detalle.getMotivo() == null
                    || detalle.getMotivo().isBlank()
                    || detalle.getMotivo().length() > 100) {

                mensajeError = "Hay productos con datos inválidos. "
                        + "Revisa cantidades, precios y motivos.";
                return -1;
            }

            cantidades.merge(
                    detalle.getIdProducto(),
                    detalle.getCantidad(),
                    Integer::sum);
        }

        String sqlVenta = "INSERT INTO Devolucion_Clientes "
                + "(id_devolucion_cliente, id_venta, id_empleado, fecha, estado) "
                + "VALUES (?, ?, ?, ?, ?)";

        String sqlDetalle = "INSERT INTO detalle_devolucion_clientes "
                + "(id_detalle_devolucion_cliente, id_producto, "
                + "id_devolucion_cliente, cantidad, precio, motivo) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        String sqlExistencia = "UPDATE Productos SET existencia = existencia + ? "
                + "WHERE id_producto = ?";

        try (Connection conexion = Conexion.conectar()) {

            if (conexion == null) {
                mensajeError = "No se pudo conectar con la base de datos.";
                return -1;
            }

            try {

                conexion.setAutoCommit(false);

                // Bloquea la venta para evitar devoluciones simultáneas
                // que superen las cantidades realmente vendidas.
                String sqlBloqueo = "SELECT estado FROM Ventas "
                        + "WHERE id_venta = ? FOR UPDATE";

                try (PreparedStatement ps = conexion.prepareStatement(sqlBloqueo)) {

                    ps.setInt(1, devolucion.getIdVenta());

                    try (ResultSet rs = ps.executeQuery()) {

                        if (!rs.next()
                                || !Estados.esActivo(rs.getString("estado"))) {

                            conexion.rollback();
                            mensajeError = "La venta no existe o está cancelada.";
                            return -1;
                        }
                    }
                }

                // Comprueba las cantidades originales y las devoluciones
                // anteriores que todavía están activas.
                String sqlValidacion = "SELECT COALESCE(SUM(dv.cantidad), 0) "
                        + "- COALESCE(( "
                        + "SELECT SUM(dd.cantidad) "
                        + "FROM detalle_devolucion_clientes dd "
                        + "INNER JOIN Devolucion_Clientes dc "
                        + "ON dd.id_devolucion_cliente = dc.id_devolucion_cliente "
                        + "WHERE dc.id_venta = ? "
                        + "AND dd.id_producto = ? "
                        + "AND UPPER(TRIM(dc.estado)) = 'ACTIVO' "
                        + "), 0) AS disponible "
                        + "FROM detalle_ventas dv "
                        + "WHERE dv.id_venta = ? "
                        + "AND dv.id_producto = ?";

                try (PreparedStatement ps = conexion.prepareStatement(sqlValidacion)) {

                    for (Map.Entry<Integer, Integer> entrada : cantidades.entrySet()) {

                        int idProducto = entrada.getKey();
                        int cantidadSolicitada = entrada.getValue();

                        ps.setInt(1, devolucion.getIdVenta());
                        ps.setInt(2, idProducto);
                        ps.setInt(3, devolucion.getIdVenta());
                        ps.setInt(4, idProducto);

                        try (ResultSet rs = ps.executeQuery()) {

                            int disponible = rs.next()
                                    ? rs.getInt("disponible")
                                    : 0;

                            if (cantidadSolicitada > disponible) {

                                conexion.rollback();

                                mensajeError = "La cantidad solicitada supera "
                                        + "las unidades disponibles para devolver "
                                        + "del producto ID " + idProducto + ". "
                                        + "Disponibles: " + disponible + ".";

                                return -1;
                            }
                        }
                    }
                }

                int idDevolucion = siguienteId(
                        conexion,
                        "SELECT COALESCE(MAX(id_devolucion_cliente), 0) + 1 "
                                + "FROM Devolucion_Clientes FOR UPDATE");

                int idDetalle = siguienteId(
                        conexion,
                        "SELECT COALESCE(MAX(id_detalle_devolucion_cliente), 0) + 1 "
                                + "FROM detalle_devolucion_clientes FOR UPDATE");

                try (PreparedStatement ps = conexion.prepareStatement(sqlVenta)) {

                    ps.setInt(1, idDevolucion);
                    ps.setInt(2, devolucion.getIdVenta());
                    ps.setInt(3, devolucion.getIdEmpleado());
                    ps.setDate(4, Date.valueOf(devolucion.getFecha()));
                    ps.setString(5, Estados.ACTIVO);

                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conexion.prepareStatement(sqlDetalle)) {

                    for (DetalleDevolucionCliente detalle : detalles) {

                        ps.setInt(1, idDetalle++);
                        ps.setInt(2, detalle.getIdProducto());
                        ps.setInt(3, idDevolucion);
                        ps.setInt(4, detalle.getCantidad());
                        ps.setBigDecimal(5, detalle.getPrecio());
                        ps.setString(6, detalle.getMotivo());

                        ps.executeUpdate();
                    }
                }

                // La devolución reintegra las unidades al inventario.
                try (PreparedStatement ps = conexion.prepareStatement(sqlExistencia)) {

                    for (Map.Entry<Integer, Integer> entrada : cantidades.entrySet()) {

                        ps.setInt(1, entrada.getValue());
                        ps.setInt(2, entrada.getKey());

                        if (ps.executeUpdate() == 0) {
                            throw new SQLException(
                                    "No se pudo actualizar el inventario "
                                            + "del producto ID " + entrada.getKey());
                        }
                    }
                }

                conexion.commit();

                devolucion.setIdDevolucionCliente(idDevolucion);
                devolucion.setEstado(Estados.ACTIVO);

                return idDevolucion;

            } catch (SQLException e) {

                conexion.rollback();
                throw e;

            } finally {
                conexion.setAutoCommit(true);
            }

        } catch (SQLException e) {

            System.err.println("Error al registrar la devolución:");
            e.printStackTrace();

            mensajeError = "No se pudo registrar la devolución: " + e.getMessage();

            return -1;
        }
    }

    // Cancela una devolución y descuenta del inventario las unidades
    // que anteriormente se habían reintegrado.
    public boolean cancelar(int idDevolucion) {

        mensajeError = null;

        String sqlBloqueo = "SELECT id_venta, estado FROM Devolucion_Clientes "
                + "WHERE id_devolucion_cliente = ? FOR UPDATE";

        String sqlDetalles = "SELECT id_producto, SUM(cantidad) AS cantidad "
                + "FROM detalle_devolucion_clientes "
                + "WHERE id_devolucion_cliente = ? "
                + "GROUP BY id_producto";

        String sqlExistencia = "UPDATE Productos SET existencia = existencia - ? "
                + "WHERE id_producto = ? AND existencia >= ?";

        String sqlEstado = "UPDATE Devolucion_Clientes SET estado = ? "
                + "WHERE id_devolucion_cliente = ? "
                + "AND UPPER(TRIM(estado)) = 'ACTIVO'";

        try (Connection conexion = Conexion.conectar()) {

            if (conexion == null) {
                mensajeError = "No se pudo conectar con la base de datos.";
                return false;
            }

            try {

                conexion.setAutoCommit(false);

                try (PreparedStatement ps = conexion.prepareStatement(sqlBloqueo)) {

                    ps.setInt(1, idDevolucion);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (!rs.next()
                                || !Estados.esActivo(rs.getString("estado"))) {

                            conexion.rollback();
                            mensajeError = "La devolución no existe o ya está cancelada.";
                            return false;
                        }
                    }
                }

                try (PreparedStatement ps = conexion.prepareStatement(sqlDetalles)) {

                    ps.setInt(1, idDevolucion);

                    try (ResultSet rs = ps.executeQuery()) {

                        while (rs.next()) {

                            int idProducto = rs.getInt("id_producto");
                            int cantidad = rs.getInt("cantidad");

                            try (PreparedStatement actualizar = conexion.prepareStatement(sqlExistencia)) {

                                actualizar.setInt(1, cantidad);
                                actualizar.setInt(2, idProducto);
                                actualizar.setInt(3, cantidad);

                                if (actualizar.executeUpdate() == 0) {

                                    conexion.rollback();

                                    mensajeError = "No hay existencia suficiente para "
                                            + "cancelar la devolución del producto "
                                            + "ID " + idProducto + ". "
                                            + "No se realizaron cambios.";

                                    return false;
                                }
                            }
                        }
                    }
                }

                try (PreparedStatement ps = conexion.prepareStatement(sqlEstado)) {

                    ps.setString(1, Estados.INACTIVO);
                    ps.setInt(2, idDevolucion);

                    if (ps.executeUpdate() == 0) {
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

            System.err.println("Error al cancelar la devolución:");
            e.printStackTrace();

            mensajeError = "No se pudo cancelar la devolución: " + e.getMessage();

            return false;
        }
    }

    private int siguienteId(Connection conexion, String sql)
            throws SQLException {

        try (PreparedStatement ps = conexion.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getInt(1) : 1;
        }
    }

    private DevolucionCliente leerDevolucion(ResultSet rs)
            throws SQLException {

        DevolucionCliente devolucion = new DevolucionCliente();

        devolucion.setIdDevolucionCliente(
                rs.getInt("id_devolucion_cliente"));

        devolucion.setIdVenta(rs.getInt("id_venta"));
        devolucion.setIdEmpleado(rs.getInt("id_empleado"));

        Date fecha = rs.getDate("fecha");

        devolucion.setFecha(
                fecha == null ? null : fecha.toLocalDate());

        devolucion.setEstado(rs.getString("estado"));
        devolucion.setFolioVenta(rs.getString("folio_venta"));
        devolucion.setNombreCliente(rs.getString("nombre_cliente"));
        devolucion.setNombreEmpleado(rs.getString("nombre_empleado"));

        return devolucion;
    }
}
