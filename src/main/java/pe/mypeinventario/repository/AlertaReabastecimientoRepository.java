package pe.mypeinventario.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.mypeinventario.consulta.AlertaResumen;
import pe.mypeinventario.domain.AlertaReabastecimiento;
import pe.mypeinventario.domain.EstadoAlerta;

public interface AlertaReabastecimientoRepository extends JpaRepository<AlertaReabastecimiento, Long> {
    @Query("""
            select a from VarianteSKU v join v.alertas a
            where v.id = :varianteId
            order by a.fechaGeneracion desc
            """)
    List<AlertaReabastecimiento> listarPorVariante(@Param("varianteId") Long varianteId);

    /**
     * Alertas de una tienda en un estado dado, por ejemplo las pendientes.
     *
     * @param tiendaId identificador de la tienda
     * @param estado   estado buscado
     * @return alertas encontradas, de la más reciente a la más antigua
     */
    @Query("""
            select a from ProductoItem p join p.variantes v join v.alertas a
            where p.tienda.id = :tiendaId and a.estado = :estado
            order by a.fechaGeneracion desc
            """)
    List<AlertaReabastecimiento> listarPorTiendaYEstado(@Param("tiendaId") Long tiendaId,
                                                        @Param("estado") EstadoAlerta estado);

    /**
     * Indica si una variante ya tiene una alerta en alguno de los estados dados.
     * Sirve para no generar alertas duplicadas.
     *
     * @param varianteId identificador de la variante
     * @param estados    estados a considerar
     * @return {@code true} si existe al menos una
     */
    @Query("""
            select count(a) > 0 from VarianteSKU v join v.alertas a
            where v.id = :varianteId and a.estado in :estados
            """)
    boolean existeAlertaEnEstados(@Param("varianteId") Long varianteId,
                                  @Param("estados") Collection<EstadoAlerta> estados);

    /**
     * Alertas de una tienda en alguno de los estados dados, ya con los datos de la variante
     * y del producto que la web necesita para mostrarlas.
     *
     * @param tiendaId identificador de la tienda
     * @param estados  estados buscados
     * @return resúmenes de la más reciente a la más antigua
     */
    @Query("""
            select new pe.mypeinventario.consulta.AlertaResumen(a.id, p.tienda.id, v.id, v.sku, p.nombre,
                   v.talla, v.color, a.fechaGeneracion, a.stockActual, a.stockMinimo, a.estado, a.mensaje)
            from ProductoItem p join p.variantes v join v.alertas a
            where p.tienda.id = :tiendaId and a.estado in :estados
            order by a.fechaGeneracion desc, a.id desc
            """)
    List<AlertaResumen> resumenesPorTiendaYEstados(@Param("tiendaId") Long tiendaId,
                                                   @Param("estados") Collection<EstadoAlerta> estados);

    /**
     * Una alerta con los datos de su variante y su producto.
     *
     * @param alertaId identificador de la alerta
     * @return el resumen, o vacío si la alerta no existe
     */
    @Query("""
            select new pe.mypeinventario.consulta.AlertaResumen(a.id, p.tienda.id, v.id, v.sku, p.nombre,
                   v.talla, v.color, a.fechaGeneracion, a.stockActual, a.stockMinimo, a.estado, a.mensaje)
            from ProductoItem p join p.variantes v join v.alertas a
            where a.id = :alertaId
            """)
    Optional<AlertaResumen> resumenPorId(@Param("alertaId") Long alertaId);
}
