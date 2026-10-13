package com.laroca.modelo;

public class UnidadMedida 
{
    private int idUnidadMedida;
    private String estado;
    private String abreviatura;
    private String nombre;

    public UnidadMedida() 
    {

    }
    public UnidadMedida(int id, String est, String ab, String nom)
    {
        idUnidadMedida = id;
        estado = est;
        abreviatura = ab;
        nombre = nom;
    }
    public int getIdUnidadMedida() 
    {
        return idUnidadMedida;
    }
    public String getEstado() 
    {
        return estado;
    }
    public String getAbreviatura() 
    {
        return abreviatura;
    }
    public String getNombre() 
    {
        return nombre;
    }
    public void setIdUnidadMedida(int id)
    {
        idUnidadMedida = id;
    }
    public void setEstado(String est) 
    {
        estado = est;
    }
    public void setAbreviatura(String ab) 
    {
        abreviatura = ab;
    }
    public void setNombre(String nom) 
    {
        nombre = nom;
    }
}
