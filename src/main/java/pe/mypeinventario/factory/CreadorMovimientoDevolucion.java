package pe.mypeinventario.factory;

import java.time.Clock;
import pe.mypeinventario.domain.MovimientoInventario;
import pe.mypeinventario.domain.TipoMovimiento;
import pe.mypeinventario.exception.OperacionInvalidaException;

/** Crea movimientos de entrada por devolución de un cliente, ligados a la venta original. */
public class CreadorMovimientoDevolucion extends CreadorMovimiento {

    public CreadorMovimientoDevolucion(Clock reloj) {
        super(reloj);
    }

    @Override
    public MovimientoInventario crear(OrigenMovimiento origen, int cantidad, String motivo) {
        if (origen == null || origen.detalleVenta() == null) {
            throw new OperacionInvalidaException("Una devolución requiere la línea de venta original como origen");
        }
        validarCantidad(cantidad, origen.detalleVenta().getCantidad());
        return construir(TipoMovimiento.DEVOLUCION_CLIENTE, cantidad,
                motivoOPorDefecto(motivo, "Devolución de cliente"), origen);
    }
}
