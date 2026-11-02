package com.laroca.dao;

import com.laroca.conexion.Conexion;
import com.laroca.modelo.MetodoPago;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MetodoPagoDAO {

    public List<MetodoPago> listar() {
        List<MetodoPago> metodos = new ArrayList<>();

        String sql = "SELECT id_metodo, nombre "
                   + "FROM Metodos_Pago ORDER BY id_metodo";

        try (Connection conexion = Conexion.conectar()) {
            if (conexion == null) {
                return metodos;
            }

            try (PreparedStatement ps = conexion.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    metodos.add(new MetodoPago(
                            rs.getInt("id_metodo"),
                            rs.getString("nombre")));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar métodos de pago:");
            e.printStackTrace();
        }

        return metodos;
    }
}
