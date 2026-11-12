package pe.mypeinventario.web;

/**
 * Lo que la persona escribe al agregar una variante. Todo llega como texto.
 *
 * @param sku código SKU de la variante
 * @param codigoBarras código de barras, opcional
 * @param talla talla de la variante
 * @param color color de la variante
 * @param precioCompra costo por unidad, como texto
 * @param precioVenta precio de venta por unidad, como texto
 * @param stockMinimo stock mínimo antes de alertar, como texto
 */
public record FormularioVariante(String sku, String codigoBarras, String talla, String color,
                                 String precioCompra, String precioVenta, String stockMinimo) {

    public static FormularioVariante vacio() {
        return new FormularioVariante(null, null, null, null, null, null, null);
    }
}
