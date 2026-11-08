package pe.mypeinventario.consulta;

import java.math.BigDecimal;

/**
 * Vista de solo lectura de una variante (talla y color) con su stock calculado.
 *
 * @param id           identificador
 * @param sku          código interno, único dentro de la tienda
 * @param codigoBarras código de barras, o {@code null}
 * @param talla        talla, o {@code null}
 * @param color        color, o {@code null}
 * @param precioCompra costo de compra, o {@code null} si no se conoce
 * @param precioVenta  precio de venta (incluye IGV)
 * @param stockMinimo  stock mínimo vigente: el propio o el valor por defecto de la tienda
 * @param stock        unidades disponibles
 * @param stockBajo    {@code true} si el stock alcanzó o bajó del mínimo
 */
public record VarianteResumen(Long id, String sku, String codigoBarras, String talla, String color,
                              BigDecimal precioCompra, BigDecimal precioVenta,
                              int stockMinimo, int stock, boolean stockBajo) {

    /** @return la variante en una frase corta, por ejemplo «M / Azul» */
    public String descripcion() {
        return TextoVariante.de(talla, color);
    }
}
