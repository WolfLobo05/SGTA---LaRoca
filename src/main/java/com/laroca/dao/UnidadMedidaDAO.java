package com.laroca.dao;

import com.laroca.conexion.Conexion;
import com.laroca.modelo.Estados;
import com.laroca.modelo.UnidadMedida;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class UnidadMedidaDAO {

    // Un registro es inactivo si su estado no es Activo (NULL cuenta como Activo).
    private static final String ES_INACTIVO =
            "UPPER(COALESCE(TRIM(estado), '')) NOT IN ('', 'ACTIVO')";

    public List<UnidadMedida> listar() {
        List<UnidadMedida> unidades = new ArrayList<>();

        String sql = "SELECT id_unidad, estado, abreviatura, nombre "
                   + "FROM Unidad_Medida ORDER BY id_unidad";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                return unidades;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    UnidadMedida unidad = new UnidadMedida();

                    unidad.setIdUnidadMedida(rs.getInt("id_unidad"));
                    unidad.setEstado(rs.getString("estado"));
                    unidad.setAbreviatura(rs.getString("abreviatura"));
                    unidad.setNombre(rs.getString("nombre"));

                    unidades.add(unidad);
                }
            }
        } catch (Exception e) {
            System.err.println("Error al listar unidades de medida:");
            e.printStackTrace();
        }

        return unidades;
    }

    public int obtenerSiguienteId() {
        String sql = "SELECT COALESCE(MAX(id_unidad), 0) + 1 AS siguiente "
                   + "FROM Unidad_Medida";

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
        } catch (Exception e) {
            System.err.println("Error al obtener el ID de la unidad:");
            e.printStackTrace();
        }

        return -1;
    }

    // Comprueba si ya existe el nombre en otra unidad.
    // idExcluir = 0 al insertar; al editar, se excluye su propio ID.
    public boolean existeNombre(String nombre, int idExcluir) {
        String sql = "SELECT COUNT(*) AS cantidad "
                   + "FROM Unidad_Medida "
                   + "WHERE LOWER(TRIM(nombre)) = LOWER(TRIM(?)) "
                   + "AND id_unidad <> ?";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                // Si no hay conexión, no se permite continuar con el guardado.
                return true;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setString(1, nombre);
                ps.setInt(2, idExcluir);

                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt("cantidad") > 0;
                }
            }
        } catch (Exception e) {
            System.err.println("Error al comprobar el nombre de la unidad:");
            e.printStackTrace();

            // Ante un error, se evita permitir el guardado.
            return true;
        }
    }

    // Indica si el valor duplicado pertenece a un registro inactivo.
    public boolean existeNombreInactivo(String nombre, int idExcluir) {
        String sql = "SELECT COUNT(*) FROM Unidad_Medida "
                   + "WHERE LOWER(TRIM(nombre)) = LOWER(TRIM(?)) "
                   + "AND id_unidad <> ? "
                   + "AND " + ES_INACTIVO;

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                return false;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setString(1, nombre);
                ps.setInt(2, idExcluir);

                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            }
        } catch (Exception e) {
            System.err.println("Error al comprobar el estado de la unidad:");
            e.printStackTrace();
            return false;
        }
    }

    public boolean insertar(UnidadMedida unidad) {
        String sql = "INSERT INTO Unidad_Medida "
                   + "(id_unidad, estado, abreviatura, nombre) "
                   + "VALUES (?, ?, ?, ?)";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                return false;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setInt(1, unidad.getIdUnidadMedida());
                ps.setString(2, unidad.getEstado());
                ps.setString(3, unidad.getAbreviatura());
                ps.setString(4, unidad.getNombre());

                return ps.executeUpdate() > 0;
            }
        } catch (Exception e) {
            System.err.println("Error al insertar unidad de medida:");
            e.printStackTrace();
            return false;
        }
    }

    public boolean actualizar(UnidadMedida unidad) {
        String sql = "UPDATE Unidad_Medida "
                   + "SET abreviatura = ?, nombre = ? "
                   + "WHERE id_unidad = ?";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                return false;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setString(1, unidad.getAbreviatura());
                ps.setString(2, unidad.getNombre());
                ps.setInt(3, unidad.getIdUnidadMedida());

                return ps.executeUpdate() > 0;
            }
        } catch (Exception e) {
            System.err.println("Error al actualizar unidad de medida:");
            e.printStackTrace();
            return false;
        }
    }

    // Ya no se borran registros: solo se cambia el estado.
    public boolean desactivar(int idUnidadMedida) {
        return cambiarEstado(idUnidadMedida, Estados.INACTIVO);
    }

    public boolean reactivar(int idUnidadMedida) {
        return cambiarEstado(idUnidadMedida, Estados.ACTIVO);
    }

    private boolean cambiarEstado(int idUnidadMedida, String estado) {
        String sql = "UPDATE Unidad_Medida SET estado = ? WHERE id_unidad = ?";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                return false;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setString(1, estado);
                ps.setInt(2, idUnidadMedida);
                return ps.executeUpdate() > 0;
            }
        } catch (Exception e) {
            System.err.println("Error al cambiar el estado de la unidad:");
            e.printStackTrace();
            return false;
        }
    }
}
