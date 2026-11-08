package pe.mypeinventario.consulta;

import java.math.BigDecimal;

/**
 * Una línea de una compra ya registrada.
 *
 * @param varianteId    variante comprada
 * @param sku           código interno de la variante
 * @param producto      nombre del producto
 * @param talla         talla, o {@code null}
 * @param color         color, o {@code null}
 * @param cantidad      unidades compradas
 * @param costoUnitario costo por unidad
 * @param subtotal      costo por unidad multiplicado por la cantidad
 */
public record LineaCompraResumen(Long varianteId, String sku, String producto, String talla, String color,
                                 int cantidad, BigDecimal costoUnitario, BigDecimal subtotal) {

    /** @return la variante en una frase corta, por ejemplo «M / Azul» */
    public String variante() {
        return TextoVariante.de(talla, color);
    }
}
