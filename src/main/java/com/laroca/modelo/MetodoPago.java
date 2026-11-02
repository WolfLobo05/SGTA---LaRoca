package com.laroca.modelo;

public class MetodoPago {

    private int idMetodo;
    private String nombre;

    public MetodoPago()
    {

    }

    public MetodoPago(int id, String nom)
    {
        idMetodo = id;
        nombre = nom;
    }

    public int getIdMetodo() {
        return idMetodo;
    }

    public void setIdMetodo(int id) {
        idMetodo = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nom) {
        nombre = nom;
    }
}
