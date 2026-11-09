package com.laroca.modelo;

import java.math.BigDecimal;

public class DetalleDevolucionProveedor {

    private int idProducto;
    private String nombreProducto;
    private int cantidad;
    private BigDecimal precio;
    private String motivo;

    public DetalleDevolucionProveedor() {
    }

    public DetalleDevolucionProveedor(
            int idProducto,
            String nombreProducto,
            int cantidad,
            BigDecimal precio,
            String motivo) {

        this.idProducto = idProducto;
        this.nombreProducto = nombreProducto;
        this.cantidad = cantidad;
        this.precio = precio;
        this.motivo = motivo;
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

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public BigDecimal getSubtotal() {
        if (precio == null) {
            return BigDecimal.ZERO;
        }
        return precio.multiply(BigDecimal.valueOf(cantidad));
    }
}
