package pe.mypeinventario.service;

import pe.mypeinventario.domain.Compra;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.service.dto.SolicitudCompra;

/** Operaciones de compra de mercadería a proveedores mayoristas. */
public interface CompraService {

    /**
     * Registra una compra: valida las líneas, calcula el total, aumenta el stock y cierra
     * las alertas de reabastecimiento que dejen de aplicar. Todo ocurre en una misma
     * transacción: si algo falla, no queda nada guardado.
     *
     * @param solicitud datos de la compra
     * @return la compra registrada, con sus líneas, total e identificadores
     * @throws RecursoNoEncontradoException si no existe la tienda, el proveedor o alguna variante
     * @throws OperacionInvalidaException   si alguna variante no pertenece a la tienda
     */
    Compra registrarCompra(SolicitudCompra solicitud);
}
