package pe.mypeinventario.reporte;

import java.math.BigDecimal;

/**
 * Totales de las ventas de un periodo, tal como están guardados. Un valor ausente
 * (periodo sin ventas) se normaliza a cero.
 *
 * @param cantidadVentas número de ventas del periodo
 * @param subtotal suma de los subtotales, antes de descuentos
 * @param descuento suma de los descuentos aplicados
 * @param total suma de los totales cobrados, con IGV
 * @param igv suma del IGV incluido en los totales
 */
public record TotalesVentas(Long cantidadVentas, BigDecimal subtotal, BigDecimal descuento,
                            BigDecimal total, BigDecimal igv) {

    public TotalesVentas {
        cantidadVentas = cantidadVentas == null ? 0L : cantidadVentas;
        subtotal = subtotal == null ? BigDecimal.ZERO : subtotal;
        descuento = descuento == null ? BigDecimal.ZERO : descuento;
        total = total == null ? BigDecimal.ZERO : total;
        igv = igv == null ? BigDecimal.ZERO : igv;
    }
}
