package pe.mypeinventario.service;

import pe.mypeinventario.domain.AlertaReabastecimiento;
import pe.mypeinventario.domain.MovimientoInventario;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.exception.StockInsuficienteException;

/**
 * Operaciones que modifican el inventario: registrar movimientos y gestionar
 * las alertas de reabastecimiento que se derivan de ellos.
 */
public interface InventarioService {

    /**
     * Registra un movimiento en una variante y actualiza sus alertas, todo en una
     * misma transacción. Si el stock queda en el mínimo o por debajo y no hay una
     * alerta abierta, genera una; si el stock supera el mínimo, cierra las abiertas.
     *
     * @param varianteId identificador de la variante
     * @param movimiento movimiento a registrar
     * @return el movimiento ya persistido, con su identificador
     * @throws RecursoNoEncontradoException si la variante no existe
     * @throws StockInsuficienteException   si es una salida mayor al stock disponible
     * @throws OperacionInvalidaException   si el movimiento es nulo
     */
    MovimientoInventario registrarMovimiento(Long varianteId, MovimientoInventario movimiento);

    /**
     * Marca una alerta pendiente como vista.
     *
     * @param alertaId identificador de la alerta
     * @return la alerta actualizada
     * @throws RecursoNoEncontradoException si la alerta no existe
     * @throws OperacionInvalidaException   si la alerta no estaba pendiente
     */
    AlertaReabastecimiento marcarAlertaVista(Long alertaId);

    /**
     * Marca una alerta como atendida (ya se reabasteció).
     *
     * @param alertaId identificador de la alerta
     * @return la alerta actualizada
     * @throws RecursoNoEncontradoException si la alerta no existe
     * @throws OperacionInvalidaException   si la alerta ya estaba atendida
     */
    AlertaReabastecimiento marcarAlertaAtendida(Long alertaId);
}
