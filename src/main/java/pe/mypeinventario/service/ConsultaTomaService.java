package pe.mypeinventario.service;

import java.util.List;
import pe.mypeinventario.consulta.TomaResumen;
import pe.mypeinventario.exception.RecursoNoEncontradoException;

/** Consultas de solo lectura sobre las tomas de inventario. No modifica ningún dato. */
public interface ConsultaTomaService {

    /**
     * Lista las tomas de una tienda.
     *
     * @param tiendaId identificador de la tienda
     * @return tomas, de la más reciente a la más antigua
     * @throws RecursoNoEncontradoException si la tienda no existe
     */
    List<TomaResumen> listarTomas(Long tiendaId);

    /**
     * Devuelve una toma con sus conteos.
     *
     * @param tomaId identificador de la toma
     * @return la toma
     * @throws RecursoNoEncontradoException si la toma no existe
     */
    TomaResumen obtenerToma(Long tomaId);
}
