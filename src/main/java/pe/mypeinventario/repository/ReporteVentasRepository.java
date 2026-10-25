package pe.mypeinventario.repository;

import java.util.Collection;
import pe.mypeinventario.domain.TipoMovimiento;
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

    /**
     * Variantes de productos activos con stock mayor que cero cuya última venta es anterior
     * al límite, o que nunca se vendieron. Cada fila trae, en este orden: id, sku, producto,
     * talla, color, costo, stock y fecha de la última venta (puede ser nula).
     */
    @Query("""
            select v.id, v.sku, p.nombre, v.talla, v.color, v.precioCompra,
                   sum(case when m.tipo in :entradas then m.cantidad else -m.cantidad end),
                   max(case when m.tipo = :tipoVenta then m.fecha else null end)
            from ProductoItem p join p.variantes v join v.movimientos m
            where p.tienda.id = :tiendaId and p.activo = true
            group by v.id, v.sku, p.nombre, v.talla, v.color, v.precioCompra
            having sum(case when m.tipo in :entradas then m.cantidad else -m.cantidad end) > 0
               and (max(case when m.tipo = :tipoVenta then m.fecha else null end) is null
                    or max(case when m.tipo = :tipoVenta then m.fecha else null end) < :limite)
            """)
    List<Object[]> variantesConStockSinVentasDesde(@Param("tiendaId") Long tiendaId,
                                                   @Param("entradas") Collection<TipoMovimiento> entradas,
                                                   @Param("tipoVenta") TipoMovimiento tipoVenta,
                                                   @Param("limite") LocalDateTime limite);

    /**
     * Ventas del periodo agrupadas por la categoría directa del producto. Cada fila trae:
     * id de la categoría, unidades, ingresos y costo (el costo solo suma líneas con costo conocido).
     */
    @Query("""
            select c.id, sum(d.cantidad), sum(d.cantidad * d.precioUnitario), sum(d.cantidad * d.costoUnitario)
            from VentaPunto vp join vp.detalles d join d.variante v, ProductoItem p join p.variantes pv join p.categoria c
            where pv.id = v.id and vp.tienda.id = :tiendaId
              and vp.fecha >= :desde and vp.fecha < :hasta
            group by c.id
            """)
    List<Object[]> ventasPorCategoria(@Param("tiendaId") Long tiendaId,
                                      @Param("desde") LocalDateTime desde,
                                      @Param("hasta") LocalDateTime hasta);
}
