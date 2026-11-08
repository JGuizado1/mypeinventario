package pe.mypeinventario.fachada;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import pe.mypeinventario.consulta.ClienteResumen;
import pe.mypeinventario.consulta.LineaVentaResumen;
import pe.mypeinventario.consulta.ProductoResumen;
import pe.mypeinventario.consulta.ResultadoVenta;
import pe.mypeinventario.consulta.VentaResumen;
import pe.mypeinventario.domain.VentaPunto;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.service.ClienteService;
import pe.mypeinventario.service.ConsultaCatalogoService;
import pe.mypeinventario.service.ConsultaVentaService;
import pe.mypeinventario.service.TiendaService;
import pe.mypeinventario.service.VentaService;
import pe.mypeinventario.service.dto.SolicitudCliente;
import pe.mypeinventario.service.dto.SolicitudVenta;

/**
 * Patrón Facade del lado de las ventas: vender, ver lo vendido y atender a los clientes.
 * Comprueba que la tienda (o la venta) sea del comerciante y devuelve solo resúmenes, no
 * entidades. Una venta ajena se responde como "no existe", igual que en el catálogo.
 */
@Service
public class VentaFacade {

    private final VentaService ventaService;
    private final ConsultaVentaService consultaVenta;
    private final ConsultaCatalogoService catalogo;
    private final ClienteService clienteService;
    private final TiendaService tiendaService;

    public VentaFacade(VentaService ventaService, ConsultaVentaService consultaVenta,
                       ConsultaCatalogoService catalogo, ClienteService clienteService,
                       TiendaService tiendaService) {
        this.ventaService = ventaService;
        this.consultaVenta = consultaVenta;
        this.catalogo = catalogo;
        this.clienteService = clienteService;
        this.tiendaService = tiendaService;
    }

    /** @return las ventas de una tienda del comerciante, de la más reciente a la más antigua */
    public List<VentaResumen> ventas(Long comercianteId, Long tiendaId) {
        tiendaService.verificarPropiedad(comercianteId, tiendaId);
        return consultaVenta.listarVentas(tiendaId);
    }

    /**
     * Devuelve una venta, siempre que sea de una tienda del comerciante.
     *
     * @throws RecursoNoEncontradoException si no existe o es de otro comerciante
     */
    public VentaResumen venta(Long comercianteId, Long ventaId) {
        VentaResumen venta = consultaVenta.obtenerVenta(ventaId);
        try {
            tiendaService.verificarPropiedad(comercianteId, venta.tiendaId());
        } catch (RecursoNoEncontradoException e) {
            throw new RecursoNoEncontradoException("Venta", ventaId);
        }
        return venta;
    }

    /** @return los productos activos de la tienda, con sus variantes y su stock, para elegir qué vender */
    public List<ProductoResumen> productosParaVender(Long comercianteId, Long tiendaId) {
        tiendaService.verificarPropiedad(comercianteId, tiendaId);
        return catalogo.listarProductos(tiendaId, true);
    }

    /**
     * Registra una venta y avisa qué variantes quedaron con poco stock.
     *
     * @see VentaService#registrarVenta(SolicitudVenta)
     */
    public ResultadoVenta registrarVenta(Long comercianteId, SolicitudVenta solicitud) {
        tiendaService.verificarPropiedad(comercianteId, solicitud.tiendaId());
        VentaPunto registrada = ventaService.registrarVenta(solicitud);
        VentaResumen venta = consultaVenta.obtenerVenta(registrada.getId());
        return new ResultadoVenta(venta, avisosDeStock(solicitud.tiendaId(), venta));
    }

    /** @return los clientes de una tienda del comerciante, ordenados por nombre */
    public List<ClienteResumen> clientes(Long comercianteId, Long tiendaId) {
        tiendaService.verificarPropiedad(comercianteId, tiendaId);
        return clienteService.listarClientes(tiendaId);
    }

    /** @see ClienteService#registrarCliente(SolicitudCliente) */
    public ClienteResumen registrarCliente(Long comercianteId, SolicitudCliente solicitud) {
        tiendaService.verificarPropiedad(comercianteId, solicitud.tiendaId());
        return clienteService.registrarCliente(solicitud);
    }

    /** Busca, entre lo que se acaba de vender, las variantes que alcanzaron su stock mínimo. */
    private List<String> avisosDeStock(Long tiendaId, VentaResumen venta) {
        Set<Long> vendidas = venta.lineas().stream()
                .map(LineaVentaResumen::varianteId)
                .collect(Collectors.toSet());
        return catalogo.listarProductos(tiendaId, true).stream()
                .flatMap(producto -> producto.variantes().stream()
                        .filter(variante -> vendidas.contains(variante.id()) && variante.stockBajo())
                        .map(variante -> producto.nombre() + " (" + variante.sku() + "): quedan "
                                + variante.stock() + ", el mínimo es " + variante.stockMinimo()))
                .toList();
    }
}
