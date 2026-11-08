package pe.mypeinventario.service;

import java.util.List;
import pe.mypeinventario.consulta.TiendaResumen;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.service.dto.SolicitudTienda;

/** Tiendas de un comerciante. */
public interface TiendaService {

    /**
     * Crea una tienda y la asocia a su comerciante.
     *
     * @param solicitud datos de la tienda
     * @return la tienda creada
     * @throws RecursoNoEncontradoException si el comerciante no existe
     */
    TiendaResumen crearTienda(SolicitudTienda solicitud);

    /**
     * Lista las tiendas de un comerciante.
     *
     * @param comercianteId identificador del comerciante
     * @return sus tiendas, ordenadas por nombre
     * @throws RecursoNoEncontradoException si el comerciante no existe
     */
    List<TiendaResumen> listarTiendas(Long comercianteId);

    /**
     * Comprueba que una tienda pertenezca a un comerciante. La interfaz lo usa antes de
     * mostrar o cambiar datos de una tienda, para que nadie abra la de otro comerciante.
     *
     * @param comercianteId comerciante que consulta
     * @param tiendaId      tienda que quiere usar
     * @throws RecursoNoEncontradoException si la tienda no existe o es de otro comerciante
     */
    void verificarPropiedad(Long comercianteId, Long tiendaId);
}
