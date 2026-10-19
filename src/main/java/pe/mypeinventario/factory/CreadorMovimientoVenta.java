package pe.mypeinventario.factory;

import java.time.Clock;
import pe.mypeinventario.domain.MovimientoInventario;
import pe.mypeinventario.domain.TipoMovimiento;
import pe.mypeinventario.exception.OperacionInvalidaException;

/** Crea movimientos de salida por venta en el punto de venta. */
public class CreadorMovimientoVenta extends CreadorMovimiento {

    public CreadorMovimientoVenta(Clock reloj) {
        super(reloj);
    }

    @Override
    public MovimientoInventario crear(OrigenMovimiento origen, int cantidad, String motivo) {
        if (origen == null || origen.detalleVenta() == null) {
            throw new OperacionInvalidaException("Un movimiento de venta requiere una línea de venta como origen");
        }
        validarCantidad(cantidad, origen.detalleVenta().getCantidad());
        return construir(TipoMovimiento.VENTA, cantidad,
                motivoOPorDefecto(motivo, "Venta en punto de venta"), origen);
    }
}
