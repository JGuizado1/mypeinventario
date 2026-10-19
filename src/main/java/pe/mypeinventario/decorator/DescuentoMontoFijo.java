package pe.mypeinventario.decorator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import pe.mypeinventario.exception.OperacionInvalidaException;

/**
 * Decorador concreto: descuenta un monto fijo (por ejemplo, un cupón). El
 * importe nunca baja de cero.
 */
public class DescuentoMontoFijo extends DecoradorImporte {

    private final BigDecimal monto;

    /**
     * @param envuelto importe al que se aplica el descuento
     * @param monto    monto a descontar, mayor que cero
     * @throws OperacionInvalidaException si el monto es nulo o no es positivo
     */
    public DescuentoMontoFijo(CalculoImporte envuelto, BigDecimal monto) {
        super(envuelto);
        if (monto == null || monto.signum() <= 0) {
            throw new OperacionInvalidaException("El monto del descuento debe ser mayor que cero");
        }
        this.monto = monto.setScale(ESCALA, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal importe() {
        return envuelto.importe().subtract(monto).max(BigDecimal.ZERO.setScale(ESCALA));
    }

    @Override
    public String descripcion() {
        return envuelto.descripcion() + " + descuento fijo de " + monto.toPlainString();
    }
}
