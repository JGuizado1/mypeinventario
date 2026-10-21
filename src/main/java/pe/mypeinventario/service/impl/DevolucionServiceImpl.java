package pe.mypeinventario.service.impl;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.mypeinventario.domain.DetalleVenta;
import pe.mypeinventario.domain.MovimientoInventario;
import pe.mypeinventario.domain.TipoMovimiento;
import pe.mypeinventario.domain.VentaPunto;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.factory.CreadorMovimiento;
import pe.mypeinventario.factory.OrigenMovimiento;
import pe.mypeinventario.repository.MovimientoInventarioRepository;
import pe.mypeinventario.repository.VentaPuntoRepository;
import pe.mypeinventario.service.DevolucionService;
import pe.mypeinventario.service.InventarioService;

/** Implementación de {@link DevolucionService}. */
@Service
public class DevolucionServiceImpl implements DevolucionService {

    private final VentaPuntoRepository ventaRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final InventarioService inventarioService;
    private final CreadorMovimiento creadorDevolucion;

    public DevolucionServiceImpl(VentaPuntoRepository ventaRepository,
                                 MovimientoInventarioRepository movimientoRepository,
                                 InventarioService inventarioService,
                                 @Qualifier("creadorDevolucion") CreadorMovimiento creadorDevolucion) {
        this.ventaRepository = ventaRepository;
        this.movimientoRepository = movimientoRepository;
        this.inventarioService = inventarioService;
        this.creadorDevolucion = creadorDevolucion;
    }

    @Override
    @Transactional
    public MovimientoInventario registrarDevolucion(Long ventaId, Long detalleVentaId, int cantidad, String motivo) {
        VentaPunto venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Venta", ventaId));
        // La línea se busca dentro de su venta: es una composición y se accede por su raíz.
        DetalleVenta detalle = venta.getDetalles().stream()
                .filter(linea -> detalleVentaId != null && detalleVentaId.equals(linea.getId()))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Línea de venta", detalleVentaId));

        long yaDevuelto = movimientoRepository.sumarCantidadPorDetalleVenta(
                detalle.getId(), TipoMovimiento.DEVOLUCION_CLIENTE);
        long porDevolver = detalle.getCantidad() - yaDevuelto;
        if (cantidad > porDevolver) {
            throw new OperacionInvalidaException("Solo quedan " + porDevolver
                    + " unidades por devolver de esta línea (vendidas " + detalle.getCantidad()
                    + ", ya devueltas " + yaDevuelto + ")");
        }

        MovimientoInventario movimiento = creadorDevolucion.crear(OrigenMovimiento.deVenta(detalle), cantidad, motivo);
        return inventarioService.registrarMovimiento(detalle.getVariante().getId(), movimiento);
    }
}
