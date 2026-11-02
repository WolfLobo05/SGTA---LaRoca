package com.laroca.modelo;

import java.math.BigDecimal;

// Un renglón de la venta: producto, cantidad y precio al momento de vender.
public class DetalleVenta {

    private int idProducto;
    private String nombreProducto;
    private int cantidad;
    private BigDecimal precioUnitario;

    public DetalleVenta()
    {

    }

    public DetalleVenta(int idProd, String nombre, int cant, BigDecimal precio)
    {
        idProducto = idProd;
        nombreProducto = nombre;
        cantidad = cant;
        precioUnitario = precio;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(int id) {
        idProducto = id;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombre) {
        nombreProducto = nombre;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cant) {
        cantidad = cant;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precio) {
        precioUnitario = precio;
    }

    public BigDecimal getSubtotal() {
        if (precioUnitario == null) {
            return BigDecimal.ZERO;
        }
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }
}
