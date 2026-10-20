package com.laroca.modelo;

import java.math.BigDecimal;

public class Producto {

    private int idProducto;
    private int idUnidad;
    private int idCategoria;
    private String codigoBarras;
    private String nombre;
    private BigDecimal precio;

    // Datos de las tablas relacionadas, solo para mostrar.
    private String nombreUnidad;
    private String nombreCategoria;

    public Producto()
    {

    }

    public Producto(int id, int idUni, int idCat, String cod, String nom, BigDecimal pre)
    {
        idProducto = id;
        idUnidad = idUni;
        idCategoria = idCat;
        codigoBarras = cod;
        nombre = nom;
        precio = pre;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(int id) {
        idProducto = id;
    }

    public int getIdUnidad() {
        return idUnidad;
    }

    public void setIdUnidad(int idUni) {
        idUnidad = idUni;
    }

    public int getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(int idCat) {
        idCategoria = idCat;
    }

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public void setCodigoBarras(String cod) {
        codigoBarras = cod;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nom) {
        nombre = nom;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal pre) {
        precio = pre;
    }

    public String getNombreUnidad() {
        return nombreUnidad;
    }

    public void setNombreUnidad(String nomUni) {
        nombreUnidad = nomUni;
    }

    public String getNombreCategoria() {
        return nombreCategoria;
    }

    public void setNombreCategoria(String nomCat) {
        nombreCategoria = nomCat;
    }
}
