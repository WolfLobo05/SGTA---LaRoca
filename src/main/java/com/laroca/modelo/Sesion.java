package com.laroca.modelo;

// Empleado que inició sesión. Se usa al registrar ventas y compras
// y como autor en los reportes.
public final class Sesion {

    private static int idEmpleado;
    private static String usuario;
    private static String nombreCompleto;

    private Sesion() {

    }

    // usuario: nombre con el que el empleado inicia sesión.
    public static void establecer(int id, String usu) {
        establecer(id, usu, null);
    }

    // nombre: nombre y apellidos del empleado; si falta se usa el usuario.
    public static void establecer(int id, String usu, String nombre) {
        idEmpleado = id;
        usuario = usu;
        nombreCompleto = nombre == null || nombre.isBlank() ? null : nombre.trim();
    }

    public static int getIdEmpleado() {
        return idEmpleado;
    }

    public static String getUsuario() {
        return usuario;
    }

    // Nombre para mostrar: nombre completo o, si no hay, el usuario.
    public static String getNombreParaMostrar() {
        return nombreCompleto != null ? nombreCompleto : usuario;
    }

    public static boolean haySesion() {
        return idEmpleado > 0;
    }

    public static void cerrar() {
        idEmpleado = 0;
        usuario = null;
        nombreCompleto = null;
    }
}
