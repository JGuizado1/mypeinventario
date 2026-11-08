package pe.mypeinventario.service.impl;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.mypeinventario.consulta.ClienteResumen;
import pe.mypeinventario.domain.Cliente;
import pe.mypeinventario.domain.Tienda;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.repository.ClienteRepository;
import pe.mypeinventario.repository.TiendaRepository;
import pe.mypeinventario.service.ClienteService;
import pe.mypeinventario.service.dto.SolicitudCliente;

/** Implementación de {@link ClienteService}. */
@Service
@Transactional
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final TiendaRepository tiendaRepository;

    public ClienteServiceImpl(ClienteRepository clienteRepository, TiendaRepository tiendaRepository) {
        this.clienteRepository = clienteRepository;
        this.tiendaRepository = tiendaRepository;
    }

    @Override
    public ClienteResumen registrarCliente(SolicitudCliente solicitud) {
        Tienda tienda = tiendaRepository.findById(solicitud.tiendaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Tienda", solicitud.tiendaId()));
        if (solicitud.documento() != null && clienteRepository
                .findByTiendaIdAndDocumento(tienda.getId(), solicitud.documento()).isPresent()) {
            throw new OperacionInvalidaException(
                    "Ya existe un cliente con el documento " + solicitud.documento() + " en la tienda");
        }
        return aResumen(clienteRepository.save(new Cliente(null, solicitud.nombre(), solicitud.celular(),
                solicitud.documento(), tienda)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResumen> listarClientes(Long tiendaId) {
        if (!tiendaRepository.existsById(tiendaId)) {
            throw new RecursoNoEncontradoException("Tienda", tiendaId);
        }
        return clienteRepository.findByTiendaId(tiendaId).stream()
                .sorted(Comparator.comparing(Cliente::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(ClienteServiceImpl::aResumen)
                .toList();
    }

    private static ClienteResumen aResumen(Cliente cliente) {
        return new ClienteResumen(cliente.getId(), cliente.getNombre(), cliente.getCelular(), cliente.getDocumento());
    }
}
