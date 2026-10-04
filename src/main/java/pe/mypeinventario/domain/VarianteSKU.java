package pe.mypeinventario.domain;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.mypeinventario.config.ConfiguracionTienda;

/**
 * Unidad mínima de inventario: una combinación de talla y color de un
 * {@link ProductoItem}, con su código, precios y stock mínimo propios.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VarianteSKU {

    private Long id;
    private String sku;
    private String codigoBarras;
    private String talla;
    private String color;
    private BigDecimal precioCompra;
    private BigDecimal precioVenta;
    /** Stock mínimo propio; si es {@code null} se usa el valor por defecto de la tienda. */
    private Integer stockMinimo;

    /**
     * Determina el stock mínimo que aplica a esta variante: el propio, si lo tiene,
     * o el valor por defecto de la configuración. La configuración se recibe como
     * parámetro para que la dependencia sea visible y se pueda probar con otra.
     *
     * @param config configuración global de la tienda
     * @return el stock mínimo vigente para esta variante
     * @throws IllegalArgumentException si la configuración es nula
     */
    public int resolverStockMinimo(ConfiguracionTienda config) {
        if (config == null) {
            throw new IllegalArgumentException("La configuración es obligatoria");
        }
        return stockMinimo != null ? stockMinimo : config.getStockMinimoPorDefecto();
    }
}
