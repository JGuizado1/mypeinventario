package pe.mypeinventario.service.impl;

import org.springframework.stereotype.Component;
import pe.mypeinventario.domain.ProductoItem;
import pe.mypeinventario.domain.VarianteSKU;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.repository.ProductoItemRepository;
import pe.mypeinventario.repository.VarianteSKURepository;

/**
 * Obtiene variantes comprobando que pertenezcan a la tienda de la operación.
 * Es el lugar único de la regla "una venta o compra solo usa variantes de su tienda".
 */
@Component
class VariantesDeTienda {

    private final VarianteSKURepository varianteRepository;
    private final ProductoItemRepository productoRepository;

    VariantesDeTienda(VarianteSKURepository varianteRepository, ProductoItemRepository productoRepository) {
        this.varianteRepository = varianteRepository;
        this.productoRepository = productoRepository;
    }

    /**
     * @param varianteId           variante buscada
     * @param tiendaId             tienda a la que debe pertenecer
     * @param exigirProductoActivo si es {@code true}, rechaza productos inactivos
     * @return la variante validada
     * @throws RecursoNoEncontradoException si la variante o su producto no existen
     * @throws OperacionInvalidaException   si no pertenece a la tienda o el producto está inactivo
     */
    VarianteSKU obtener(Long varianteId, Long tiendaId, boolean exigirProductoActivo) {
        VarianteSKU variante = varianteRepository.findById(varianteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Variante", varianteId));
        ProductoItem producto = productoRepository.findByVarianteId(varianteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto de la variante", varianteId));
        if (!producto.getTienda().getId().equals(tiendaId)) {
            throw new OperacionInvalidaException(
                    "La variante " + variante.getSku() + " no pertenece a la tienda " + tiendaId);
        }
        if (exigirProductoActivo && !producto.isActivo()) {
            throw new OperacionInvalidaException(
                    "El producto de la variante " + variante.getSku() + " está inactivo");
        }
        return variante;
    }
}
