package pe.mypeinventario.fachada;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import pe.mypeinventario.consulta.MovimientoResumen;
import pe.mypeinventario.consulta.ProductoResumen;
import pe.mypeinventario.domain.MovimientoInventario;
import pe.mypeinventario.domain.TipoMovimiento;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.service.AjusteInventarioService;
import pe.mypeinventario.service.ConsultaCatalogoService;
import pe.mypeinventario.service.ConsultaInventarioService;
import pe.mypeinventario.service.TiendaService;

/**
 * Patrón Facade de los ajustes de stock: corregir el stock de una variante y ver su historial.
 * Antes de tocar una variante comprueba que pertenezca a una tienda del comerciante; una
 * variante ajena se responde como "no existe".
 */
@Service
public class AjusteFacade {

    private final AjusteInventarioService ajusteService;
    private final ConsultaInventarioService inventario;
    private final ConsultaCatalogoService catalogo;
    private final TiendaService tiendaService;

    public AjusteFacade(AjusteInventarioService ajusteService, ConsultaInventarioService inventario,
                        ConsultaCatalogoService catalogo, TiendaService tiendaService) {
        this.ajusteService = ajusteService;
        this.inventario = inventario;
        this.catalogo = catalogo;
        this.tiendaService = tiendaService;
    }

    /** @return todos los productos de la tienda, también los inactivos, con su stock actual */
    public List<ProductoResumen> productos(Long comercianteId, Long tiendaId) {
        tiendaService.verificarPropiedad(comercianteId, tiendaId);
        return catalogo.listarProductos(tiendaId, false);
    }

    /**
     * Devuelve el historial de una variante de la tienda, del movimiento más reciente al más antiguo.
     *
     * @throws RecursoNoEncontradoException si la tienda o la variante no son del comerciante
     */
    public List<MovimientoResumen> historial(Long comercianteId, Long tiendaId, Long varianteId) {
        exigirVariante(comercianteId, tiendaId, varianteId);
        return inventario.obtenerHistorial(varianteId).stream()
                .map(movimiento -> new MovimientoResumen(movimiento.getId(), movimiento.getTipo(),
                        movimiento.getCantidad(), movimiento.calcularEfecto(), movimiento.getFecha(),
                        movimiento.getMotivo()))
                .sorted(Comparator.comparing(MovimientoResumen::fecha).reversed()
                        .thenComparing(Comparator.comparing(MovimientoResumen::id).reversed()))
                .toList();
    }

    /**
     * Registra un ajuste manual sobre una variante de la tienda.
     *
     * @see AjusteInventarioService#registrarAjuste(Long, TipoMovimiento, int, String)
     * @throws RecursoNoEncontradoException si la tienda o la variante no son del comerciante
     */
    public MovimientoInventario ajustar(Long comercianteId, Long tiendaId, Long varianteId, TipoMovimiento tipo,
                                        int cantidad, String motivo) {
        exigirVariante(comercianteId, tiendaId, varianteId);
        return ajusteService.registrarAjuste(varianteId, tipo, cantidad, motivo);
    }

    private void exigirVariante(Long comercianteId, Long tiendaId, Long varianteId) {
        tiendaService.verificarPropiedad(comercianteId, tiendaId);
        boolean esDeLaTienda = catalogo.listarProductos(tiendaId, false).stream()
                .flatMap(producto -> producto.variantes().stream())
                .anyMatch(variante -> variante.id().equals(varianteId));
        if (!esDeLaTienda) {
            throw new RecursoNoEncontradoException("Variante", varianteId);
        }
    }
}
