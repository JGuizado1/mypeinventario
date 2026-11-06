package pe.mypeinventario.service;

import pe.mypeinventario.domain.TomaInventario;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.exception.StockInsuficienteException;

/** Toma de inventario físico: contar, comparar con el sistema y ajustar las diferencias. */
public interface InventarioFisicoService {

    /**
     * Abre una toma de inventario en una tienda.
     *
     * @param tiendaId tienda donde se cuenta
     * @return la toma en curso
     * @throws RecursoNoEncontradoException si la tienda no existe
     * @throws OperacionInvalidaException   si la tienda ya tiene una toma en curso
     */
    TomaInventario iniciarToma(Long tiendaId);

    /**
     * Registra el conteo físico de una variante, guardando el stock que el sistema tenía en ese momento.
     *
     * @param tomaId      toma en curso
     * @param varianteId  variante contada
     * @param stockContado unidades contadas, no negativas
     * @return la toma actualizada
     * @throws RecursoNoEncontradoException si la toma o la variante no existen
     * @throws OperacionInvalidaException   si la toma está cerrada, la variante ya fue contada,
     *                                      no pertenece a la tienda o el stock es negativo
     */
    TomaInventario registrarConteo(Long tomaId, Long varianteId, int stockContado);

    /**
     * Cierra la toma y aplica cada diferencia como un ajuste de inventario, todo en una misma
     * transacción: si algún ajuste falla, no se aplica ninguno y la toma sigue en curso.
     *
     * @param tomaId toma a cerrar
     * @return la toma cerrada
     * @throws RecursoNoEncontradoException si la toma no existe
     * @throws OperacionInvalidaException   si ya estaba cerrada o no tiene conteos
     * @throws StockInsuficienteException   si una diferencia negativa supera el stock actual
     */
    TomaInventario cerrarToma(Long tomaId);

    /**
     * Consulta una toma con sus conteos.
     *
     * @param tomaId toma buscada
     * @return la toma
     * @throws RecursoNoEncontradoException si no existe
     */
    TomaInventario obtenerToma(Long tomaId);
}
