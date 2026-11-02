package com.laroca.controlador;

import com.laroca.dao.ProductoDAO;
import com.laroca.modelo.Producto;

import javafx.stage.Window;

// Buscador de productos activos que comparten Nueva Venta y Registrar Compra.
final class BuscadorProductos {

    private BuscadorProductos() {

    }

    // Abre el buscador con el filtro indicado. Devuelve el producto elegido o null.
    static Producto buscar(Window duenio, String filtro) {
        ProductoDAO productoDAO = new ProductoDAO();

        DialogoSeleccion<Producto> dialogo = new DialogoSeleccion<>(
                duenio, "Buscar producto",
                texto -> productoDAO.buscarActivos(texto));

        dialogo.agregarColumna("Código", Producto::getCodigoBarras, 130, false);
        dialogo.agregarColumna("Nombre", Producto::getNombre, 200, false);
        dialogo.agregarColumna("Precio", p -> Formato.dinero(p.getPrecio()), 85, true);
        dialogo.agregarColumna("Existencia", p -> String.valueOf(p.getExistencia()), 85, true);

        return dialogo.mostrar(filtro);
    }
}
