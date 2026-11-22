package com.laroca.controlador;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

// Formato único de dinero, cantidades y fechas para toda la aplicación.
// Dinero en formato MX: $1,234.00 (negativos: -$1,234.00).
public final class Formato {

    // Separadores fijos ($1,234.50) sin depender de la configuración de Windows.
    private static final DecimalFormatSymbols SIMBOLOS =
            DecimalFormatSymbols.getInstance(Locale.US);

    private static final DecimalFormat DINERO = new DecimalFormat("$#,##0.00", SIMBOLOS);

    private static final DecimalFormat ENTERO = new DecimalFormat("#,##0", SIMBOLOS);

    public static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static final DateTimeFormatter FECHA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Formato() {

    }

    public static String dinero(BigDecimal cantidad) {
        return cantidad == null ? "" : DINERO.format(cantidad);
    }

    public static String dinero(double cantidad) {
        return DINERO.format(cantidad);
    }

    // Cantidades enteras con separador de miles: 1,234.
    public static String entero(long cantidad) {
        return ENTERO.format(cantidad);
    }

    public static String fecha(LocalDateTime fecha) {
        return fecha == null ? "" : fecha.format(FECHA);
    }

    public static String fecha(LocalDate fecha) {
        return fecha == null ? "" : fecha.format(FECHA);
    }

    public static String fechaHora(LocalDateTime fecha) {
        return fecha == null ? "" : fecha.format(FECHA_HORA);
    }
}
