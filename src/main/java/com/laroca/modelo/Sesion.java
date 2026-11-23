package com.laroca.modelo;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

public final class Sesion {
    private static int idEmpleado;
    private static String usuario;
    private static String nombreCompleto;
    private static String rol;

    private static final Set<String> PERMISOS_AUXILIAR = Set.of(
            "PRODUCTOS", "UNIDADES", "MERMAS", "DEVOLUCIONES_CLIENTES",
            "DEVOLUCIONES_PROVEEDORES", "CLIENTES", "VENTAS", "COMPRAS",
            "CONSULTAR_INVENTARIO");

    private static final Set<String> PERMISOS_CAJERO = Set.of(
            "VENTAS", "CLIENTES", "DEVOLUCIONES_CLIENTES");

    private Sesion() { }

    public static void establecer(int id, String usu) {
        establecer(id, usu, null, null);
    }

    public static void establecer(int id, String usu, String nombre) {
        establecer(id, usu, nombre, null);
    }

    public static void establecer(int id, String usu, String nombre, String nombreRol) {
        idEmpleado = id;
        usuario = usu;
        nombreCompleto = nombre == null || nombre.isBlank() ? null : nombre.trim();
        rol = normalizar(nombreRol);
    }

    public static int getIdEmpleado() {
        return idEmpleado;
    }

    public static String getUsuario() {
        return usuario;
    }

    public static String getRol() {
        return rol;
    }

    public static String getNombreParaMostrar() {
        return nombreCompleto != null ? nombreCompleto : usuario;
    }

    // Nombre del rol para mostrar en pantalla (menú y bienvenida).
    public static String getRolParaMostrar() {
        if (esAdministrador()) {
            return "Administrador";
        }

        if ("AUXILIAR".equals(rol)) {
            return "Auxiliar";
        }

        if ("CAJERO".equals(rol)) {
            return "Cajero";
        }

        return "Usuario";
    }

    public static boolean haySesion() {
        return idEmpleado > 0;
    }

    public static boolean esAdministrador() {
        return "ADMINISTRADOR".equals(rol)
                || "ADMIN".equals(rol)
                || "ADMINISTRATIVE".equals(rol)
                || "ADMINISTRATOR".equals(rol);
    }

    public static boolean tienePermiso(String permiso) {
        if (!haySesion() || rol == null) {
            return false;
        }

        if (esAdministrador()) {
            return true;
        }

        String clave = normalizar(permiso);

        if ("AUXILIAR".equals(rol)) {
            return PERMISOS_AUXILIAR.contains(clave);
        }

        if ("CAJERO".equals(rol)) {
            return PERMISOS_CAJERO.contains(clave);
        }

        return false;
    }

    private static String normalizar(String valor) {
        if (valor == null) {
            return null;
        }

        return Normalizer.normalize(valor.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }

    public static void cerrar() {
        idEmpleado = 0;
        usuario = null;
        nombreCompleto = null;
        rol = null;
    }
}
