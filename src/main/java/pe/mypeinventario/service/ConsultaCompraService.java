package pe.mypeinventario.service;

import java.util.List;
import pe.mypeinventario.consulta.CompraResumen;
import pe.mypeinventario.exception.RecursoNoEncontradoException;

/** Consultas de solo lectura sobre las compras. No modifica ningún dato. */
public interface ConsultaCompraService {

    /**
     * Lista las compras de una tienda.
     *
     * @param tiendaId identificador de la tienda
     * @return compras, de la más reciente a la más antigua
     * @throws RecursoNoEncontradoException si la tienda no existe
     */
    List<CompraResumen> listarCompras(Long tiendaId);

    /**
     * Devuelve una compra con sus líneas.
     *
     * @param compraId identificador de la compra
     * @return la compra
     * @throws RecursoNoEncontradoException si la compra no existe
     */
    CompraResumen obtenerCompra(Long compraId);
}
