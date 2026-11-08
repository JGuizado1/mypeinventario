package pe.mypeinventario.fachada;

import org.springframework.stereotype.Service;
import pe.mypeinventario.consulta.VentaResumen;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.service.ConsultaVentaService;
import pe.mypeinventario.service.DevolucionService;
import pe.mypeinventario.service.TiendaService;

/**
 * Patrón Facade de las devoluciones de clientes. Comprueba que la venta sea de una tienda
 * del comerciante antes de reponer el stock; una venta ajena se responde como "no existe".
 * Una devolución solo repone stock: no reembolsa dinero ni cambia el total de la venta.
 */
@Service
public class DevolucionFacade {

    private final DevolucionService devolucionService;
    private final ConsultaVentaService consultaVenta;
    private final TiendaService tiendaService;

    public DevolucionFacade(DevolucionService devolucionService, ConsultaVentaService consultaVenta,
                            TiendaService tiendaService) {
        this.devolucionService = devolucionService;
        this.consultaVenta = consultaVenta;
        this.tiendaService = tiendaService;
    }

    /**
     * Registra la devolución de unidades de una línea de una venta del comerciante.
     *
     * @throws RecursoNoEncontradoException si la venta es ajena o no existe
     * @see DevolucionService#registrarDevolucion(Long, Long, int, String)
     */
    public void devolver(Long comercianteId, Long ventaId, Long detalleId, int cantidad, String motivo) {
        VentaResumen venta = consultaVenta.obtenerVenta(ventaId);
        try {
            tiendaService.verificarPropiedad(comercianteId, venta.tiendaId());
        } catch (RecursoNoEncontradoException e) {
            throw new RecursoNoEncontradoException("Venta", ventaId);
        }
        devolucionService.registrarDevolucion(ventaId, detalleId, cantidad, motivo);
    }
}
