package pe.mypeinventario.service.impl;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.mypeinventario.consulta.LineaVentaResumen;
import pe.mypeinventario.consulta.VentaResumen;
import pe.mypeinventario.domain.DetalleVenta;
import pe.mypeinventario.domain.TipoMovimiento;
import pe.mypeinventario.domain.VarianteSKU;
import pe.mypeinventario.domain.VentaPunto;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.repository.MovimientoInventarioRepository;
import pe.mypeinventario.repository.ProductoItemRepository;
import pe.mypeinventario.repository.TiendaRepository;
import pe.mypeinventario.repository.VentaPuntoRepository;
import pe.mypeinventario.service.ConsultaVentaService;

/**
 * Implementación de {@link ConsultaVentaService}. Convierte las entidades en records dentro
 * de la transacción, porque la web no puede cargar relaciones perezosas por su cuenta.
 */
@Service
@Transactional(readOnly = true)
public class ConsultaVentaServiceImpl implements ConsultaVentaService {

    private final VentaPuntoRepository ventaRepository;
    private final TiendaRepository tiendaRepository;
    private final ProductoItemRepository productoRepository;
    private final MovimientoInventarioRepository movimientoRepository;

    public ConsultaVentaServiceImpl(VentaPuntoRepository ventaRepository, TiendaRepository tiendaRepository,
                                    ProductoItemRepository productoRepository,
                                    MovimientoInventarioRepository movimientoRepository) {
        this.ventaRepository = ventaRepository;
        this.tiendaRepository = tiendaRepository;
        this.productoRepository = productoRepository;
        this.movimientoRepository = movimientoRepository;
    }

    @Override
    public List<VentaResumen> listarVentas(Long tiendaId) {
        if (!tiendaRepository.existsById(tiendaId)) {
            throw new RecursoNoEncontradoException("Tienda", tiendaId);
        }
        return ventaRepository.findByTiendaIdOrderByFechaDesc(tiendaId).stream().map(this::resumir).toList();
    }

    @Override
    public VentaResumen obtenerVenta(Long ventaId) {
        return ventaRepository.findById(ventaId)
                .map(this::resumir)
                .orElseThrow(() -> new RecursoNoEncontradoException("Venta", ventaId));
    }

    private VentaResumen resumir(VentaPunto venta) {
        List<LineaVentaResumen> lineas = venta.getDetalles().stream()
                .sorted(Comparator.comparing(DetalleVenta::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::resumir)
                .toList();
        return new VentaResumen(venta.getId(), venta.getTienda().getId(), venta.getFecha(),
                venta.getCliente() == null ? null : venta.getCliente().getNombre(), venta.getMedioPago(),
                venta.getSubtotal(), venta.getDescuento(), venta.getTotal(), venta.getIgv(), lineas);
    }

    private LineaVentaResumen resumir(DetalleVenta detalle) {
        VarianteSKU variante = detalle.getVariante();
        String producto = productoRepository.findByVarianteId(variante.getId())
                .map(p -> p.getNombre())
                .orElse("—");
        int devueltas = (int) movimientoRepository.sumarCantidadPorDetalleVenta(
                detalle.getId(), TipoMovimiento.DEVOLUCION_CLIENTE);
        return new LineaVentaResumen(detalle.getId(), variante.getId(), variante.getSku(), producto,
                variante.getTalla(), variante.getColor(), detalle.getCantidad(), detalle.getPrecioUnitario(),
                detalle.calcularSubtotal(), devueltas);
    }
}
