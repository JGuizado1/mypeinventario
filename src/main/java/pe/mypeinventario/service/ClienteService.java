package pe.mypeinventario.service;

import java.util.List;
import pe.mypeinventario.consulta.ClienteResumen;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.service.dto.SolicitudCliente;

/** Clientes de una tienda. */
public interface ClienteService {

    /**
     * Registra un cliente. El documento no puede repetirse dentro de la tienda.
     *
     * @param solicitud datos del cliente
     * @return el cliente creado
     * @throws OperacionInvalidaException   si el documento ya existe en la tienda
     * @throws RecursoNoEncontradoException si la tienda no existe
     */
    ClienteResumen registrarCliente(SolicitudCliente solicitud);

    /**
     * Lista los clientes de una tienda.
     *
     * @param tiendaId identificador de la tienda
     * @return clientes ordenados por nombre
     * @throws RecursoNoEncontradoException si la tienda no existe
     */
    List<ClienteResumen> listarClientes(Long tiendaId);
}
