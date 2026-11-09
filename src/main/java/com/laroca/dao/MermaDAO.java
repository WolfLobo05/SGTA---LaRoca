package com.laroca.dao;

import com.laroca.conexion.Conexion;
import com.laroca.modelo.DetalleMerma;
import com.laroca.modelo.Estados;
import com.laroca.modelo.Merma;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MermaDAO {

    private static final String CONSULTA_MERMA =
            "SELECT m.id_merma, m.id_empleado, m.fecha, m.estado, "
          + "e.usuario AS nombre_empleado "
          + "FROM Mermas m "
          + "INNER JOIN Empleados e ON m.id_empleado = e.id_empleado ";

    private static final String MERMA_ACTIVA =
            "UPPER(COALESCE(TRIM(m.estado), '')) IN ('', 'ACTIVO')";

    private String mensajeError;

    private final CompraDAO compraDAO = new CompraDAO();

    public String getMensajeError() {
        return mensajeError;
    }

    // =========================================================
    // CONSULTAS
    // =========================================================

    public List<Merma> listar() {

        List<Merma> mermas = new ArrayList<>();

        String sql = CONSULTA_MERMA + "ORDER BY m.id_merma";

        try (Connection conexion = Conexion.conectar()) {

            if (conexion == null) {
                return mermas;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    mermas.add(leerMerma(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al consultar mermas:");
            e.printStackTrace();
        }

        return mermas;
    }

    public Merma obtenerMerma(int idMerma) {

        String sql = CONSULTA_MERMA + "WHERE m.id_merma = ?";

        try (Connection conexion = Conexion.conectar()) {

            if (conexion == null) {
                return null;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {

                ps.setInt(1, idMerma);

                try (ResultSet rs = ps.executeQuery()) {

                    if (rs.next()) {
                        return leerMerma(rs);
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al consultar la merma:");
            e.printStackTrace();
        }

        return null;
    }

    public List<DetalleMerma> listarDetalle(int idMerma) {

        List<DetalleMerma> detalles = new ArrayList<>();

        String sql = "SELECT d.id_producto, p.nombre, "
                   + "d.cantidad, d.precio, d.motivo "
                   + "FROM detalle_mermas d "
                   + "INNER JOIN Productos p ON d.id_producto = p.id_producto "
                   + "WHERE d.id_merma = ? "
                   + "ORDER BY d.id_detalle_merma";

        try (Connection conexion = Conexion.conectar()) {

            if (conexion == null) {
                return detalles;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {

                ps.setInt(1, idMerma);

                try (ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {

                        detalles.add(new DetalleMerma(
                                rs.getInt("id_producto"),
                                rs.getString("nombre"),
                                rs.getInt("cantidad"),
                                rs.getBigDecimal("precio"),
                                rs.getString("motivo")));
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al consultar el detalle de la merma:");
            e.printStackTrace();
        }

        return detalles;
    }

    public int obtenerSiguienteId() {

        String sql = "SELECT COALESCE(MAX(id_merma), 0) + 1 AS siguiente "
                   + "FROM Mermas";

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
            System.err.println("Error al obtener el siguiente ID de merma:");
            e.printStackTrace();
        }

        return -1;
    }

    // =========================================================
    // REGISTRAR MERMA
    // =========================================================

    public int registrar(Merma merma, List<DetalleMerma> detalles) {

        mensajeError = null;

        if (merma == null || detalles == null || detalles.isEmpty()) {
            mensajeError = "La merma no tiene productos.";
            return -1;
        }

        if (merma.getIdEmpleado() <= 0 || merma.getFecha() == null) {
            mensajeError = "Los datos del empleado o la fecha no son válidos.";
            return -1;
        }

        for (DetalleMerma detalle : detalles) {

            if (detalle == null
                    || detalle.getIdProducto() <= 0
                    || detalle.getCantidad() <= 0
                    || detalle.getPrecio() == null
                    || detalle.getPrecio().compareTo(BigDecimal.ZERO) <= 0
                    || detalle.getMotivo() == null
                    || detalle.getMotivo().isBlank()
                    || detalle.getMotivo().trim().length() > 100) {

                mensajeError = "Hay productos con datos incompletos o inválidos.";
                return -1;
            }
        }

        String sqlMerma = "INSERT INTO Mermas "
                        + "(id_merma, id_empleado, fecha, estado) "
                        + "VALUES (?, ?, ?, ?)";

        String sqlDetalle = "INSERT INTO detalle_mermas "
                          + "(id_detalle_merma, id_producto, id_merma, "
                          + "cantidad, precio, motivo) "
                          + "VALUES (?, ?, ?, ?, ?, ?)";

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

                int idMerma = siguienteId(
                        conexion,
                        "SELECT COALESCE(MAX(id_merma), 0) + 1 "
                      + "FROM Mermas FOR UPDATE");

                int idDetalle = siguienteId(
                        conexion,
                        "SELECT COALESCE(MAX(id_detalle_merma), 0) + 1 "
                      + "FROM detalle_mermas FOR UPDATE");

                try (PreparedStatement ps = conexion.prepareStatement(sqlMerma)) {

                    ps.setInt(1, idMerma);
                    ps.setInt(2, merma.getIdEmpleado());
                    ps.setDate(3, Date.valueOf(merma.getFecha()));
                    ps.setString(4, Estados.ACTIVO);

                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conexion.prepareStatement(sqlDetalle)) {

                    for (DetalleMerma detalle : detalles) {

                        ps.setInt(1, idDetalle++);
                        ps.setInt(2, detalle.getIdProducto());
                        ps.setInt(3, idMerma);
                        ps.setInt(4, detalle.getCantidad());
                        ps.setBigDecimal(5, detalle.getPrecio());
                        ps.setString(6, detalle.getMotivo().trim());

                        ps.executeUpdate();
                    }
                }

                try (PreparedStatement ps = conexion.prepareStatement(sqlExistencia)) {

                    for (DetalleMerma detalle : detalles) {

                        ps.setInt(1, detalle.getCantidad());
                        ps.setInt(2, detalle.getIdProducto());
                        ps.setInt(3, detalle.getCantidad());

                        if (ps.executeUpdate() == 0) {

                            conexion.rollback();

                            mensajeError = "No hay existencia suficiente de \""
                                    + detalle.getNombreProducto()
                                    + "\". La merma no se registró.";

                            return -1;
                        }
                    }
                }

                conexion.commit();

                merma.setIdMerma(idMerma);
                merma.setEstado(Estados.ACTIVO);

                return idMerma;

            } catch (SQLException e) {

                conexion.rollback();
                throw e;

            } finally {
                conexion.setAutoCommit(true);
            }

        } catch (SQLException e) {

            System.err.println("Error al registrar la merma:");
            e.printStackTrace();

            mensajeError = "No se pudo registrar la merma: " + e.getMessage();

            return -1;
        }
    }

    // =========================================================
    // CANCELAR MERMA
    // =========================================================

    public boolean cancelar(int idMerma) {

        mensajeError = null;

        String sqlMerma = "UPDATE Mermas m "
                        + "SET m.estado = ? "
                        + "WHERE m.id_merma = ? AND " + MERMA_ACTIVA;

        String sqlExistencia = "UPDATE Productos p "
                             + "INNER JOIN ("
                             + "SELECT id_producto, SUM(cantidad) AS cantidad "
                             + "FROM detalle_mermas "
                             + "WHERE id_merma = ? "
                             + "GROUP BY id_producto"
                             + ") d ON p.id_producto = d.id_producto "
                             + "SET p.existencia = p.existencia + d.cantidad";

        try (Connection conexion = Conexion.conectar()) {

            if (conexion == null) {
                mensajeError = "No se pudo conectar con la base de datos.";
                return false;
            }

            try {

                conexion.setAutoCommit(false);

                try (PreparedStatement ps = conexion.prepareStatement(sqlMerma)) {

                    ps.setString(1, Estados.INACTIVO);
                    ps.setInt(2, idMerma);

                    if (ps.executeUpdate() == 0) {

                        conexion.rollback();

                        mensajeError = "La merma no existe o ya estaba cancelada.";

                        return false;
                    }
                }

                try (PreparedStatement ps = conexion.prepareStatement(sqlExistencia)) {

                    ps.setInt(1, idMerma);
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

            System.err.println("Error al cancelar la merma:");
            e.printStackTrace();

            mensajeError = "No se pudo cancelar la merma: " + e.getMessage();

            return false;
        }
    }

    // =========================================================
    // PRECIO DE COMPRA
    // =========================================================

    public BigDecimal obtenerPrecioCompra(int idProducto) {
        return compraDAO.ultimoPrecioCompra(idProducto);
    }

    // =========================================================
    // MÉTODOS AUXILIARES
    // =========================================================

    private int siguienteId(Connection conexion, String sql)
            throws SQLException {

        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getInt(1) : 1;
        }
    }

    private Merma leerMerma(ResultSet rs) throws SQLException {

        Merma merma = new Merma();

        merma.setIdMerma(rs.getInt("id_merma"));
        merma.setIdEmpleado(rs.getInt("id_empleado"));

        Date fecha = rs.getDate("fecha");

        merma.setFecha(fecha == null ? null : fecha.toLocalDate());

        merma.setEstado(rs.getString("estado"));
        merma.setNombreEmpleado(rs.getString("nombre_empleado"));

        return merma;
    }
}

