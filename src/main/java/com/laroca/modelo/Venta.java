package com.laroca.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Venta {

    private int idVenta;
    private int idEmpleado;
    private int idCliente;
    private int idMetodo;
    private LocalDateTime fecha;
    private BigDecimal total;
    private BigDecimal montoPagado;
    private BigDecimal cambio;
    private String estado;

    // Datos de las tablas relacionadas, solo para mostrar.
    private String nombreEmpleado;
    private String nombreCliente;
    private String nombreMetodo;

    public Venta()
    {

    }

    public int getIdVenta() {
        return idVenta;
    }

    public void setIdVenta(int id) {
        idVenta = id;
    }

    // Folio visible de la venta, por ejemplo V-004.
    public String getFolio() {
        return String.format("V-%03d", idVenta);
    }

    public int getIdEmpleado() {
        return idEmpleado;
    }

    public void setIdEmpleado(int id) {
        idEmpleado = id;
    }

    public int getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(int id) {
        idCliente = id;
    }

    public int getIdMetodo() {
        return idMetodo;
    }

    public void setIdMetodo(int id) {
        idMetodo = id;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fec) {
        fecha = fec;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal tot) {
        total = tot;
    }

    public BigDecimal getMontoPagado() {
        return montoPagado;
    }

    public void setMontoPagado(BigDecimal monto) {
        montoPagado = monto;
    }

    public BigDecimal getCambio() {
        return cambio;
    }

    public void setCambio(BigDecimal cam) {
        cambio = cam;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String est) {
        estado = est;
    }

    public String getNombreEmpleado() {
        return nombreEmpleado;
    }

    public void setNombreEmpleado(String nombre) {
        nombreEmpleado = nombre;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombre) {
        nombreCliente = nombre;
    }

    public String getNombreMetodo() {
        return nombreMetodo;
    }

    public void setNombreMetodo(String nombre) {
        nombreMetodo = nombre;
    }
}
