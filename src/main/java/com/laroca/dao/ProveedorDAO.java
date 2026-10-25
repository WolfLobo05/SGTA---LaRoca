package com.laroca.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.laroca.conexion.Conexion;
import com.laroca.modelo.Estados;
import com.laroca.modelo.Proveedor;

public class ProveedorDAO {

    // Un registro es inactivo si su estado no es Activo (NULL cuenta como Activo).
    private static final String ES_INACTIVO =
            "UPPER(COALESCE(TRIM(estado), '')) NOT IN ('', 'ACTIVO')";

    public List<Proveedor> listar() {
        List<Proveedor> lista = new ArrayList<>();
        String sql = "SELECT id_proveedor, rfc, nombre, telefono, correo, direccion, contacto, estado FROM Proveedores ORDER BY id_proveedor";

        try (Connection con = Conexion.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Proveedor p = new Proveedor(
                        rs.getInt("id_proveedor"),
                        rs.getString("rfc"),
                        rs.getString("nombre"),
                        rs.getString("telefono"),
                        rs.getString("correo"),
                        rs.getString("direccion"),
                        rs.getString("contacto"),
                        rs.getString("estado")
                );
                lista.add(p);
            }
        } catch (SQLException e) {
            System.err.println("Error al listar proveedores: " + e.getMessage());
        }
        return lista;
    }

    private int obtenerSiguienteId(Connection con) throws SQLException {
        String sql = "SELECT COALESCE(MAX(id_proveedor), 0) + 1 AS siguiente_id FROM Proveedores";
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("siguiente_id");
            }
        }
        return 1;
    }

    public boolean guardar(Proveedor p) {
        String sql = "INSERT INTO Proveedores (id_proveedor, rfc, nombre, telefono, correo, direccion, contacto, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = Conexion.conectar()) {
            if (con == null) return false;

            int nuevoId = obtenerSiguienteId(con);
            p.setIdProveedor(nuevoId);

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, p.getIdProveedor());
                ps.setString(2, p.getRfc());
                ps.setString(3, p.getNombre());
                ps.setString(4, p.getTelefono());
                ps.setString(5, p.getCorreo());
                ps.setString(6, p.getDireccion());
                ps.setString(7, p.getContacto());
                ps.setString(8, Estados.ACTIVO);

                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al guardar proveedor: " + e.getMessage());
            return false;
        }
    }

    // Busca el RFC en todos los proveedores, activos e inactivos.
    // Si la consulta falla devuelve true para no permitir el guardado.
    public boolean existeRfc(String rfc, int idExcluir) {
        return contarRfc(rfc, idExcluir, "") != 0;
    }

    // Indica si el RFC duplicado pertenece a un proveedor inactivo.
    public boolean existeRfcInactivo(String rfc, int idExcluir) {
        return contarRfc(rfc, idExcluir, " AND " + ES_INACTIVO) > 0;
    }

    // Devuelve -1 si falla la consulta.
    private int contarRfc(String rfc, int idExcluir, String filtro) {
        String sql = "SELECT COUNT(*) FROM Proveedores WHERE UPPER(TRIM(rfc)) = UPPER(TRIM(?)) AND id_proveedor <> ?" + filtro;

        try (Connection con = Conexion.conectar()) {
            if (con == null) return -1;

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, rfc);
                ps.setInt(2, idExcluir);

                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : -1;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al comprobar el RFC: " + e.getMessage());
            return -1;
        }
    }

    public boolean actualizar(Proveedor p) {
        String sql = "UPDATE Proveedores SET rfc = ?, nombre = ?, telefono = ?, correo = ?, direccion = ?, contacto = ? WHERE id_proveedor = ?";

        try (Connection con = Conexion.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, p.getRfc());
            ps.setString(2, p.getNombre());
            ps.setString(3, p.getTelefono());
            ps.setString(4, p.getCorreo());
            ps.setString(5, p.getDireccion());
            ps.setString(6, p.getContacto());
            ps.setInt(7, p.getIdProveedor());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar proveedor: " + e.getMessage());
            return false;
        }
    }

    // Ya no se borran proveedores: solo se cambia el estado.
    public boolean desactivar(int idProveedor) {
        return cambiarEstado(idProveedor, Estados.INACTIVO);
    }

    public boolean reactivar(int idProveedor) {
        return cambiarEstado(idProveedor, Estados.ACTIVO);
    }

    private boolean cambiarEstado(int idProveedor, String estado) {
        String sql = "UPDATE Proveedores SET estado = ? WHERE id_proveedor = ?";

        try (Connection con = Conexion.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, estado);
            ps.setInt(2, idProveedor);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al cambiar el estado del proveedor: " + e.getMessage());
            return false;
        }
    }
}