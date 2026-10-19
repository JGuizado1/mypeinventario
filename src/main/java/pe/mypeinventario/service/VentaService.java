package pe.mypeinventario.service;

import pe.mypeinventario.domain.VentaPunto;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.exception.StockInsuficienteException;
import pe.mypeinventario.service.dto.SolicitudVenta;

/** Operaciones de venta en el punto de venta. */
public interface VentaService {

    /**
     * Registra una venta: valida las líneas, congela los precios, aplica los descuentos,
     * desglosa el IGV, descuenta el stock y genera las alertas que correspondan. Todo
     * ocurre en una misma transacción: si algo falla, no queda nada guardado.
     *
     * @param solicitud datos de la venta
     * @return la venta registrada, con sus líneas, totales e identificadores
     * @throws RecursoNoEncontradoException si no existe la tienda, el cliente o alguna variante
     * @throws OperacionInvalidaException   si una variante o el cliente no pertenecen a la tienda,
     *                                      si un producto está inactivo o si algún dato es inválido
     * @throws StockInsuficienteException   si alguna línea supera el stock disponible
     */
    VentaPunto registrarVenta(SolicitudVenta solicitud);
}
