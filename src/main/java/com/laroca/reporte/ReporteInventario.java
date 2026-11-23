package com.laroca.reporte;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.laroca.modelo.FilaInventario;

// Reporte de Inventario ya generado: filtros, filas y totales.
// La pantalla, la impresión, el PDF y el Excel usan este mismo objeto,
// así todos muestran exactamente los mismos números.
public final class ReporteInventario {

    public static final String NOTA_STOCK =
            "Stock = existencia actual registrada al generar el reporte.";

    public static final String NOTA_REORDEN =
            "Reordenar = stock igual o menor que el punto de reorden del producto.";

    private final String tipo;
    private final String categoria;
    private final String filtroReorden;
    private final LocalDateTime generadoEn;
    private final String generadoPor;
    private final List<FilaInventario> filas;

    private final int totalStock;
    private final BigDecimal totalValor;
    private final int productosReordenar;
    private final int productosNegativos;

    // filtroReorden: null cuando es "Todos".
    public ReporteInventario(String tipo,
            String categoria, String filtroReorden,
            LocalDateTime generadoEn, String generadoPor,
            List<FilaInventario> filas) {

        this.tipo = tipo;
        this.categoria = categoria;
        this.filtroReorden = filtroReorden;
        this.generadoEn = generadoEn;
        this.generadoPor = generadoPor;
        this.filas = List.copyOf(filas);

        int stock = 0;
        int reordenar = 0;
        int negativos = 0;
        BigDecimal valor = BigDecimal.ZERO.setScale(2);

        for (FilaInventario fila : this.filas) {
            stock += fila.getStock();
            valor = valor.add(fila.getValor());

            if (fila.isReordenar()) {
                reordenar++;
            }
            if (fila.isStockNegativo()) {
                negativos++;
            }
        }

        totalStock = stock;
        totalValor = valor;
        productosReordenar = reordenar;
        productosNegativos = negativos;
    }

    public String getTipo() {
        return tipo;
    }

    // Día al que corresponden las existencias (el día en que se generó).
    public LocalDate getFechaExistencias() {
        return generadoEn.toLocalDate();
    }

    public String getCategoria() {
        return categoria;
    }

    public String getFiltroReorden() {
        return filtroReorden;
    }

    public LocalDateTime getGeneradoEn() {
        return generadoEn;
    }

    public String getGeneradoPor() {
        return generadoPor;
    }

    public List<FilaInventario> getFilas() {
        return filas;
    }

    // Tarjeta "Productos".
    public int getTotalProductos() {
        return filas.size();
    }

    // Tarjeta "Unidades en existencia" y total de la columna Stock.
    public int getTotalStock() {
        return totalStock;
    }

    // Tarjeta "Valor del inventario" y total de la columna Valor.
    public BigDecimal getTotalValor() {
        return totalValor;
    }

    // Tarjeta "Productos a reordenar".
    public int getProductosReordenar() {
        return productosReordenar;
    }

    public int getProductosNegativos() {
        return productosNegativos;
    }
}
