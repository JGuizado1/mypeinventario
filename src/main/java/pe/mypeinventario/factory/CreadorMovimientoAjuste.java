package pe.mypeinventario.factory;

import java.time.Clock;
import pe.mypeinventario.domain.MovimientoInventario;
import pe.mypeinventario.domain.TipoMovimiento;
import pe.mypeinventario.exception.OperacionInvalidaException;

/**
 * Crea ajustes manuales de inventario (positivos o negativos). No tienen documento
 * de origen y exigen un motivo que explique la corrección.
 */
public class CreadorMovimientoAjuste extends CreadorMovimiento {

    private final TipoMovimiento tipo;

    /**
     * @param reloj fuente de la fecha del movimiento
     * @param tipo  {@code AJUSTE_POSITIVO} o {@code AJUSTE_NEGATIVO}
     * @throws OperacionInvalidaException si el tipo no es un ajuste
     */
    public CreadorMovimientoAjuste(Clock reloj, TipoMovimiento tipo) {
        super(reloj);
        if (tipo != TipoMovimiento.AJUSTE_POSITIVO && tipo != TipoMovimiento.AJUSTE_NEGATIVO) {
            throw new OperacionInvalidaException("El tipo de ajuste debe ser AJUSTE_POSITIVO o AJUSTE_NEGATIVO");
        }
        this.tipo = tipo;
    }

    @Override
    public MovimientoInventario crear(OrigenMovimiento origen, int cantidad, String motivo) {
        if (origen != null && (origen.detalleVenta() != null || origen.detalleCompra() != null)) {
            throw new OperacionInvalidaException("Un ajuste no puede tener una venta o compra de origen");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new OperacionInvalidaException("Un ajuste de inventario exige un motivo");
        }
        validarCantidad(cantidad, Integer.MAX_VALUE);
        return construir(tipo, cantidad, motivo.strip(), OrigenMovimiento.ninguno());
    }
}
