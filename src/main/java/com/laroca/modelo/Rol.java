package com.laroca.modelo;

public class Rol {

    private int idRol;
    private String estado;
    private String nombre;
    private String descripcion;

    public Rol() 
    {

    }

    public Rol(int id, String est, String nom, String desc) 
    {
        idRol = id;
        estado = est;
        nombre = nom;
        descripcion = desc;
    }

    public int getIdRol() {
        return idRol;
    }

    public void setIdRol(int id) {
        idRol = id;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String est) {
        estado = est;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nom) {
        nombre = nom;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String desc) {
        descripcion = desc;
    }
}
