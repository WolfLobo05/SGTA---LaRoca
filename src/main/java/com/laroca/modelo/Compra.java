package com.laroca.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Compra {

    private int idCompra;
    private int idProveedor;
    private int idEmpleado;
    private LocalDate fecha;
    private BigDecimal total;
    private String estado;

    // Datos de las tablas relacionadas, solo para mostrar.
    private String nombreProveedor;
    private String nombreEmpleado;

    public Compra()
    {

    }

    public int getIdCompra() {
        return idCompra;
    }

    public void setIdCompra(int id) {
        idCompra = id;
    }

    // Folio visible de la compra, por ejemplo C-004.
    public String getFolio() {
        return String.format("C-%03d", idCompra);
    }

    public int getIdProveedor() {
        return idProveedor;
    }

    public void setIdProveedor(int id) {
        idProveedor = id;
    }

    public int getIdEmpleado() {
        return idEmpleado;
    }

    public void setIdEmpleado(int id) {
        idEmpleado = id;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fec) {
        fecha = fec;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal tot) {
        total = tot;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String est) {
        estado = est;
    }

    public String getNombreProveedor() {
        return nombreProveedor;
    }

    public void setNombreProveedor(String nombre) {
        nombreProveedor = nombre;
    }

    public String getNombreEmpleado() {
        return nombreEmpleado;
    }

    public void setNombreEmpleado(String nombre) {
        nombreEmpleado = nombre;
    }
}
