package pe.mypeinventario.service.dto;

import java.math.BigDecimal;
import java.util.List;
import pe.mypeinventario.domain.MedioPago;
import pe.mypeinventario.exception.OperacionInvalidaException;

/**
 * Datos para registrar una venta.
 *
 * @param tiendaId             tienda donde se vende
 * @param clienteId            cliente, o {@code null} si es una venta anónima
 * @param medioPago            medio de pago
 * @param lineas               líneas de la venta, al menos una
 * @param descuentoPorcentaje  porcentaje de descuento (por ejemplo 10), o {@code null}
 * @param descuentoMonto       descuento de monto fijo, o {@code null}
 */
public record SolicitudVenta(Long tiendaId, Long clienteId, MedioPago medioPago, List<LineaVenta> lineas,
                             BigDecimal descuentoPorcentaje, BigDecimal descuentoMonto) {

    public SolicitudVenta {
        if (tiendaId == null) {
            throw new OperacionInvalidaException("La tienda es obligatoria");
        }
        if (medioPago == null) {
            throw new OperacionInvalidaException("El medio de pago es obligatorio");
        }
        if (lineas == null || lineas.isEmpty()) {
            throw new OperacionInvalidaException("Una venta debe tener al menos una línea");
        }
        lineas = List.copyOf(lineas);
    }
}
