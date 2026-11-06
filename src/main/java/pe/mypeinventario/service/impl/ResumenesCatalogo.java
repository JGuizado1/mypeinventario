package pe.mypeinventario.service.impl;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;
import pe.mypeinventario.config.ParametrosStock;
import pe.mypeinventario.consulta.CategoriaResumen;
import pe.mypeinventario.consulta.ProductoResumen;
import pe.mypeinventario.consulta.VarianteResumen;
import pe.mypeinventario.domain.Categoria;
import pe.mypeinventario.domain.ProductoItem;
import pe.mypeinventario.domain.VarianteSKU;
import pe.mypeinventario.repository.VarianteSKURepository;

/**
 * Convierte entidades del catálogo en resúmenes de solo lectura. Es lo único que sabe
 * armar un resumen, de modo que los dos servicios del catálogo no repitan esa lógica.
 * Debe usarse dentro de una transacción, porque lee relaciones perezosas.
 */
@Component
class ResumenesCatalogo {

    private final VarianteSKURepository varianteRepository;
    private final ParametrosStock parametros;

    ResumenesCatalogo(VarianteSKURepository varianteRepository, ParametrosStock parametros) {
        this.varianteRepository = varianteRepository;
        this.parametros = parametros;
    }

    CategoriaResumen resumir(Categoria categoria) {
        Categoria padre = categoria.getCategoriaPadre();
        return new CategoriaResumen(categoria.getId(), categoria.getNombre(), categoria.getDescripcion(),
                padre == null ? null : padre.getId(), padre == null ? null : padre.getNombre());
    }

    ProductoResumen resumir(ProductoItem producto) {
        List<VarianteResumen> variantes = producto.getVariantes().stream()
                .sorted(Comparator.comparing(VarianteSKU::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::resumir)
                .toList();
        return new ProductoResumen(producto.getId(), producto.getTienda().getId(), producto.getNombre(),
                producto.getDescripcion(), producto.getCategoria().getNombre(),
                producto.getMarca() == null ? null : producto.getMarca().getNombre(),
                producto.isActivo(), variantes);
    }

    VarianteResumen resumir(VarianteSKU variante) {
        // El stock se suma en la base de datos: no se cargan todos los movimientos de la variante.
        int stock = varianteRepository.calcularStock(variante.getId());
        int stockMinimo = variante.resolverStockMinimo(parametros);
        return new VarianteResumen(variante.getId(), variante.getSku(), variante.getCodigoBarras(),
                variante.getTalla(), variante.getColor(), variante.getPrecioCompra(), variante.getPrecioVenta(),
                stockMinimo, stock, stock <= stockMinimo);
    }
}
