package com.laroca.modelo;

// Empleado que inició sesión. Se usa al registrar ventas y compras.
public final class Sesion {

    private static int idEmpleado;
    private static String usuario;

    private Sesion() {

    }

    // usuario: nombre con el que el empleado inicia sesión.
    public static void establecer(int id, String usu) {
        idEmpleado = id;
        usuario = usu;
    }

    public static int getIdEmpleado() {
        return idEmpleado;
    }

    public static String getUsuario() {
        return usuario;
    }

    public static boolean haySesion() {
        return idEmpleado > 0;
    }

    public static void cerrar() {
        idEmpleado = 0;
        usuario = null;
    }
}
