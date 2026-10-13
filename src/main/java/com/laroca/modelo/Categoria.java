package com.laroca.modelo;

public class Categoria
{
    private int idcategoria;
    private String estado;
    private String nombre;
    private String descripcion;

    public Categoria() 
    {

    }
    public Categoria(int id, String est, String nom, String desc)
    {
        idcategoria = id;
        estado = est;
        nombre = nom;
        descripcion = desc;
    }
    public int getIdCategoria() 
    {
        return idcategoria;
    }
    public String getEstado() 
    {
        return estado;
    }
    public String getNombre() 
    {
        return nombre;
    }
    public String getDescripcion() 
    {
        return descripcion;
    }
    public void setIdCategoria(int id)
    {
        idcategoria = id;
    }
    public void setEstado(String est) 
    {
        estado = est;
    }
    public void setNombre(String nom) 
    {
        nombre = nom;
    }
    public void setDescripcion(String desc) 
    {
        descripcion = desc;
    }
}