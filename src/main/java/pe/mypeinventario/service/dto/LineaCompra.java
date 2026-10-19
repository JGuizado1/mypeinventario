package pe.mypeinventario.service.dto;

import java.math.BigDecimal;
import pe.mypeinventario.exception.OperacionInvalidaException;

/**
 * Línea de una compra solicitada.
 *
 * @param varianteId    variante comprada
 * @param cantidad      unidades, mayores que cero
 * @param costoUnitario costo por unidad, no negativo
 */
public record LineaCompra(Long varianteId, int cantidad, BigDecimal costoUnitario) {

    public LineaCompra {
        if (varianteId == null) {
            throw new OperacionInvalidaException("La variante es obligatoria");
        }
        if (cantidad <= 0) {
            throw new OperacionInvalidaException("La cantidad debe ser mayor que cero");
        }
        if (costoUnitario == null || costoUnitario.signum() < 0) {
            throw new OperacionInvalidaException("El costo unitario no puede ser nulo ni negativo");
        }
    }
}