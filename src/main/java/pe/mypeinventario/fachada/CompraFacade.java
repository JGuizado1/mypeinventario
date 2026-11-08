package pe.mypeinventario.fachada;

import java.util.List;
import org.springframework.stereotype.Service;
import pe.mypeinventario.consulta.CompraResumen;
import pe.mypeinventario.consulta.ProductoResumen;
import pe.mypeinventario.consulta.ProveedorResumen;
import pe.mypeinventario.domain.Compra;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.service.CompraService;
import pe.mypeinventario.service.ConsultaCatalogoService;
import pe.mypeinventario.service.ConsultaCompraService;
import pe.mypeinventario.service.ProveedorService;
import pe.mypeinventario.service.TiendaService;
import pe.mypeinventario.service.dto.SolicitudCompra;
import pe.mypeinventario.service.dto.SolicitudProveedor;

/**
 * Patrón Facade del lado de las compras: reponer mercadería y llevar los proveedores.
 * Comprueba que la tienda (o la compra) sea del comerciante y devuelve solo resúmenes.
 * Los proveedores son compartidos por todas las tiendas y no tienen dueño.
 */
@Service
public class CompraFacade {

    private final CompraService compraService;
    private final ConsultaCompraService consultaCompra;
    private final ConsultaCatalogoService catalogo;
    private final ProveedorService proveedorService;
    private final TiendaService tiendaService;

    public CompraFacade(CompraService compraService, ConsultaCompraService consultaCompra,
                        ConsultaCatalogoService catalogo, ProveedorService proveedorService,
                        TiendaService tiendaService) {
        this.compraService = compraService;
        this.consultaCompra = consultaCompra;
        this.catalogo = catalogo;
        this.proveedorService = proveedorService;
        this.tiendaService = tiendaService;
    }

    /** @return las compras de una tienda del comerciante, de la más reciente a la más antigua */
    public List<CompraResumen> compras(Long comercianteId, Long tiendaId) {
        tiendaService.verificarPropiedad(comercianteId, tiendaId);
        return consultaCompra.listarCompras(tiendaId);
    }

    /**
     * Devuelve una compra, siempre que sea de una tienda del comerciante.
     *
     * @throws RecursoNoEncontradoException si no existe o es de otro comerciante
     */
    public CompraResumen compra(Long comercianteId, Long compraId) {
        CompraResumen compra = consultaCompra.obtenerCompra(compraId);
        try {
            tiendaService.verificarPropiedad(comercianteId, compra.tiendaId());
        } catch (RecursoNoEncontradoException e) {
            throw new RecursoNoEncontradoException("Compra", compraId);
        }
        return compra;
    }

    /** @return todos los productos de la tienda, también los inactivos (se puede reponer antes de reactivar) */
    public List<ProductoResumen> productosParaComprar(Long comercianteId, Long tiendaId) {
        tiendaService.verificarPropiedad(comercianteId, tiendaId);
        return catalogo.listarProductos(tiendaId, false);
    }

    /** @see CompraService#registrarCompra(SolicitudCompra) */
    public CompraResumen registrarCompra(Long comercianteId, SolicitudCompra solicitud) {
        tiendaService.verificarPropiedad(comercianteId, solicitud.tiendaId());
        Compra registrada = compraService.registrarCompra(solicitud);
        return consultaCompra.obtenerCompra(registrada.getId());
    }

    /** @return todos los proveedores, ordenados por razón social */
    public List<ProveedorResumen> proveedores() {
        return proveedorService.listarProveedores();
    }

    /** @see ProveedorService#registrarProveedor(SolicitudProveedor) */
    public ProveedorResumen registrarProveedor(SolicitudProveedor solicitud) {
        return proveedorService.registrarProveedor(solicitud);
    }
}
