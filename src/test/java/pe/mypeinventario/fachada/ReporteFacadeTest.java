package pe.mypeinventario.fachada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.mypeinventario.composite.GrupoVentasCategoria;
import pe.mypeinventario.composite.NodoVentasCategoria;
import pe.mypeinventario.composite.VentasDeCategoria;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.reporte.FilaCategoria;
import pe.mypeinventario.reporte.InformeTienda;
import pe.mypeinventario.reporte.ResumenVentas;
import pe.mypeinventario.service.ReporteService;
import pe.mypeinventario.service.TiendaService;

/** Prueba unitaria con mocks: el informe es de una tienda propia y aplana bien el árbol de categorías. */
@ExtendWith(MockitoExtension.class)
class ReporteFacadeTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 11, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 11, 6);

    @Mock private ReporteService reporteService;
    @Mock private TiendaService tiendaService;
    @InjectMocks private ReporteFacade fachada;

    private static VentasDeCategoria ventas(String nombre, long unidades, String ingresos, String costo) {
        return new VentasDeCategoria(nombre, unidades, new BigDecimal(ingresos), new BigDecimal(costo));
    }

    @Test
    void elInformeDeUnaTiendaAjenaSeNiegaSinConsultarNada() {
        doThrow(new RecursoNoEncontradoException("Tienda", 5L)).when(tiendaService).verificarPropiedad(2L, 5L);

        assertThatThrownBy(() -> fachada.informe(2L, 5L, DESDE, HASTA, 30, 10))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(reporteService);
    }

    @Test
    void elInformeJuntaLosCuatroReportesYAplanaElArbolDeCategorias() {
        GrupoVentasCategoria ropa = new GrupoVentasCategoria("Ropa", ventas("Ropa", 0, "0", "0"));
        ropa.agregar(ventas("Polos", 8, "239.20", "96.00"));
        ropa.agregar(ventas("Pantalones", 2, "100.00", "40.00"));
        List<NodoVentasCategoria> arbol = List.of(ropa, ventas("Calzado", 1, "80.00", "50.00"));
        ResumenVentas resumen = new ResumenVentas(3, new BigDecimal("419.20"), BigDecimal.ZERO,
                new BigDecimal("419.20"), new BigDecimal("63.95"), new BigDecimal("186.00"),
                new BigDecimal("233.20"), 0);
        when(reporteService.obtenerResumen(5L, DESDE, HASTA)).thenReturn(resumen);
        when(reporteService.obtenerMasVendidos(5L, DESDE, HASTA, 10)).thenReturn(List.of());
        when(reporteService.obtenerVentasPorCategoria(5L, DESDE, HASTA)).thenReturn(arbol);
        when(reporteService.obtenerSinRotacion(5L, 30)).thenReturn(List.of());

        InformeTienda informe = fachada.informe(1L, 5L, DESDE, HASTA, 30, 10);

        assertThat(informe.resumen()).isEqualTo(resumen);
        assertThat(informe.categorias()).extracting(FilaCategoria::nombre)
                .containsExactly("Ropa", "Polos", "Pantalones", "Calzado");
        assertThat(informe.categorias()).extracting(FilaCategoria::nivel).containsExactly(0, 1, 1, 0);
        FilaCategoria filaRopa = informe.categorias().get(0);
        assertThat(filaRopa.unidades()).isEqualTo(10);
        assertThat(filaRopa.ingresos()).isEqualByComparingTo("339.20");
        assertThat(filaRopa.utilidad()).isEqualByComparingTo("203.20");
        assertThat(informe.categorias().get(1).utilidad()).isEqualByComparingTo("143.20");
    }
}
