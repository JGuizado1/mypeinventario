package pe.mypeinventario.service;

import java.util.Collection;
import java.util.List;
import pe.mypeinventario.consulta.AlertaResumen;
import pe.mypeinventario.domain.AlertaReabastecimiento;
import pe.mypeinventario.domain.EstadoAlerta;
import pe.mypeinventario.domain.MovimientoInventario;
import pe.mypeinventario.exception.RecursoNoEncontradoException;

/** Consultas de solo lectura sobre el inventario. No modifica ningún dato. */
public interface ConsultaInventarioService {

    /**
     * Calcula el stock actual de una variante sumando sus movimientos en la base de datos.
     *
     * @param varianteId identificador de la variante
     * @return unidades disponibles
     * @throws RecursoNoEncontradoException si la variante no existe
     */
    int consultarStock(Long varianteId);

    /**
     * Devuelve el historial de movimientos de una variante.
     *
     * @param varianteId identificador de la variante
     * @return movimientos, del más reciente al más antiguo
     * @throws RecursoNoEncontradoException si la variante no existe
     */
    List<MovimientoInventario> obtenerHistorial(Long varianteId);

    /**
     * Lista las alertas de una tienda en un estado dado.
     *
     * @param tiendaId identificador de la tienda
     * @param estado   estado buscado, por ejemplo pendiente
     * @return alertas encontradas, de la más reciente a la más antigua
     * @throws RecursoNoEncontradoException si la tienda no existe
     */
    List<AlertaReabastecimiento> listarAlertas(Long tiendaId, EstadoAlerta estado);

    /**
     * Lista las alertas de una tienda en alguno de los estados dados, con el producto y la
     * variante a los que se refieren.
     *
     * @param tiendaId identificador de la tienda
     * @param estados  estados buscados, por ejemplo pendiente y vista
     * @return resúmenes de la más reciente a la más antigua
     * @throws RecursoNoEncontradoException si la tienda no existe
     */
    List<AlertaResumen> listarResumenAlertas(Long tiendaId, Collection<EstadoAlerta> estados);

    /**
     * Devuelve una alerta con el producto y la variante a los que se refiere.
     *
     * @param alertaId identificador de la alerta
     * @return el resumen de la alerta
     * @throws RecursoNoEncontradoException si la alerta no existe
     */
    AlertaResumen obtenerResumenAlerta(Long alertaId);
}
