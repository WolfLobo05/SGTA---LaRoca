package com.laroca.modelo;

// Valores del campo estado que usan todos los catálogos.
// Los registros nunca se borran: se marcan como Inactivo.
public final class Estados {

    public static final String ACTIVO = "Activo";
    public static final String INACTIVO = "Inactivo";

    private Estados() {

    }

    // Compara sin distinguir mayúsculas; NULL o vacío cuenta como Activo.
    public static boolean esActivo(String estado) {
        return estado == null
                || estado.isBlank()
                || estado.trim().equalsIgnoreCase(ACTIVO);
    }

    // Texto que se muestra en el campo de solo lectura del formulario.
    public static String mostrar(String estado) {
        return esActivo(estado) ? ACTIVO : INACTIVO;
    }
}
