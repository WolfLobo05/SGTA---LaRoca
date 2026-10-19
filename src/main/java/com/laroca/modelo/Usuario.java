package com.laroca.modelo;

// Representa un registro de la tabla Empleados.
public class Usuario {

    private int idUsuario;
    private int idRol;
    private String estado;
    private String usuario;
    private String passwordHash;
    private String nombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String telefono;
    private String correo;

    // Dato de la tabla Roles, solo para mostrar.
    private String nombreRol;

    public Usuario()
    {

    }

    public Usuario(int id, int idR, String est, String usu, String pass,
                   String nom, String apPat, String apMat, String tel, String cor)
    {
        idUsuario = id;
        idRol = idR;
        estado = est;
        usuario = usu;
        passwordHash = pass;
        nombre = nom;
        apellidoPaterno = apPat;
        apellidoMaterno = apMat;
        telefono = tel;
        correo = cor;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int id) {
        idUsuario = id;
    }

    public int getIdRol() {
        return idRol;
    }

    public void setIdRol(int idR) {
        idRol = idR;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String est) {
        estado = est;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usu) {
        usuario = usu;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String pass) {
        passwordHash = pass;
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

    public void setApellidoPaterno(String apPat) {
        apellidoPaterno = apPat;
    }

    public String getApellidoMaterno() {
        return apellidoMaterno;
    }

    public void setApellidoMaterno(String apMat) {
        apellidoMaterno = apMat;
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

    public void setCorreo(String cor) {
        correo = cor;
    }

    public String getNombreRol() {
        return nombreRol;
    }

    public void setNombreRol(String nomRol) {
        nombreRol = nomRol;
    }

    // Une nombre y apellidos, omitiendo los que estén vacíos.
    public String getNombreCompleto() {
        StringBuilder completo = new StringBuilder();

        for (String parte : new String[] {nombre, apellidoPaterno, apellidoMaterno}) {
            if (parte != null && !parte.isBlank()) {
                if (completo.length() > 0) {
                    completo.append(" ");
                }
                completo.append(parte.trim());
            }
        }

        return completo.toString();
    }
}
