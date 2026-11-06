package pe.mypeinventario.web;

/** Lo que la persona escribe al agregar una variante. Todo llega como texto. */
public record FormularioVariante(String sku, String codigoBarras, String talla, String color,
                                 String precioCompra, String precioVenta, String stockMinimo) {

    public static FormularioVariante vacio() {
        return new FormularioVariante(null, null, null, null, null, null, null);
    }
}
