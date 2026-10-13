package com.laroca.prueba;

import com.laroca.dao.UnidadMedidaDAO;
import com.laroca.modelo.UnidadMedida;

public class PruebaUnidadMedida {

    public static void main(String[] args) {

        UnidadMedidaDAO dao = new UnidadMedidaDAO();

        System.out.println("=== LISTADO DE UNIDADES ===");

        for (UnidadMedida unidad : dao.listar()) {
            System.out.println(
                unidad.getIdUnidadMedida() + " | " +
                unidad.getEstado() + " | " +
                unidad.getAbreviatura() + " | " +
                unidad.getNombre()
            );
        }
    }
}