package pe.mypeinventario.service.impl;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.mypeinventario.consulta.CompraResumen;
import pe.mypeinventario.consulta.LineaCompraResumen;
import pe.mypeinventario.domain.Compra;
import pe.mypeinventario.domain.DetalleCompra;
import pe.mypeinventario.domain.VarianteSKU;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.repository.CompraRepository;
import pe.mypeinventario.repository.ProductoItemRepository;
import pe.mypeinventario.repository.TiendaRepository;
import pe.mypeinventario.service.ConsultaCompraService;

/**
 * Implementación de {@link ConsultaCompraService}. Convierte las entidades en records dentro
 * de la transacción, porque la web no puede cargar relaciones perezosas por su cuenta.
 */
@Service
@Transactional(readOnly = true)
public class ConsultaCompraServiceImpl implements ConsultaCompraService {

    private final CompraRepository compraRepository;
    private final TiendaRepository tiendaRepository;
    private final ProductoItemRepository productoRepository;

    public ConsultaCompraServiceImpl(CompraRepository compraRepository, TiendaRepository tiendaRepository,
                                     ProductoItemRepository productoRepository) {
        this.compraRepository = compraRepository;
        this.tiendaRepository = tiendaRepository;
        this.productoRepository = productoRepository;
    }

    @Override
    public List<CompraResumen> listarCompras(Long tiendaId) {
        if (!tiendaRepository.existsById(tiendaId)) {
            throw new RecursoNoEncontradoException("Tienda", tiendaId);
        }
        return compraRepository.findByTiendaIdOrderByFechaDesc(tiendaId).stream().map(this::resumir).toList();
    }

    @Override
    public CompraResumen obtenerCompra(Long compraId) {
        return compraRepository.findById(compraId)
                .map(this::resumir)
                .orElseThrow(() -> new RecursoNoEncontradoException("Compra", compraId));
    }

    private CompraResumen resumir(Compra compra) {
        List<LineaCompraResumen> lineas = compra.getDetalles().stream()
                .sorted(Comparator.comparing(DetalleCompra::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::resumir)
                .toList();
        return new CompraResumen(compra.getId(), compra.getTienda().getId(), compra.getFecha(),
                compra.getProveedor().getRazonSocial(), compra.getTotal(), lineas);
    }

    private LineaCompraResumen resumir(DetalleCompra detalle) {
        VarianteSKU variante = detalle.getVariante();
        String producto = productoRepository.findByVarianteId(variante.getId())
                .map(p -> p.getNombre())
                .orElse("—");
        return new LineaCompraResumen(variante.getId(), variante.getSku(), producto, variante.getTalla(),
                variante.getColor(), detalle.getCantidad(), detalle.getCostoUnitario(), detalle.calcularSubtotal());
    }
}
