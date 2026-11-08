package pe.mypeinventario.web;

/**
 * Lo que la persona elige al vender, fuera de las cantidades (que llegan como campos
 * «cantidad-ID», uno por variante). Todo llega como texto.
 */
public record FormularioVenta(String clienteId, String medioPago, String descuentoPorcentaje,
                              String descuentoMonto) {

    public static FormularioVenta vacio() {
        return new FormularioVenta("", "EFECTIVO", "", "");
    }
}
