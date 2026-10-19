package pe.mypeinventario.decorator;

import java.math.BigDecimal;

/**
 * Componente del patrón Decorator: un importe a cobrar que puede ir
 * acompañado de una descripción de los conceptos aplicados.
 */
public interface CalculoImporte {

    /** @return importe resultante, con dos decimales */
    BigDecimal importe();

    /** @return texto que resume los conceptos aplicados, por ejemplo "Subtotal + descuento 10 %" */
    String descripcion();
}
