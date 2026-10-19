package pe.mypeinventario.service;

import java.util.List;
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
}
