package pe.mypeinventario.composite;

import java.math.BigDecimal;

/**
 * Hoja del Composite: las ventas de una categoría, sin subcategorías. También sirve
 * para representar las ventas propias de una categoría que sí tiene subcategorías.
 *
 * @param nombre   nombre de la categoría
 * @param unidades unidades vendidas
 * @param ingresos ventas a precio de lista
 * @param costo    costo de lo vendido
 */
public record VentasDeCategoria(String nombre, long unidades, BigDecimal ingresos, BigDecimal costo)
        implements NodoVentasCategoria {

    public VentasDeCategoria {
        ingresos = ingresos == null ? BigDecimal.ZERO : ingresos;
        costo = costo == null ? BigDecimal.ZERO : costo;
    }
}
