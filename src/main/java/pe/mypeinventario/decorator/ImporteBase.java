package pe.mypeinventario.decorator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import pe.mypeinventario.exception.OperacionInvalidaException;

/** Componente concreto: el subtotal de una venta, antes de aplicar descuentos. */
public class ImporteBase implements CalculoImporte {

    private final BigDecimal subtotal;

    /**
     * @param subtotal importe de partida, no negativo
     * @throws OperacionInvalidaException si es nulo o negativo
     */
    public ImporteBase(BigDecimal subtotal) {
        if (subtotal == null || subtotal.signum() < 0) {
            throw new OperacionInvalidaException("El subtotal no puede ser nulo ni negativo");
        }
        this.subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal importe() {
        return subtotal;
    }

    @Override
    public String descripcion() {
        return "Subtotal";
    }
}
