package pe.mypeinventario.composite;

import java.math.BigDecimal;
import java.util.List;

/**
 * Componente del patrón Composite: ventas de una categoría. Puede ser una categoría
 * simple o un grupo que incluye a sus subcategorías; ambas se tratan igual.
 */
public interface NodoVentasCategoria {

    /** @return nombre de la categoría */
    String nombre();

    /** @return unidades vendidas, incluidas las de las subcategorías */
    long unidades();

    /** @return ventas a precio de lista, incluidas las de las subcategorías */
    BigDecimal ingresos();

    /** @return costo de lo vendido, incluido el de las subcategorías */
    BigDecimal costo();

    /** @return ingresos menos costo */
    default BigDecimal utilidad() {
        return ingresos().subtract(costo());
    }

    /** @return subcategorías con ventas; vacío si es una categoría simple */
    default List<NodoVentasCategoria> subcategorias() {
        return List.of();
    }
}
