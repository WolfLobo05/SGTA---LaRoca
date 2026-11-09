package com.laroca.modelo;

import java.time.LocalDate;

public class DevolucionCliente {

    private int idDevolucionCliente;
    private int idVenta;
    private int idEmpleado;
    private LocalDate fecha;
    private String estado;

    private String folioVenta;
    private String nombreCliente;
    private String nombreEmpleado;

    public DevolucionCliente() {
    }

    public int getIdDevolucionCliente() {
        return idDevolucionCliente;
    }

    public void setIdDevolucionCliente(int id) {
        idDevolucionCliente = id;
    }

    public String getFolio() {
        return String.format("DC-%03d", idDevolucionCliente);
    }

    public int getIdVenta() {
        return idVenta;
    }

    public void setIdVenta(int id) {
        idVenta = id;
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

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getFolioVenta() {
        return folioVenta;
    }

    public void setFolioVenta(String folio) {
        folioVenta = folio;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombre) {
        nombreCliente = nombre;
    }

    public String getNombreEmpleado() {
        return nombreEmpleado;
    }

    public void setNombreEmpleado(String nombre) {
        nombreEmpleado = nombre;
    }
}
