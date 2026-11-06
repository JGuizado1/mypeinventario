package pe.mypeinventario.service.impl;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.mypeinventario.consulta.CategoriaResumen;
import pe.mypeinventario.consulta.ProductoResumen;
import pe.mypeinventario.domain.Categoria;
import pe.mypeinventario.domain.Marca;
import pe.mypeinventario.domain.ProductoItem;
import pe.mypeinventario.domain.Tienda;
import pe.mypeinventario.domain.VarianteSKU;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.repository.CategoriaRepository;
import pe.mypeinventario.repository.MarcaRepository;
import pe.mypeinventario.repository.ProductoItemRepository;
import pe.mypeinventario.repository.TiendaRepository;
import pe.mypeinventario.repository.VarianteSKURepository;
import pe.mypeinventario.service.CatalogoService;
import pe.mypeinventario.service.dto.SolicitudProducto;
import pe.mypeinventario.service.dto.SolicitudVariante;

/** Implementación de {@link CatalogoService}. */
@Service
@Transactional
public class CatalogoServiceImpl implements CatalogoService {

    private final CategoriaRepository categoriaRepository;
    private final MarcaRepository marcaRepository;
    private final ProductoItemRepository productoRepository;
    private final VarianteSKURepository varianteRepository;
    private final TiendaRepository tiendaRepository;
    private final ResumenesCatalogo resumenes;

    public CatalogoServiceImpl(CategoriaRepository categoriaRepository,
                               MarcaRepository marcaRepository,
                               ProductoItemRepository productoRepository,
                               VarianteSKURepository varianteRepository,
                               TiendaRepository tiendaRepository,
                               ResumenesCatalogo resumenes) {
        this.categoriaRepository = categoriaRepository;
        this.marcaRepository = marcaRepository;
        this.productoRepository = productoRepository;
        this.varianteRepository = varianteRepository;
        this.tiendaRepository = tiendaRepository;
        this.resumenes = resumenes;
    }

    @Override
    public CategoriaResumen crearCategoria(String nombre, String descripcion, Long categoriaPadreId) {
        String limpio = exigirTexto(nombre, "El nombre de la categoría es obligatorio");
        if (categoriaRepository.existsByNombreIgnoreCase(limpio)) {
            throw new OperacionInvalidaException("Ya existe una categoría llamada " + limpio);
        }
        Categoria padre = categoriaPadreId == null ? null : categoriaRepository.findById(categoriaPadreId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría", categoriaPadreId));
        Categoria nueva = categoriaRepository.save(new Categoria(null, limpio, textoOpcional(descripcion), padre));
        return resumenes.resumir(nueva);
    }

    @Override
    public Marca crearMarca(String nombre) {
        String limpio = exigirTexto(nombre, "El nombre de la marca es obligatorio");
        if (marcaRepository.findByNombreIgnoreCase(limpio).isPresent()) {
            throw new OperacionInvalidaException("Ya existe una marca llamada " + limpio);
        }
        return marcaRepository.save(new Marca(null, limpio));
    }

    @Override
    public ProductoResumen registrarProducto(SolicitudProducto solicitud) {
        Tienda tienda = tiendaRepository.findById(solicitud.tiendaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Tienda", solicitud.tiendaId()));
        Categoria categoria = categoriaRepository.findById(solicitud.categoriaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría", solicitud.categoriaId()));
        Marca marca = solicitud.marcaId() == null ? null : marcaRepository.findById(solicitud.marcaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Marca", solicitud.marcaId()));

        exigirSinRepetidos(solicitud.variantes());
        solicitud.variantes().forEach(variante -> exigirLibreEnTienda(tienda.getId(), variante));

        ProductoItem producto = new ProductoItem(null, solicitud.nombre(), solicitud.descripcion(), tienda, marca);
        categoria.agregarProducto(producto);
        solicitud.variantes().forEach(variante -> producto.agregarVariante(crearVariante(variante)));
        return resumenes.resumir(productoRepository.saveAndFlush(producto));
    }

    @Override
    public ProductoResumen agregarVariante(Long productoId, SolicitudVariante solicitud) {
        ProductoItem producto = exigirProducto(productoId);
        exigirLibreEnTienda(producto.getTienda().getId(), solicitud);
        producto.agregarVariante(crearVariante(solicitud));
        // saveAndFlush: la variante nueva recibe su identificador antes de calcular su stock.
        return resumenes.resumir(productoRepository.saveAndFlush(producto));
    }

    @Override
    public void actualizarPrecios(Long varianteId, BigDecimal precioCompra, BigDecimal precioVenta) {
        VarianteSKU variante = varianteRepository.findById(varianteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Variante", varianteId));
        if (precioVenta == null || precioVenta.signum() <= 0) {
            throw new OperacionInvalidaException("El precio de venta debe ser mayor que cero");
        }
        if (precioCompra != null && precioCompra.signum() < 0) {
            throw new OperacionInvalidaException("El precio de compra no puede ser negativo");
        }
        if (precioCompra != null) {
            variante.setPrecioCompra(precioCompra);
        }
        variante.setPrecioVenta(precioVenta);
    }

    @Override
    public void cambiarEstadoProducto(Long productoId, boolean activo) {
        exigirProducto(productoId).setActivo(activo);
    }

    private ProductoItem exigirProducto(Long productoId) {
        return productoRepository.findById(productoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto", productoId));
    }

    private VarianteSKU crearVariante(SolicitudVariante datos) {
        return new VarianteSKU(null, datos.sku(), datos.codigoBarras(), datos.talla(), datos.color(),
                datos.precioCompra(), datos.precioVenta(), datos.stockMinimo());
    }

    /** Dentro de una misma solicitud no se puede repetir el SKU ni el código de barras. */
    private void exigirSinRepetidos(List<SolicitudVariante> variantes) {
        Set<String> skus = new HashSet<>();
        Set<String> codigos = new HashSet<>();
        for (SolicitudVariante variante : variantes) {
            if (!skus.add(variante.sku())) {
                throw new OperacionInvalidaException("El SKU " + variante.sku() + " está repetido en la solicitud");
            }
            if (variante.codigoBarras() != null && !codigos.add(variante.codigoBarras())) {
                throw new OperacionInvalidaException(
                        "El código de barras " + variante.codigoBarras() + " está repetido en la solicitud");
            }
        }
    }

    /** El SKU y el código de barras son únicos dentro de una tienda, no en todo el sistema. */
    private void exigirLibreEnTienda(Long tiendaId, SolicitudVariante variante) {
        if (varianteRepository.buscarPorSkuEnTienda(tiendaId, variante.sku()).isPresent()) {
            throw new OperacionInvalidaException("Ya existe el SKU " + variante.sku() + " en la tienda");
        }
        if (variante.codigoBarras() != null
                && varianteRepository.buscarPorCodigoBarrasEnTienda(tiendaId, variante.codigoBarras()).isPresent()) {
            throw new OperacionInvalidaException(
                    "Ya existe el código de barras " + variante.codigoBarras() + " en la tienda");
        }
    }

    private static String exigirTexto(String texto, String mensaje) {
        if (texto == null || texto.isBlank()) {
            throw new OperacionInvalidaException(mensaje);
        }
        return texto.trim();
    }

    private static String textoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
