package com.laroca.modelo;

import java.math.BigDecimal;

// Un renglón de la merma: producto, cantidad, costo y motivo individual.
public class DetalleMerma {

    private int idProducto;
    private String nombreProducto;
    private int cantidad;
    private BigDecimal precio;
    private String motivo;

    public DetalleMerma() {

    }

    public DetalleMerma(
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

