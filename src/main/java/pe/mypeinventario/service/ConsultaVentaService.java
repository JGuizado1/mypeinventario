package pe.mypeinventario.service;

import java.util.List;
import pe.mypeinventario.consulta.VentaResumen;
import pe.mypeinventario.exception.RecursoNoEncontradoException;

/** Consultas de solo lectura sobre las ventas. No modifica ningún dato. */
public interface ConsultaVentaService {

    /**
     * Lista las ventas de una tienda.
     *
     * @param tiendaId identificador de la tienda
     * @return ventas, de la más reciente a la más antigua
     * @throws RecursoNoEncontradoException si la tienda no existe
     */
    List<VentaResumen> listarVentas(Long tiendaId);

    /**
     * Devuelve una venta con sus líneas.
     *
     * @param ventaId identificador de la venta
     * @return la venta
     * @throws RecursoNoEncontradoException si la venta no existe
     */
    VentaResumen obtenerVenta(Long ventaId);
}
