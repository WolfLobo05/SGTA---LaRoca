package com.laroca.modelo;

import java.time.LocalDate;

public class DevolucionProveedor {

    private int idDevolucionProveedor;
    private int idCompra;
    private int idEmpleado;
    private LocalDate fecha;
    private String estado;

    // Datos relacionados, únicamente para mostrar.
    private String folioCompra;
    private String nombreProveedor;
    private String nombreEmpleado;

    public DevolucionProveedor() {
    }

    public int getIdDevolucionProveedor() {
        return idDevolucionProveedor;
    }

    public void setIdDevolucionProveedor(int idDevolucionProveedor) {
        this.idDevolucionProveedor = idDevolucionProveedor;
    }

    // Folio visible, por ejemplo DP-001.
    public String getFolio() {
        return String.format("DP-%03d", idDevolucionProveedor);
    }

    public int getIdCompra() {
        return idCompra;
    }

    public void setIdCompra(int idCompra) {
        this.idCompra = idCompra;
    }

    public int getIdEmpleado() {
        return idEmpleado;
    }

    public void setIdEmpleado(int idEmpleado) {
        this.idEmpleado = idEmpleado;
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

    public String getFolioCompra() {
        return folioCompra;
    }

    public void setFolioCompra(String folioCompra) {
        this.folioCompra = folioCompra;
    }

    public String getNombreProveedor() {
        return nombreProveedor;
    }

    public void setNombreProveedor(String nombreProveedor) {
        this.nombreProveedor = nombreProveedor;
    }

    public String getNombreEmpleado() {
        return nombreEmpleado;
    }

    public void setNombreEmpleado(String nombreEmpleado) {
        this.nombreEmpleado = nombreEmpleado;
    }
}
