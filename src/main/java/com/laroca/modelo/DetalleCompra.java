package com.laroca.modelo;

import java.math.BigDecimal;

// Un renglón de la compra: producto, cantidad y precio pagado al proveedor.
public class DetalleCompra {

    private int idProducto;
    private String nombreProducto;
    private int cantidad;
    private BigDecimal precioCompra;

    public DetalleCompra()
    {

    }

    public DetalleCompra(int idProd, String nombre, int cant, BigDecimal precio)
    {
        idProducto = idProd;
        nombreProducto = nombre;
        cantidad = cant;
        precioCompra = precio;
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

    public BigDecimal getPrecioCompra() {
        return precioCompra;
    }

    public void setPrecioCompra(BigDecimal precio) {
        precioCompra = precio;
    }

    public BigDecimal getSubtotal() {
        if (precioCompra == null) {
            return BigDecimal.ZERO;
        }
        return precioCompra.multiply(BigDecimal.valueOf(cantidad));
    }
}
