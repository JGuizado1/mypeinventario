package pe.mypeinventario.decorator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import pe.mypeinventario.exception.OperacionInvalidaException;

/**
 * Desglose de un total que ya incluye IGV: cuánto es base imponible y cuánto
 * es impuesto. No modifica el total.
 *
 * @param baseImponible total sin IGV
 * @param igv           IGV contenido en el total
 * @param total         total que paga el cliente
 */
public record DesgloseIgv(BigDecimal baseImponible, BigDecimal igv, BigDecimal total) {

    /**
     * Calcula el desglose de un total con IGV incluido.
     *
     * @param totalConIgv total que paga el cliente, no negativo
     * @param tasaIgv     tasa como fracción entre 0 y 1 (por ejemplo 0.18)
     * @return el desglose, con dos decimales
     * @throws OperacionInvalidaException si algún dato es nulo o está fuera de rango
     */
    public static DesgloseIgv desde(BigDecimal totalConIgv, BigDecimal tasaIgv) {
        if (totalConIgv == null || totalConIgv.signum() < 0) {
            throw new OperacionInvalidaException("El total no puede ser nulo ni negativo");
        }
        if (tasaIgv == null || tasaIgv.signum() < 0 || tasaIgv.compareTo(BigDecimal.ONE) > 0) {
            throw new OperacionInvalidaException("La tasa de IGV debe estar entre 0 y 1");
        }
        BigDecimal total = totalConIgv.setScale(2, RoundingMode.HALF_UP);
        BigDecimal igv = total.multiply(tasaIgv).divide(BigDecimal.ONE.add(tasaIgv), 2, RoundingMode.HALF_UP);
        return new DesgloseIgv(total.subtract(igv), igv, total);
    }
}
