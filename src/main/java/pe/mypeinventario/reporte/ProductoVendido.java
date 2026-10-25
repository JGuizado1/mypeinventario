package pe.mypeinventario.reporte;

import java.math.BigDecimal;

/**
 * Una variante con lo vendido en un periodo.
 *
 * @param varianteId identificador de la variante
 * @param sku        código de la variante
 * @param producto   nombre del producto
 * @param talla      talla
 * @param color      color
 * @param unidades   unidades vendidas
 * @param ingresos   ventas a precio de lista, sin repartir descuentos
 */
public record ProductoVendido(Long varianteId, String sku, String producto, String talla,
                              String color, Long unidades, BigDecimal ingresos) {
}
