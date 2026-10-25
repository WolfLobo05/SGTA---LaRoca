
package com.laroca.modelo;

public class Cliente {

    private int idCliente;
    private String nombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String telefono;
    private String correo;

    public Cliente()
    {

    }

    public Cliente(int id, String nom, String apPaterno,
            String apMaterno, String tel, String corr)
    {
        idCliente = id;
        nombre = nom;
        apellidoPaterno = apPaterno;
        apellidoMaterno = apMaterno;
        telefono = tel;
        correo = corr;
    }

    public int getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(int id) {
        idCliente = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nom) {
        nombre = nom;
    }

    public String getApellidoPaterno() {
        return apellidoPaterno;
    }

    public void setApellidoPaterno(String apPaterno) {
        apellidoPaterno = apPaterno;
    }

    public String getApellidoMaterno() {
        return apellidoMaterno;
    }

    public void setApellidoMaterno(String apMaterno) {
        apellidoMaterno = apMaterno;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String tel) {
        telefono = tel;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String corr) {
        correo = corr;
    }
}