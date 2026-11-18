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

    public static final String NOTA_FORMULA =
            "Entradas = compras + devoluciones de clientes · "
          + "Salidas = ventas + mermas + devoluciones a proveedores";

    public static final String NOTA_STOCK =
            "Stock = existencia actual registrada al generar el reporte; "
          + "entradas y salidas son solo del periodo.";

    private final String tipo;
    private final LocalDate desde;
    private final LocalDate hasta;
    private final String categoria;
    private final String filtroReorden;
    private final LocalDateTime generadoEn;
    private final String generadoPor;
    private final List<FilaInventario> filas;

    private final int totalEntradas;
    private final int totalSalidas;
    private final int totalStock;
    private final BigDecimal totalValor;
    private final int productosReordenar;
    private final int productosNegativos;

    // filtroReorden: null cuando es "Todos".
    public ReporteInventario(String tipo, LocalDate desde, LocalDate hasta,
            String categoria, String filtroReorden,
            LocalDateTime generadoEn, String generadoPor,
            List<FilaInventario> filas) {

        this.tipo = tipo;
        this.desde = desde;
        this.hasta = hasta;
        this.categoria = categoria;
        this.filtroReorden = filtroReorden;
        this.generadoEn = generadoEn;
        this.generadoPor = generadoPor;
        this.filas = List.copyOf(filas);

        int entradas = 0;
        int salidas = 0;
        int stock = 0;
        int reordenar = 0;
        int negativos = 0;
        BigDecimal valor = BigDecimal.ZERO.setScale(2);

        for (FilaInventario fila : this.filas) {
            entradas += fila.getEntradas();
            salidas += fila.getSalidas();
            stock += fila.getStock();
            valor = valor.add(fila.getValor());

            if (fila.isReordenar()) {
                reordenar++;
            }
            if (fila.isStockNegativo()) {
                negativos++;
            }
        }

        totalEntradas = entradas;
        totalSalidas = salidas;
        totalStock = stock;
        totalValor = valor;
        productosReordenar = reordenar;
        productosNegativos = negativos;
    }

    public String getTipo() {
        return tipo;
    }

    public LocalDate getDesde() {
        return desde;
    }

    public LocalDate getHasta() {
        return hasta;
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

    public int getTotalEntradas() {
        return totalEntradas;
    }

    public int getTotalSalidas() {
        return totalSalidas;
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
