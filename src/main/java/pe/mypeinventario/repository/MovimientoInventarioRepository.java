package pe.mypeinventario.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import pe.mypeinventario.domain.MovimientoInventario;
import pe.mypeinventario.domain.TipoMovimiento;

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

    /**
     * Suma las unidades de los movimientos de un tipo ligados a una línea de venta.
     * Sirve, por ejemplo, para saber cuántas unidades de una línea ya se devolvieron.
     *
     * @param detalleVentaId identificador de la línea de venta
     * @param tipo           tipo de movimiento a sumar
     * @return unidades acumuladas, 0 si no hay movimientos
     */
    @Query("""
            select coalesce(sum(m.cantidad), 0L) from MovimientoInventario m
            where m.detalleVenta.id = :detalleVentaId and m.tipo = :tipo
            """)
    long sumarCantidadPorDetalleVenta(@Param("detalleVentaId") Long detalleVentaId,
                                      @Param("tipo") TipoMovimiento tipo);
}
