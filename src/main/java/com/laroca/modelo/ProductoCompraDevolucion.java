package com.laroca.modelo;

import java.math.BigDecimal;

public class ProductoCompraDevolucion {

    private int idProducto;
    private String nombreProducto;
    private int cantidadComprada;
    private int cantidadDevuelta;
    private int cantidadDisponible;
    private BigDecimal precioCompra;

    public ProductoCompraDevolucion() {
    }

    public ProductoCompraDevolucion(
            int idProducto,
            String nombreProducto,
            int cantidadComprada,
            int cantidadDevuelta,
            int cantidadDisponible,
            BigDecimal precioCompra) {

        this.idProducto = idProducto;
        this.nombreProducto = nombreProducto;
        this.cantidadComprada = cantidadComprada;
        this.cantidadDevuelta = cantidadDevuelta;
        this.cantidadDisponible = cantidadDisponible;
        this.precioCompra = precioCompra;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(int idProducto) {
        this.idProducto = idProducto;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public int getCantidadComprada() {
        return cantidadComprada;
    }

    public void setCantidadComprada(int cantidadComprada) {
        this.cantidadComprada = cantidadComprada;
    }

    public int getCantidadDevuelta() {
        return cantidadDevuelta;
    }

    public void setCantidadDevuelta(int cantidadDevuelta) {
        this.cantidadDevuelta = cantidadDevuelta;
    }

    public int getCantidadDisponible() {
        return cantidadDisponible;
    }

    public void setCantidadDisponible(int cantidadDisponible) {
        this.cantidadDisponible = cantidadDisponible;
    }

    public BigDecimal getPrecioCompra() {
        return precioCompra;
    }

    public void setPrecioCompra(BigDecimal precioCompra) {
        this.precioCompra = precioCompra;
    }
}
