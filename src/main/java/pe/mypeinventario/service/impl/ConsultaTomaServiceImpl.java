package pe.mypeinventario.service.impl;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.mypeinventario.consulta.ConteoResumen;
import pe.mypeinventario.consulta.TomaResumen;
import pe.mypeinventario.domain.DetalleTomaInventario;
import pe.mypeinventario.domain.TomaInventario;
import pe.mypeinventario.domain.VarianteSKU;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.repository.ProductoItemRepository;
import pe.mypeinventario.repository.TiendaRepository;
import pe.mypeinventario.repository.TomaInventarioRepository;
import pe.mypeinventario.service.ConsultaTomaService;

/**
 * Implementación de {@link ConsultaTomaService}. Convierte las entidades en records dentro
 * de la transacción, porque la web no puede cargar relaciones perezosas por su cuenta.
 */
@Service
@Transactional(readOnly = true)
public class ConsultaTomaServiceImpl implements ConsultaTomaService {

    private final TomaInventarioRepository tomaRepository;
    private final TiendaRepository tiendaRepository;
    private final ProductoItemRepository productoRepository;

    public ConsultaTomaServiceImpl(TomaInventarioRepository tomaRepository, TiendaRepository tiendaRepository,
                                   ProductoItemRepository productoRepository) {
        this.tomaRepository = tomaRepository;
        this.tiendaRepository = tiendaRepository;
        this.productoRepository = productoRepository;
    }

    @Override
    public List<TomaResumen> listarTomas(Long tiendaId) {
        if (!tiendaRepository.existsById(tiendaId)) {
            throw new RecursoNoEncontradoException("Tienda", tiendaId);
        }
        return tomaRepository.findByTiendaIdOrderByFechaDesc(tiendaId).stream().map(this::resumir).toList();
    }

    @Override
    public TomaResumen obtenerToma(Long tomaId) {
        return tomaRepository.findById(tomaId)
                .map(this::resumir)
                .orElseThrow(() -> new RecursoNoEncontradoException("Toma de inventario", tomaId));
    }

    private TomaResumen resumir(TomaInventario toma) {
        List<ConteoResumen> conteos = toma.getDetalles().stream()
                .sorted(Comparator.comparing(DetalleTomaInventario::getId,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::resumir)
                .toList();
        return new TomaResumen(toma.getId(), toma.getTienda().getId(), toma.getFecha(), toma.getEstado(), conteos);
    }

    private ConteoResumen resumir(DetalleTomaInventario detalle) {
        VarianteSKU variante = detalle.getVariante();
        String producto = productoRepository.findByVarianteId(variante.getId())
                .map(p -> p.getNombre())
                .orElse("—");
        return new ConteoResumen(variante.getId(), variante.getSku(), producto, variante.getTalla(),
                variante.getColor(), detalle.getStockSistema(), detalle.getStockContado());
    }
}
