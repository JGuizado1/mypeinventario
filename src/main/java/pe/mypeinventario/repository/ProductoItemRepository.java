package pe.mypeinventario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.mypeinventario.domain.ProductoItem;

import java.util.List;
import java.util.Optional;

public interface ProductoItemRepository extends JpaRepository<ProductoItem, Long> {
    List<ProductoItem> findByTiendaId(Long tiendaId);

    List<ProductoItem> findByTiendaIdAndActivoTrue(Long tiendaId);

    List<ProductoItem> findByCategoriaId(Long categoriaId);

    /**
     * Obtiene el producto al que pertenece una variante. Sirve, por ejemplo, para
     * comprobar que una venta use variantes de su misma tienda.
     *
     * @param varianteId identificador de la variante
     * @return el producto dueño de la variante, si existe
     */
    @Query("select p from ProductoItem p join p.variantes v where v.id = :varianteId")
    Optional<ProductoItem> findByVarianteId(@Param("varianteId") Long varianteId);
}
