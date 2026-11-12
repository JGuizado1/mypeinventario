package pe.mypeinventario.web;

/**
 * Lo que la persona escribe al crear un producto con su primera variante. Todo llega como texto.
 *
 * @param nombre nombre del producto
 * @param descripcion descripción opcional
 * @param categoriaId identificador de la categoría
 * @param marcaId identificador de la marca, opcional
 * @param sku código SKU de la primera variante
 * @param codigoBarras código de barras, opcional
 * @param talla talla de la variante
 * @param color color de la variante
 * @param precioCompra costo por unidad, como texto
 * @param precioVenta precio de venta por unidad, como texto
 * @param stockMinimo stock mínimo antes de alertar, como texto
 */
public record FormularioProducto(String nombre, String descripcion, Long categoriaId, Long marcaId,
                                 String sku, String codigoBarras, String talla, String color,
                                 String precioCompra, String precioVenta, String stockMinimo) {

    public static FormularioProducto vacio() {
        return new FormularioProducto(null, null, null, null, null, null, null, null, null, null, null);
    }
}
