package pe.mypeinventario.consulta;

import java.math.BigDecimal;

/**
 * Una línea de una venta ya registrada, con el precio que tenía al momento de vender.
 *
 * @param detalleId      identificador de la línea (lo necesitan las devoluciones)
 * @param varianteId     variante vendida
 * @param sku            código interno de la variante
 * @param producto       nombre del producto
 * @param talla          talla, o {@code null}
 * @param color          color, o {@code null}
 * @param cantidad       unidades vendidas
 * @param precioUnitario precio por unidad al momento de la venta
 * @param subtotal       precio por unidad multiplicado por la cantidad
 * @param devueltas      unidades que el cliente ya devolvió de esta línea
 */
public record LineaVentaResumen(Long detalleId, Long varianteId, String sku, String producto, String talla,
                                String color, int cantidad, BigDecimal precioUnitario, BigDecimal subtotal,
                                int devueltas) {

    /** @return la variante en una frase corta, por ejemplo «M / Azul» */
    public String variante() {
        return TextoVariante.de(talla, color);
    }

    /** @return unidades que todavía se pueden devolver */
    public int porDevolver() {
        return cantidad - devueltas;
    }
}
