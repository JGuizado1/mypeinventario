package pe.mypeinventario.reporte;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Una variante con stock que no se vende hace tiempo.
 *
 * @param varianteId          identificador de la variante
 * @param sku                 código de la variante
 * @param producto            nombre del producto
 * @param talla               talla
 * @param color               color
 * @param stock               unidades en stock
 * @param ultimaVenta         fecha de la última venta, o {@code null} si nunca se vendió
 * @param capitalInmovilizado stock por costo, o {@code null} si el costo se desconoce
 */
public record VarianteSinRotacion(Long varianteId, String sku, String producto, String talla,
                                  String color, long stock, LocalDateTime ultimaVenta,
                                  BigDecimal capitalInmovilizado) {

    /** @return la fecha de la última venta como «06/11/2026», o «Nunca» si jamás se vendió */
    public String ultimaVentaTexto() {
        return ultimaVenta == null ? "Nunca" : DateTimeFormatter.ofPattern("dd/MM/yyyy").format(ultimaVenta);
    }
}
