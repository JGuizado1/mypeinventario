package pe.mypeinventario.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import pe.mypeinventario.domain.VentaPunto;
import pe.mypeinventario.reporte.ProductoVendido;
import pe.mypeinventario.reporte.TotalesVentas;

/**
 * Consultas de solo lectura para los reportes de ventas. Todas reciben el periodo
 * como un intervalo cerrado al inicio y abierto al final: {@code [desde, hasta)}.
 */
public interface ReporteVentasRepository extends Repository<VentaPunto, Long> {

    /**
     * Variantes vendidas en el periodo, de la más a la menos vendida.
     *
     * @param tiendaId tienda consultada
     * @param desde    inicio del periodo (incluido)
     * @param hasta    fin del periodo (excluido)
     * @param pagina   permite limitar la cantidad de resultados
     * @return variantes con unidades e ingresos a precio de lista
     */
    @Query("""
            select new pe.mypeinventario.reporte.ProductoVendido(
                   v.id, v.sku, p.nombre, v.talla, v.color, sum(d.cantidad), sum(d.cantidad * d.precioUnitario))
            from VentaPunto vp join vp.detalles d join d.variante v, ProductoItem p join p.variantes pv
            where pv.id = v.id and vp.tienda.id = :tiendaId
              and vp.fecha >= :desde and vp.fecha < :hasta
            group by v.id, v.sku, p.nombre, v.talla, v.color
            order by sum(d.cantidad) desc
            """)
    List<ProductoVendido> masVendidos(@Param("tiendaId") Long tiendaId,
                                      @Param("desde") LocalDateTime desde,
                                      @Param("hasta") LocalDateTime hasta,
                                      Pageable pagina);

    /** Totales guardados de las ventas del periodo; con cero ventas devuelve ceros. */
    @Query("""
            select new pe.mypeinventario.reporte.TotalesVentas(
                   count(vp), sum(vp.subtotal), sum(vp.descuento), sum(vp.total), sum(vp.igv))
            from VentaPunto vp
            where vp.tienda.id = :tiendaId and vp.fecha >= :desde and vp.fecha < :hasta
            """)
    TotalesVentas totalesDelPeriodo(@Param("tiendaId") Long tiendaId,
                                    @Param("desde") LocalDateTime desde,
                                    @Param("hasta") LocalDateTime hasta);

    /** Costo de lo vendido en las líneas con costo conocido; {@code null} si no hay ninguna. */
    @Query("""
            select sum(d.costoUnitario * d.cantidad)
            from VentaPunto vp join vp.detalles d
            where vp.tienda.id = :tiendaId and vp.fecha >= :desde and vp.fecha < :hasta
              and d.costoUnitario is not null
            """)
    BigDecimal costoDelPeriodo(@Param("tiendaId") Long tiendaId,
                               @Param("desde") LocalDateTime desde,
                               @Param("hasta") LocalDateTime hasta);

    /** Cantidad de líneas vendidas cuyo costo no se conocía. */
    @Query("""
            select count(d)
            from VentaPunto vp join vp.detalles d
            where vp.tienda.id = :tiendaId and vp.fecha >= :desde and vp.fecha < :hasta
              and d.costoUnitario is null
            """)
    long contarLineasSinCosto(@Param("tiendaId") Long tiendaId,
                              @Param("desde") LocalDateTime desde,
                              @Param("hasta") LocalDateTime hasta);
}
