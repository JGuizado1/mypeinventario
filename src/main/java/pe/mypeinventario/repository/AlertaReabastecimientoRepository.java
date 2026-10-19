package pe.mypeinventario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.mypeinventario.domain.AlertaReabastecimiento;
import pe.mypeinventario.domain.EstadoAlerta;

import java.util.Collection;
import java.util.List;

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
}
