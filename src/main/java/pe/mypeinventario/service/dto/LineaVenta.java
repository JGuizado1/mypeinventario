package pe.mypeinventario.service.dto;

import pe.mypeinventario.exception.OperacionInvalidaException;

/**
 * Línea de una venta solicitada: qué variante y cuántas unidades.
 *
 * @param varianteId variante vendida
 * @param cantidad   unidades, mayores que cero
 */
public record LineaVenta(Long varianteId, int cantidad) {

    public LineaVenta {
        if (varianteId == null) {
            throw new OperacionInvalidaException("La variante es obligatoria");
        }
        if (cantidad <= 0) {
            throw new OperacionInvalidaException("La cantidad debe ser mayor que cero");
        }
    }
}
