package pe.mypeinventario.web;

/**
 * Lo que la persona elige al vender, fuera de las cantidades (que llegan como campos
 * «cantidad-ID», uno por variante). Todo llega como texto.
 *
 * @param clienteId identificador del cliente, vacío si no hay
 * @param medioPago medio de pago elegido
 * @param descuentoPorcentaje descuento en porcentaje, como texto
 * @param descuentoMonto descuento en monto fijo, como texto
 */
public record FormularioVenta(String clienteId, String medioPago, String descuentoPorcentaje,
                              String descuentoMonto) {

    public static FormularioVenta vacio() {
        return new FormularioVenta("", "EFECTIVO", "", "");
    }
}
