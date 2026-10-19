package pe.mypeinventario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.mypeinventario.domain.TipoMovimiento;
import pe.mypeinventario.domain.VarianteSKU;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VarianteSKURepository extends JpaRepository<VarianteSKU, Long> {
    /** Un mismo SKU puede repetirse entre tiendas, por eso se busca dentro de una tienda. */
    @Query("""
            select v from ProductoItem p join p.variantes v
            where p.tienda.id = :tiendaId and v.sku = :sku
            """)
    Optional<VarianteSKU> buscarPorSkuEnTienda(@Param("tiendaId") Long tiendaId,
                                               @Param("sku") String sku);

    @Query("""
            select v from ProductoItem p join p.variantes v
            where p.tienda.id = :tiendaId and v.codigoBarras = :codigoBarras
            """)
    Optional<VarianteSKU> buscarPorCodigoBarrasEnTienda(@Param("tiendaId") Long tiendaId,
                                                        @Param("codigoBarras") String codigoBarras);

    @Query("select v from ProductoItem p join p.variantes v where p.tienda.id = :tiendaId")
    List<VarianteSKU> listarPorTienda(@Param("tiendaId") Long tiendaId);

    /**
     * Suma los movimientos de una variante directamente en la base de datos: las
     * entradas suman y el resto resta. Devuelve 0 si la variante no tiene movimientos.
     *
     * @param varianteId identificador de la variante
     * @param entradas   tipos de movimiento que aumentan el stock
     * @return stock calculado
     */
    @Query("""
            select coalesce(sum(case when m.tipo in :entradas then m.cantidad else -m.cantidad end), 0L)
            from VarianteSKU v join v.movimientos m
            where v.id = :varianteId
            """)
    long sumarMovimientos(@Param("varianteId") Long varianteId,
                          @Param("entradas") Collection<TipoMovimiento> entradas);

    /**
     * Calcula el stock actual de una variante. El signo de cada tipo se toma de
     * {@link TipoMovimiento}, de modo que esta consulta no cambia si se agrega un tipo.
     *
     * @param varianteId identificador de la variante
     * @return unidades disponibles
     */
    default int calcularStock(Long varianteId) {
        return (int) sumarMovimientos(varianteId, TipoMovimiento.entradas());
    }
}
