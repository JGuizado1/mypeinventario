package pe.mypeinventario.service.impl;

import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.mypeinventario.consulta.CategoriaResumen;
import pe.mypeinventario.consulta.ProductoResumen;
import pe.mypeinventario.domain.Marca;
import pe.mypeinventario.domain.ProductoItem;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.repository.CategoriaRepository;
import pe.mypeinventario.repository.MarcaRepository;
import pe.mypeinventario.repository.ProductoItemRepository;
import pe.mypeinventario.repository.TiendaRepository;
import pe.mypeinventario.service.ConsultaCatalogoService;

/** Implementación de {@link ConsultaCatalogoService}: solo lectura. */
@Service
@Transactional(readOnly = true)
public class ConsultaCatalogoServiceImpl implements ConsultaCatalogoService {

    private static final Sort POR_NOMBRE = Sort.by("nombre");

    private final CategoriaRepository categoriaRepository;
    private final MarcaRepository marcaRepository;
    private final ProductoItemRepository productoRepository;
    private final TiendaRepository tiendaRepository;
    private final ResumenesCatalogo resumenes;

    public ConsultaCatalogoServiceImpl(CategoriaRepository categoriaRepository,
                                       MarcaRepository marcaRepository,
                                       ProductoItemRepository productoRepository,
                                       TiendaRepository tiendaRepository,
                                       ResumenesCatalogo resumenes) {
        this.categoriaRepository = categoriaRepository;
        this.marcaRepository = marcaRepository;
        this.productoRepository = productoRepository;
        this.tiendaRepository = tiendaRepository;
        this.resumenes = resumenes;
    }

    @Override
    public List<CategoriaResumen> listarCategorias() {
        return categoriaRepository.findAll(POR_NOMBRE).stream().map(resumenes::resumir).toList();
    }

    @Override
    public List<Marca> listarMarcas() {
        return marcaRepository.findAll(POR_NOMBRE);
    }

    @Override
    public List<ProductoResumen> listarProductos(Long tiendaId, boolean soloActivos) {
        if (!tiendaRepository.existsById(tiendaId)) {
            throw new RecursoNoEncontradoException("Tienda", tiendaId);
        }
        List<ProductoItem> productos = soloActivos
                ? productoRepository.findByTiendaIdAndActivoTrue(tiendaId)
                : productoRepository.findByTiendaId(tiendaId);
        return productos.stream()
                .sorted(Comparator.comparing(ProductoItem::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(resumenes::resumir)
                .toList();
    }

    @Override
    public ProductoResumen obtenerProducto(Long productoId) {
        return productoRepository.findById(productoId)
                .map(resumenes::resumir)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto", productoId));
    }
}
