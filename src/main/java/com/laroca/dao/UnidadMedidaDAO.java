package com.laroca.dao;

import com.laroca.conexion.Conexion;
import com.laroca.modelo.UnidadMedida;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class UnidadMedidaDAO {

    public List<UnidadMedida> listar() {

        List<UnidadMedida> unidades = new ArrayList<>();

        String sql = "SELECT id_unidad, estado, abreviatura, nombre " +
                     "FROM Unidad_Medida ORDER BY id_unidad";

        try (
            Connection conexion = Conexion.conectar();
            PreparedStatement ps = conexion.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {

                UnidadMedida unidad = new UnidadMedida();

                unidad.setIdUnidadMedida(rs.getInt("id_unidad"));
                unidad.setEstado(rs.getString("estado"));
                unidad.setAbreviatura(rs.getString("abreviatura"));
                unidad.setNombre(rs.getString("nombre"));

                unidades.add(unidad);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return unidades;
    }


    public int obtenerSiguienteId() {

        String sql = "SELECT COALESCE(MAX(id_unidad), 0) + 1 AS siguiente " +
                     "FROM Unidad_Medida";

        try (
            Connection conexion = Conexion.conectar();
            PreparedStatement ps = conexion.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {

            if (rs.next()) {
                return rs.getInt("siguiente");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 1;
    }


    public boolean insertar(UnidadMedida unidad) {

        String sql = "INSERT INTO Unidad_Medida " +
                     "(id_unidad, estado, abreviatura, nombre) " +
                     "VALUES (?, ?, ?, ?)";

        try (
            Connection conexion = Conexion.conectar();
            PreparedStatement ps = conexion.prepareStatement(sql)
        ) {

            ps.setInt(1, unidad.getIdUnidadMedida());
            ps.setString(2, unidad.getEstado());
            ps.setString(3, unidad.getAbreviatura());
            ps.setString(4, unidad.getNombre());

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    public boolean actualizar(UnidadMedida unidad) {

        String sql = "UPDATE Unidad_Medida " +
                     "SET estado = ?, abreviatura = ?, nombre = ? " +
                     "WHERE id_unidad = ?";

        try (
            Connection conexion = Conexion.conectar();
            PreparedStatement ps = conexion.prepareStatement(sql)
        ) {

            ps.setString(1, unidad.getEstado());
            ps.setString(2, unidad.getAbreviatura());
            ps.setString(3, unidad.getNombre());
            ps.setInt(4, unidad.getIdUnidadMedida());

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    public boolean eliminar(int idUnidadMedida) {

        String sql = "DELETE FROM Unidad_Medida WHERE id_unidad = ?";

        try (
            Connection conexion = Conexion.conectar();
            PreparedStatement ps = conexion.prepareStatement(sql)
        ) {

            ps.setInt(1, idUnidadMedida);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}