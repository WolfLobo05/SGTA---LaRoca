package com.laroca.reporte;

import java.time.LocalDate;
import java.util.List;

/**
 * Datos ya calculados por Estadísticas Generales.
 * Los exportadores solo presentan la información; no ejecutan consultas ni
 * modifican cálculos del dashboard.
 */
public record DatosReporteEstadisticas(
        LocalDate desde,
        LocalDate hasta,
        String categoria,
        LocalDate generado,
        List<Dato> indicadores,
        List<Seccion> secciones) {

    public DatosReporteEstadisticas {
        indicadores = List.copyOf(indicadores);
        secciones = List.copyOf(secciones);
    }

    public record Dato(String nombre, String valor, String detalle) {
        public Dato {
            nombre = nombre == null ? "" : nombre;
            valor = valor == null ? "" : valor;
            detalle = detalle == null ? "" : detalle;
        }
    }

    public record Seccion(String titulo, List<Dato> datos) {
        public Seccion {
            titulo = titulo == null ? "" : titulo;
            datos = datos == null ? List.of() : List.copyOf(datos);
        }
    }
}
