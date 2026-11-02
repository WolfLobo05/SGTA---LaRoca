package com.laroca.controlador;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

// Formato de dinero y fechas que usan las pantallas de ventas.
final class Formato {

    // Separadores fijos ($1,234.50) sin depender de la configuración de Windows.
    private static final DecimalFormat DINERO = new DecimalFormat(
            "$#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));

    static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Formato() {

    }

    static String dinero(BigDecimal cantidad) {
        return cantidad == null ? "" : DINERO.format(cantidad);
    }

    static String fecha(LocalDateTime fecha) {
        return fecha == null ? "" : fecha.format(FECHA);
    }

    static String fecha(LocalDate fecha) {
        return fecha == null ? "" : fecha.format(FECHA);
    }
}
