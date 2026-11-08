package pe.mypeinventario.web;

/** Lo que la persona escribe al crear un producto con su primera variante. Todo llega como texto. */
public record FormularioProducto(String nombre, String descripcion, Long categoriaId, Long marcaId,
                                 String sku, String codigoBarras, String talla, String color,
                                 String precioCompra, String precioVenta, String stockMinimo) {

    public static FormularioProducto vacio() {
        return new FormularioProducto(null, null, null, null, null, null, null, null, null, null, null);
    }
}
