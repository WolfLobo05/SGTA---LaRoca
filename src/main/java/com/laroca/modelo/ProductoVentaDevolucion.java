package com.laroca.modelo;

import java.math.BigDecimal;

public class ProductoVentaDevolucion {

    private int idProducto;
    private String nombreProducto;
    private int cantidadVendida;
    private int cantidadDevuelta;
    private int cantidadDisponible;
    private BigDecimal precioUnitario;

    public ProductoVentaDevolucion(
            int idProducto,
            String nombreProducto,
            int cantidadVendida,
            int cantidadDevuelta,
            int cantidadDisponible,
            BigDecimal precioUnitario) {

        this.idProducto = idProducto;
        this.nombreProducto = nombreProducto;
        this.cantidadVendida = cantidadVendida;
        this.cantidadDevuelta = cantidadDevuelta;
        this.cantidadDisponible = cantidadDisponible;
        this.precioUnitario = precioUnitario;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public int getCantidadVendida() {
        return cantidadVendida;
    }

    public int getCantidadDevuelta() {
        return cantidadDevuelta;
    }

    public int getCantidadDisponible() {
        return cantidadDisponible;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }
}
