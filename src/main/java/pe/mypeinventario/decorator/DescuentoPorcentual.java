package pe.mypeinventario.decorator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import pe.mypeinventario.exception.OperacionInvalidaException;

/** Decorador concreto: descuenta un porcentaje del importe recibido. */
public class DescuentoPorcentual extends DecoradorImporte {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final BigDecimal porcentaje;

    /**
     * @param envuelto   importe al que se aplica el descuento
     * @param porcentaje porcentaje a descontar, mayor que 0 y hasta 100 (por ejemplo 10 para 10 %)
     * @throws OperacionInvalidaException si el porcentaje es nulo o está fuera de rango
     */
    public DescuentoPorcentual(CalculoImporte envuelto, BigDecimal porcentaje) {
        super(envuelto);
        if (porcentaje == null || porcentaje.signum() <= 0 || porcentaje.compareTo(CIEN) > 0) {
            throw new OperacionInvalidaException("El porcentaje de descuento debe ser mayor que 0 y hasta 100");
        }
        this.porcentaje = porcentaje;
    }

    @Override
    public BigDecimal importe() {
        BigDecimal base = envuelto.importe();
        BigDecimal descuento = base.multiply(porcentaje).divide(CIEN, ESCALA, RoundingMode.HALF_UP);
        return base.subtract(descuento);
    }

    @Override
    public String descripcion() {
        return envuelto.descripcion() + " + descuento " + porcentaje.stripTrailingZeros().toPlainString() + " %";
    }
}
