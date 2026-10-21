package pe.mypeinventario.service;

import pe.mypeinventario.domain.MovimientoInventario;
import pe.mypeinventario.domain.TipoMovimiento;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.exception.StockInsuficienteException;

/** Ajustes manuales de inventario, por ejemplo tras un conteo físico o por mercadería dañada. */
public interface AjusteInventarioService {

    /**
     * Registra un ajuste manual sobre una variante. Exige un motivo que explique la corrección.
     *
     * @param varianteId identificador de la variante
     * @param tipo       {@code AJUSTE_POSITIVO} o {@code AJUSTE_NEGATIVO}
     * @param cantidad   unidades a ajustar, mayores que cero
     * @param motivo     explicación del ajuste, obligatoria
     * @return el movimiento registrado
     * @throws RecursoNoEncontradoException si la variante no existe
     * @throws OperacionInvalidaException   si el tipo no es un ajuste, falta el motivo o la cantidad es inválida
     * @throws StockInsuficienteException   si un ajuste negativo supera el stock disponible
     */
    MovimientoInventario registrarAjuste(Long varianteId, TipoMovimiento tipo, int cantidad, String motivo);
}
