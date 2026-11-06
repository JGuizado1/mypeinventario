package pe.mypeinventario.fachada;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import pe.mypeinventario.consulta.CategoriaResumen;
import pe.mypeinventario.consulta.MarcaResumen;
import pe.mypeinventario.consulta.ProductoResumen;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.service.CatalogoService;
import pe.mypeinventario.service.ConsultaCatalogoService;
import pe.mypeinventario.service.TiendaService;
import pe.mypeinventario.service.dto.SolicitudProducto;
import pe.mypeinventario.service.dto.SolicitudVariante;

/**
 * Patrón Facade del catálogo: lo que la web necesita para productos, categorías y marcas.
 * Antes de tocar un producto o una tienda comprueba que sea del comerciante que lo pide;
 * si no lo es, responde "no existe", igual que si el producto no estuviera.
 * Las categorías y las marcas son un catálogo compartido y no tienen dueño.
 */
@Service
public class CatalogoFacade {

    private final CatalogoService catalogo;
    private final ConsultaCatalogoService consulta;
    private final TiendaService tiendaService;

    public CatalogoFacade(CatalogoService catalogo, ConsultaCatalogoService consulta, TiendaService tiendaService) {
        this.catalogo = catalogo;
        this.consulta = consulta;
        this.tiendaService = tiendaService;
    }

    /** @return los productos de una tienda del comerciante, ordenados por nombre */
    public List<ProductoResumen> productos(Long comercianteId, Long tiendaId, boolean soloActivos) {
        tiendaService.verificarPropiedad(comercianteId, tiendaId);
        return consulta.listarProductos(tiendaId, soloActivos);
    }

    /**
     * Devuelve un producto, siempre que sea de una tienda del comerciante.
     *
     * @throws RecursoNoEncontradoException si no existe o es de otro comerciante
     */
    public ProductoResumen producto(Long comercianteId, Long productoId) {
        ProductoResumen producto = consulta.obtenerProducto(productoId);
        try {
            tiendaService.verificarPropiedad(comercianteId, producto.tiendaId());
        } catch (RecursoNoEncontradoException e) {
            throw new RecursoNoEncontradoException("Producto", productoId);
        }
        return producto;
    }

    /** @see CatalogoService#registrarProducto(SolicitudProducto) */
    public ProductoResumen registrarProducto(Long comercianteId, SolicitudProducto solicitud) {
        tiendaService.verificarPropiedad(comercianteId, solicitud.tiendaId());
        return catalogo.registrarProducto(solicitud);
    }

    /** @see CatalogoService#agregarVariante(Long, SolicitudVariante) */
    public ProductoResumen agregarVariante(Long comercianteId, Long productoId, SolicitudVariante solicitud) {
        producto(comercianteId, productoId);
        return catalogo.agregarVariante(productoId, solicitud);
    }

    /**
     * Cambia los precios de una variante de un producto del comerciante.
     *
     * @throws RecursoNoEncontradoException si el producto es ajeno o la variante no es de ese producto
     */
    public void actualizarPrecios(Long comercianteId, Long productoId, Long varianteId,
                                  BigDecimal precioCompra, BigDecimal precioVenta) {
        boolean esDelProducto = producto(comercianteId, productoId).variantes().stream()
                .anyMatch(variante -> variante.id().equals(varianteId));
        if (!esDelProducto) {
            throw new RecursoNoEncontradoException("Variante", varianteId);
        }
        catalogo.actualizarPrecios(varianteId, precioCompra, precioVenta);
    }

    /** @see CatalogoService#cambiarEstadoProducto(Long, boolean) */
    public void cambiarEstado(Long comercianteId, Long productoId, boolean activo) {
        producto(comercianteId, productoId);
        catalogo.cambiarEstadoProducto(productoId, activo);
    }

    /** @return todas las categorías, ordenadas por nombre */
    public List<CategoriaResumen> categorias() {
        return consulta.listarCategorias();
    }

    /** @return todas las marcas, ordenadas por nombre */
    public List<MarcaResumen> marcas() {
        return consulta.listarMarcas().stream()
                .map(marca -> new MarcaResumen(marca.getId(), marca.getNombre()))
                .toList();
    }

    /** @see CatalogoService#crearCategoria(String, String, Long) */
    public CategoriaResumen crearCategoria(String nombre, String descripcion, Long categoriaPadreId) {
        return catalogo.crearCategoria(nombre, descripcion, categoriaPadreId);
    }

    /** @see CatalogoService#crearMarca(String) */
    public MarcaResumen crearMarca(String nombre) {
        var marca = catalogo.crearMarca(nombre);
        return new MarcaResumen(marca.getId(), marca.getNombre());
    }
}
