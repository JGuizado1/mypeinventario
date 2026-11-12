package pe.mypeinventario.service;

import java.time.LocalDate;
import java.util.List;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.reporte.ProductoVendido;
import pe.mypeinventario.reporte.ResumenVentas;
import pe.mypeinventario.composite.NodoVentasCategoria;
import pe.mypeinventario.reporte.VarianteSinRotacion;

/** Reportes de rotación y rentabilidad. Son de solo lectura. */
public interface ReporteService {

    /**
     * Lista las variantes más vendidas en un periodo.
     *
     * @param tiendaId tienda consultada
     * @param desde    primer día del periodo (incluido)
     * @param hasta    último día del periodo (incluido)
     * @param limite   cantidad máxima de resultados, entre 1 y 100
     * @return variantes de la más a la menos vendida; vacío si no hubo ventas
     * @throws RecursoNoEncontradoException si la tienda no existe
     * @throws OperacionInvalidaException   si el periodo o el límite son inválidos
     */
    List<ProductoVendido> obtenerMasVendidos(Long tiendaId, LocalDate desde, LocalDate hasta, int limite);

    /**
     * Resume ventas, descuentos, IGV, costo y utilidad de un periodo.
     *
     * @param tiendaId tienda consultada
     * @param desde    primer día del periodo (incluido)
     * @param hasta    último día del periodo (incluido)
     * @return el resumen; con cero ventas, todos los valores son cero
     * @throws RecursoNoEncontradoException si la tienda no existe
     * @throws OperacionInvalidaException   si el periodo es inválido
     */
    ResumenVentas obtenerResumen(Long tiendaId, LocalDate desde, LocalDate hasta);

    /**
     * Lista las variantes con stock que no se venden hace tiempo, para decidir qué rematar
     * o dejar de comprar.
     *
     * @param tiendaId tienda consultada
     * @param dias     días sin ventas para considerar parada una variante, entre 1 y 3650
     * @return variantes paradas: primero las nunca vendidas y luego las de venta más antigua
     * @throws RecursoNoEncontradoException si la tienda no existe
     * @throws OperacionInvalidaException   si los datos son inválidos
     */
    List<VarianteSinRotacion> obtenerSinRotacion(Long tiendaId, int dias);

    /**
     * Calcula ventas, costo y utilidad por categoría. Cada categoría incluye lo vendido en
     * sus subcategorías. Solo aparecen las categorías con ventas en el periodo.
     *
     * @param tiendaId tienda consultada
     * @param desde    primer día del periodo (incluido)
     * @param hasta    último día del periodo (incluido)
     * @return las categorías raíz con ventas, cada una con su árbol de subcategorías
     * @throws RecursoNoEncontradoException si la tienda no existe
     * @throws OperacionInvalidaException   si el periodo es inválido
     */
    List<NodoVentasCategoria> obtenerVentasPorCategoria(Long tiendaId, LocalDate desde, LocalDate hasta);
}
