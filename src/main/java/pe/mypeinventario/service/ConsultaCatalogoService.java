package pe.mypeinventario.service;

import java.util.List;
import pe.mypeinventario.consulta.CategoriaResumen;
import pe.mypeinventario.consulta.ProductoResumen;
import pe.mypeinventario.domain.Marca;
import pe.mypeinventario.exception.RecursoNoEncontradoException;

/** Consultas de solo lectura sobre el catálogo. No modifica ningún dato. */
public interface ConsultaCatalogoService {

    /** @return todas las categorías, ordenadas por nombre */
    List<CategoriaResumen> listarCategorias();

    /** @return todas las marcas, ordenadas por nombre */
    List<Marca> listarMarcas();

    /**
     * Lista los productos de una tienda con el stock de cada variante.
     *
     * @param tiendaId    tienda consultada
     * @param soloActivos {@code true} para omitir los productos inactivos
     * @return productos ordenados por nombre
     * @throws RecursoNoEncontradoException si la tienda no existe
     */
    List<ProductoResumen> listarProductos(Long tiendaId, boolean soloActivos);

    /**
     * Obtiene un producto con sus variantes.
     *
     * @param productoId identificador del producto
     * @return el producto
     * @throws RecursoNoEncontradoException si el producto no existe
     */
    ProductoResumen obtenerProducto(Long productoId);
}
