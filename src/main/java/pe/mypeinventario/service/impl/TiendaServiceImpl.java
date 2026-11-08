package pe.mypeinventario.service.impl;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.mypeinventario.consulta.TiendaResumen;
import pe.mypeinventario.domain.Comerciante;
import pe.mypeinventario.domain.Tienda;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.repository.ComercianteRepository;
import pe.mypeinventario.repository.TiendaRepository;
import pe.mypeinventario.service.TiendaService;
import pe.mypeinventario.service.dto.SolicitudTienda;

/** Implementación de {@link TiendaService}. */
@Service
@Transactional
public class TiendaServiceImpl implements TiendaService {

    private final TiendaRepository tiendaRepository;
    private final ComercianteRepository comercianteRepository;

    public TiendaServiceImpl(TiendaRepository tiendaRepository, ComercianteRepository comercianteRepository) {
        this.tiendaRepository = tiendaRepository;
        this.comercianteRepository = comercianteRepository;
    }

    @Override
    public TiendaResumen crearTienda(SolicitudTienda solicitud) {
        Comerciante comerciante = comercianteRepository.findById(solicitud.comercianteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Comerciante", solicitud.comercianteId()));
        Tienda tienda = tiendaRepository.save(
                new Tienda(null, solicitud.nombre(), solicitud.direccion(), solicitud.galeria()));
        comerciante.agregarTienda(tienda);
        return aResumen(tienda);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TiendaResumen> listarTiendas(Long comercianteId) {
        if (!comercianteRepository.existsById(comercianteId)) {
            throw new RecursoNoEncontradoException("Comerciante", comercianteId);
        }
        return tiendaRepository.listarPorComerciante(comercianteId).stream()
                .sorted(Comparator.comparing(Tienda::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(TiendaServiceImpl::aResumen)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public void verificarPropiedad(Long comercianteId, Long tiendaId) {
        boolean esSuya = tiendaRepository.listarPorComerciante(comercianteId).stream()
                .anyMatch(tienda -> tienda.getId().equals(tiendaId));
        if (!esSuya) {
            // Mismo error si no existe o es de otro: no se revela qué tiendas hay.
            throw new RecursoNoEncontradoException("Tienda", tiendaId);
        }
    }

    private static TiendaResumen aResumen(Tienda tienda) {
        return new TiendaResumen(tienda.getId(), tienda.getNombre(), tienda.getDireccion(), tienda.getGaleria());
    }
}
