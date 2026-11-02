package com.laroca.controlador;

import java.util.function.Consumer;

import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.shape.SVGPath;

// Celda con una ✕ roja que quita el renglón (Nueva Venta y Registrar Compra).
// La ✕ es una figura, no texto, para que la selección de la tabla no la
// pinte de otro color.
class CeldaQuitar<T> extends TableCell<T, Void> {

    private final Button boton = new Button();

    CeldaQuitar(Consumer<T> quitar) {
        SVGPath equis = new SVGPath();
        equis.setContent("M0,0 L8,8 M8,0 L0,8");
        equis.getStyleClass().add("icono-quitar");

        boton.setGraphic(equis);
        boton.getStyleClass().add("boton-quitar");
        boton.setFocusTraversable(false);
        boton.setOnAction(e -> {
            T renglon = getTableRow().getItem();
            if (renglon != null) {
                quitar.accept(renglon);
            }
        });
    }

    @Override
    protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        setGraphic(empty ? null : boton);
    }
}
