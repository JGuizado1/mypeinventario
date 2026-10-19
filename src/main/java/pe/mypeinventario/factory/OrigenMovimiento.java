package pe.mypeinventario.factory;

import java.util.Objects;
import pe.mypeinventario.domain.DetalleCompra;
import pe.mypeinventario.domain.DetalleVenta;

/**
 * Línea de venta o de compra que origina un movimiento de inventario. Un origen
 * no puede ser a la vez una venta y una compra.
 *
 * @param detalleVenta  línea de venta de origen, o {@code null}
 * @param detalleCompra línea de compra de origen, o {@code null}
 */
public record OrigenMovimiento(DetalleVenta detalleVenta, DetalleCompra detalleCompra) {

    public OrigenMovimiento {
        if (detalleVenta != null && detalleCompra != null) {
            throw new IllegalArgumentException(
                    "Un movimiento no puede originarse en una venta y una compra a la vez");
        }
    }

    /** @return un origen basado en una línea de venta */
    public static OrigenMovimiento deVenta(DetalleVenta detalle) {
        return new OrigenMovimiento(Objects.requireNonNull(detalle, "La línea de venta es obligatoria"), null);
    }

    /** @return un origen basado en una línea de compra */
    public static OrigenMovimiento deCompra(DetalleCompra detalle) {
        return new OrigenMovimiento(null, Objects.requireNonNull(detalle, "La línea de compra es obligatoria"));
    }

    /** @return un origen vacío, para movimientos sin documento de respaldo (ajustes) */
    public static OrigenMovimiento ninguno() {
        return new OrigenMovimiento(null, null);
    }
}
