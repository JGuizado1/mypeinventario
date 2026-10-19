package pe.mypeinventario.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import pe.mypeinventario.domain.MovimientoInventario;

import java.util.List;
/**
 * Acceso de solo lectura al historial de {@link MovimientoInventario}. No expone
 * guardar ni borrar: los movimientos se crean únicamente a través de
 * {@code VarianteSKU.registrarMovimiento}, y nunca se editan ni se eliminan.
 */
public interface MovimientoInventarioRepository extends Repository<MovimientoInventario, Long> {
    /**
     * Historial de una variante, del movimiento más reciente al más antiguo.
     *
     * @param varianteId identificador de la variante
     * @return movimientos de la variante
     */
    @Query("""
            select m from VarianteSKU v join v.movimientos m
            where v.id = :varianteId
            order by m.fecha desc
            """)
    List<MovimientoInventario> listarPorVariante(@Param("varianteId") Long varianteId);
}
