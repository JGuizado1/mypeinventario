package pe.mypeinventario.fachada;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import pe.mypeinventario.composite.NodoVentasCategoria;
import pe.mypeinventario.reporte.FilaCategoria;
import pe.mypeinventario.reporte.InformeTienda;
import pe.mypeinventario.service.ReporteService;
import pe.mypeinventario.service.TiendaService;

/**
 * Patrón Facade de los reportes: arma de una sola vez todo lo que muestra la pantalla de
 * reportes. También recorre el árbol de categorías (patrón Composite) y lo entrega aplanado,
 * con el nivel de cada categoría, para que la web solo tenga que pintar filas.
 */
@Service
public class ReporteFacade {

    private final ReporteService reporteService;
    private final TiendaService tiendaService;

    public ReporteFacade(ReporteService reporteService, TiendaService tiendaService) {
        this.reporteService = reporteService;
        this.tiendaService = tiendaService;
    }

    /**
     * Arma el informe de una tienda del comerciante para un periodo.
     *
     * @param desde             primer día del periodo (incluido)
     * @param hasta             último día del periodo (incluido)
     * @param diasSinRotacion   días sin ventas para considerar parada una variante
     * @param limiteMasVendidos cuántas variantes mostrar entre las más vendidas
     */
    public InformeTienda informe(Long comercianteId, Long tiendaId, LocalDate desde, LocalDate hasta,
                                 int diasSinRotacion, int limiteMasVendidos) {
        tiendaService.verificarPropiedad(comercianteId, tiendaId);
        return new InformeTienda(
                reporteService.obtenerResumen(tiendaId, desde, hasta),
                reporteService.obtenerMasVendidos(tiendaId, desde, hasta, limiteMasVendidos),
                aplanar(reporteService.obtenerVentasPorCategoria(tiendaId, desde, hasta)),
                reporteService.obtenerSinRotacion(tiendaId, diasSinRotacion));
    }

    /** Recorre el árbol de categorías en profundidad y deja una fila por categoría. */
    private List<FilaCategoria> aplanar(List<NodoVentasCategoria> raices) {
        List<FilaCategoria> filas = new ArrayList<>();
        raices.forEach(raiz -> agregar(raiz, 0, filas));
        return filas;
    }

    private void agregar(NodoVentasCategoria nodo, int nivel, List<FilaCategoria> filas) {
        filas.add(new FilaCategoria(nodo.nombre(), nivel, nodo.unidades(), nodo.ingresos(), nodo.costo(),
                nodo.utilidad()));
        nodo.subcategorias().forEach(hijo -> agregar(hijo, nivel + 1, filas));
    }
}
