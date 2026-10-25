package com.laroca.dao;

import com.laroca.conexion.Conexion;
import com.laroca.modelo.Cliente;
import com.laroca.modelo.Estados;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ClienteDAO {

    // Un registro es inactivo si su estado no es Activo (NULL cuenta como Activo).
    private static final String ES_INACTIVO =
            "UPPER(COALESCE(TRIM(estado), '')) NOT IN ('', 'ACTIVO')";

    public List<Cliente> listar() {
        List<Cliente> lista = new ArrayList<>();

        String sql = "SELECT id_cliente, nombre, apellido_paterno, "
                + "apellido_materno, telefono, correo, estado "
                + "FROM Clientes ORDER BY id_cliente";

        try (Connection con = Conexion.conectar();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Cliente cliente = new Cliente(
                        rs.getInt("id_cliente"),
                        rs.getString("nombre"),
                        rs.getString("apellido_paterno"),
                        rs.getString("apellido_materno"),
                        rs.getString("telefono"),
                        rs.getString("correo"));

                cliente.setEstado(rs.getString("estado"));

                lista.add(cliente);
            }

        } catch (SQLException e) {
            System.err.println("Error al listar clientes: "
                    + e.getMessage());
        }

        return lista;
    }

    public int obtenerSiguienteId() {
        String sql = "SELECT COALESCE(MAX(id_cliente), 0) + 1 "
                + "FROM Clientes";

        try (Connection con = Conexion.conectar();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("Error al obtener ID del cliente: "
                    + e.getMessage());
        }

        return -1;
    }

    public boolean existeCliente(
            String nombre,
            String apellidoPaterno,
            String apellidoMaterno,
            String telefono,
            String correo,
            int idExcluir) {

        if (telefono == null || telefono.trim().isEmpty()) {
            return false;
        }

        String sql = "SELECT COUNT(*) FROM Clientes "
                + "WHERE id_cliente <> ? "
                + "AND telefono = ?";

        try (Connection con = Conexion.conectar();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idExcluir);
            ps.setString(2, telefono.trim());

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            System.err.println(
                    "Error al comprobar teléfono duplicado: "
                            + e.getMessage());

            return true;
        }
    }

    // Indica si el teléfono duplicado pertenece a un cliente inactivo.
    public boolean existeTelefonoInactivo(String telefono, int idExcluir) {
        String sql = "SELECT COUNT(*) FROM Clientes "
                + "WHERE id_cliente <> ? "
                + "AND telefono = ? "
                + "AND " + ES_INACTIVO;

        try (Connection con = Conexion.conectar();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idExcluir);
            ps.setString(2, telefono.trim());

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            System.err.println(
                    "Error al comprobar el estado del cliente: "
                            + e.getMessage());
            return false;
        }
    }

    public boolean insertar(Cliente cliente) {
        String sql = "INSERT INTO Clientes "
                + "(id_cliente, nombre, apellido_paterno, "
                + "apellido_materno, telefono, correo, estado) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = Conexion.conectar();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, cliente.getIdCliente());
            ps.setString(2, cliente.getNombre());
            ps.setString(3, cliente.getApellidoPaterno());
            ps.setString(4, cliente.getApellidoMaterno());
            ps.setString(5, cliente.getTelefono());
            ps.setString(6, cliente.getCorreo());
            ps.setString(7, cliente.getEstado());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al insertar cliente: "
                    + e.getMessage());
            return false;
        }
    }

    public boolean actualizar(Cliente cliente) {
        String sql = "UPDATE Clientes SET "
                + "nombre = ?, "
                + "apellido_paterno = ?, "
                + "apellido_materno = ?, "
                + "telefono = ?, "
                + "correo = ? "
                + "WHERE id_cliente = ?";

        try (Connection con = Conexion.conectar();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, cliente.getNombre());
            ps.setString(2, cliente.getApellidoPaterno());
            ps.setString(3, cliente.getApellidoMaterno());
            ps.setString(4, cliente.getTelefono());
            ps.setString(5, cliente.getCorreo());
            ps.setInt(6, cliente.getIdCliente());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al actualizar cliente: "
                    + e.getMessage());
            return false;
        }
    }

    // Ya no se borran clientes: solo se cambia el estado.
    public boolean desactivar(int idCliente) {
        return cambiarEstado(idCliente, Estados.INACTIVO);
    }

    public boolean reactivar(int idCliente) {
        return cambiarEstado(idCliente, Estados.ACTIVO);
    }

    private boolean cambiarEstado(int idCliente, String estado) {
        String sql = "UPDATE Clientes SET estado = ? WHERE id_cliente = ?";

        try (Connection con = Conexion.conectar();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, estado);
            ps.setInt(2, idCliente);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al cambiar el estado del cliente: "
                    + e.getMessage());
            return false;
        }
    }
}
