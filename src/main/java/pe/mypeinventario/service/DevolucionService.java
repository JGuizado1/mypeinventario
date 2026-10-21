package pe.mypeinventario.service;

import pe.mypeinventario.domain.MovimientoInventario;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.exception.RecursoNoEncontradoException;

/**
 * Devoluciones de clientes. Solo reponen stock: no modifican el total de la
 * venta original ni reembolsan dinero.
 */
public interface DevolucionService {

    /**
     * Registra la devolución de unidades de una línea de venta. La suma de todas las
     * devoluciones de una línea no puede superar las unidades vendidas.
     *
     * @param ventaId        venta a la que pertenece la línea
     * @param detalleVentaId línea de venta devuelta
     * @param cantidad       unidades devueltas, mayores que cero
     * @param motivo         explicación opcional; si está vacía se usa una por defecto
     * @return el movimiento de devolución registrado
     * @throws RecursoNoEncontradoException si la venta o la línea no existen
     * @throws OperacionInvalidaException   si la cantidad es inválida o supera lo que queda por devolver
     */
    MovimientoInventario registrarDevolucion(Long ventaId, Long detalleVentaId, int cantidad, String motivo);
}
