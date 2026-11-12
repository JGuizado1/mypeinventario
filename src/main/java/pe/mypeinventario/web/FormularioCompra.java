package pe.mypeinventario.web;

/**
 * Lo que la persona elige al comprar, fuera de las cantidades y los costos (que llegan como
 * campos «cantidad-ID» y «costo-ID», uno por variante). Todo llega como texto.
 *
 * @param proveedorId identificador del proveedor elegido, como texto
 */
public record FormularioCompra(String proveedorId) {

    public static FormularioCompra vacio() {
        return new FormularioCompra("");
    }
}
