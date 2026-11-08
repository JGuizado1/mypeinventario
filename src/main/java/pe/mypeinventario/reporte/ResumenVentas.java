package pe.mypeinventario.reporte;

import java.math.BigDecimal;

/**
 * Resumen de ventas, costo y utilidad de un periodo.
 *
 * @param cantidadVentas  ventas registradas
 * @param ventasBrutas    suma de las líneas, antes de descuentos
 * @param descuentos      descuentos aplicados
 * @param totalCobrado    lo que pagaron los clientes (IGV incluido)
 * @param igv             IGV contenido en el total cobrado
 * @param costo           costo de lo vendido, solo de líneas con costo conocido
 * @param utilidadBruta   total cobrado menos costo
 * @param lineasSinCosto  líneas vendidas sin costo conocido; si es mayor que cero, la
 *                        utilidad está sobrestimada
 */
public record ResumenVentas(long cantidadVentas, BigDecimal ventasBrutas, BigDecimal descuentos,
                            BigDecimal totalCobrado, BigDecimal igv, BigDecimal costo,
                            BigDecimal utilidadBruta, long lineasSinCosto) {
}
