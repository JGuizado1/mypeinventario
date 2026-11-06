package pe.mypeinventario.fachada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.mypeinventario.consulta.ProductoResumen;
import pe.mypeinventario.consulta.ResumenInicio;
import pe.mypeinventario.consulta.TiendaResumen;
import pe.mypeinventario.consulta.VarianteResumen;
import pe.mypeinventario.domain.AlertaReabastecimiento;
import pe.mypeinventario.domain.EstadoAlerta;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.service.ConsultaCatalogoService;
import pe.mypeinventario.service.ConsultaInventarioService;
import pe.mypeinventario.service.CuentaService;
import pe.mypeinventario.service.TiendaService;

/** Prueba unitaria con mocks: la fachada coordina servicios, no tiene reglas propias. */
@ExtendWith(MockitoExtension.class)
class InventarioFacadeTest {

    @Mock private CuentaService cuentaService;
    @Mock private TiendaService tiendaService;
    @Mock private ConsultaCatalogoService catalogo;
    @Mock private ConsultaInventarioService inventario;
    @InjectMocks private InventarioFacade fachada;

    private static VarianteResumen variante(String sku, int stock, boolean bajo) {
        return new VarianteResumen(1L, sku, null, null, null, null, new BigDecimal("10.00"), 2, stock, bajo);
    }

    @Test
    void elResumenCuentaProductosVariantesBajasYAlertas() {
        TiendaResumen moda = new TiendaResumen(5L, "Moda Rosa", null, null);
        when(tiendaService.listarTiendas(1L)).thenReturn(List.of(moda));
        when(catalogo.listarProductos(5L, true)).thenReturn(List.of(
                new ProductoResumen(10L, 5L, "Polo", null, "Polos", null, true,
                        List.of(variante("POL-M", 1, true), variante("POL-L", 9, false))),
                new ProductoResumen(11L, 5L, "Jean", null, "Pantalones", null, true,
                        List.of(variante("JEA-30", 0, true)))));
        when(inventario.listarAlertas(5L, EstadoAlerta.PENDIENTE))
                .thenReturn(List.of(mock(AlertaReabastecimiento.class)));

        ResumenInicio resumen = fachada.resumenInicio(1L, 5L);

        assertThat(resumen.tienda()).isEqualTo(moda);
        assertThat(resumen.productosActivos()).isEqualTo(2);
        assertThat(resumen.variantesConStockBajo()).isEqualTo(2);
        assertThat(resumen.alertasPendientes()).isEqualTo(1);
        verify(tiendaService).verificarPropiedad(1L, 5L);
    }

    @Test
    void unaTiendaAjenaSeRechazaSinConsultarNada() {
        doThrow(new RecursoNoEncontradoException("Tienda", 5L)).when(tiendaService).verificarPropiedad(2L, 5L);

        assertThatThrownBy(() -> fachada.resumenInicio(2L, 5L)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> fachada.tienda(2L, 5L)).isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(catalogo, inventario);
    }

    @Test
    void laTiendaDevueltaEsLaPedida() {
        TiendaResumen moda = new TiendaResumen(5L, "Moda Rosa", null, null);
        TiendaResumen calzado = new TiendaResumen(6L, "Calzado Rosa", null, null);
        when(tiendaService.listarTiendas(1L)).thenReturn(List.of(calzado, moda));

        assertThat(fachada.tienda(1L, 5L)).isEqualTo(moda);
        assertThat(fachada.tiendasDe(1L)).containsExactly(calzado, moda);
    }
}
