package com.laroca.modelo;

import java.time.LocalDate;

public class Merma {

    private int idMerma;
    private int idEmpleado;
    private LocalDate fecha;
    private String estado;

    // Datos relacionados, únicamente para mostrar.
    private String nombreEmpleado;

    public Merma() {

    }

    public int getIdMerma() {
        return idMerma;
    }

    public void setIdMerma(int id) {
        idMerma = id;
    }

    // Folio visible, por ejemplo M-001.
    public String getFolio() {
        return String.format("M-%03d", idMerma);
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
}
