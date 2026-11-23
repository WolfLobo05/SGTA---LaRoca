package com.laroca.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;

// Un renglón del Reporte de Inventario.
//
// stock es Productos.existencia: la existencia actual que mantienen
// compras, ventas, mermas y devoluciones (y sus cancelaciones).
public class FilaInventario {

    private final int idProducto;
    private final String codigo;
    private final String producto;
    private final String categoria;
    private final String unidad;

    private final int stock;
    private final BigDecimal precio;
    private final Integer puntoReorden;

    public FilaInventario(int idProducto, String codigo, String producto,
            String categoria, String unidad,
            int stock, BigDecimal precio, Integer puntoReorden) {

        this.idProducto = idProducto;
        this.codigo = codigo;
        this.producto = producto;
        this.categoria = categoria;
        this.unidad = unidad;
        this.stock = stock;
        this.precio = precio;
        this.puntoReorden = puntoReorden;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getProducto() {
        return producto;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getUnidad() {
        return unidad;
    }

    public int getStock() {
        return stock;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    // Valor = stock × precio. Sin precio, el valor es 0.
    // Con stock negativo el valor también es negativo (no se oculta).
    public BigDecimal getValor() {
        return precio == null
                ? BigDecimal.ZERO.setScale(2)
                : precio.multiply(BigDecimal.valueOf(stock)).setScale(2, RoundingMode.HALF_UP);
    }

    public Integer getPuntoReorden() {
        return puntoReorden;
    }

    // stock <= punto_reorden. Sin punto de reorden (NULL) no se marca.
    public boolean isReordenar() {
        return puntoReorden != null && stock <= puntoReorden;
    }

    // Existencia negativa: dato incorrecto que hay que revisar.
    public boolean isStockNegativo() {
        return stock < 0;
    }
}
