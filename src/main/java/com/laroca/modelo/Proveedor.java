package com.laroca.modelo;

public class Proveedor {
    private int idProveedor;
    private String rfc;
    private String nombre;
    private String telefono;
    private String correo;
    private String direccion;
    private String contacto;
    private String estado;

    public Proveedor() {}

    public Proveedor(int idProveedor, String rfc, String nombre, String telefono, String correo, String direccion, String contacto, String estado) {
        this.idProveedor = idProveedor;
        this.rfc = rfc;
        this.nombre = nombre;
        this.telefono = telefono;
        this.correo = correo;
        this.direccion = direccion;
        this.contacto = contacto;
        this.estado = estado;
    }

    public int getIdProveedor() { return idProveedor; }
    public void setIdProveedor(int idProveedor) { this.idProveedor = idProveedor; }

    public String getRfc() { return rfc; }
    public void setRfc(String rfc) { this.rfc = rfc; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getContacto() { return contacto; }
    public void setContacto(String contacto) { this.contacto = contacto; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
