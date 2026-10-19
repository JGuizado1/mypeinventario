package pe.mypeinventario.factory;

import java.time.Clock;
import pe.mypeinventario.domain.MovimientoInventario;
import pe.mypeinventario.domain.TipoMovimiento;
import pe.mypeinventario.exception.OperacionInvalidaException;

/** Crea movimientos de entrada por compra a un proveedor. */
public class CreadorMovimientoCompra extends CreadorMovimiento {

    public CreadorMovimientoCompra(Clock reloj) {
        super(reloj);
    }

    @Override
    public MovimientoInventario crear(OrigenMovimiento origen, int cantidad, String motivo) {
        if (origen == null || origen.detalleCompra() == null) {
            throw new OperacionInvalidaException("Un movimiento de compra requiere una línea de compra como origen");
        }
        validarCantidad(cantidad, origen.detalleCompra().getCantidad());
        return construir(TipoMovimiento.COMPRA, cantidad,
                motivoOPorDefecto(motivo, "Compra de mercadería"), origen);
    }
}
