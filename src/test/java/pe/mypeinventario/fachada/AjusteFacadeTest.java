package pe.mypeinventario.fachada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.mypeinventario.consulta.MovimientoResumen;
import pe.mypeinventario.consulta.ProductoResumen;
import pe.mypeinventario.consulta.VarianteResumen;
import pe.mypeinventario.domain.MovimientoInventario;
import pe.mypeinventario.domain.TipoMovimiento;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.service.AjusteInventarioService;
import pe.mypeinventario.service.ConsultaCatalogoService;
import pe.mypeinventario.service.ConsultaInventarioService;
import pe.mypeinventario.service.TiendaService;

/** Prueba unitaria con mocks: nadie ajusta ni ve el historial de una variante que no es de su tienda. */
@ExtendWith(MockitoExtension.class)
class AjusteFacadeTest {

    @Mock private AjusteInventarioService ajusteService;
    @Mock private ConsultaInventarioService inventario;
    @Mock private ConsultaCatalogoService catalogo;
    @Mock private TiendaService tiendaService;
    @InjectMocks private AjusteFacade fachada;

    private static List<ProductoResumen> productosConVariante(Long varianteId) {
        VarianteResumen variante = new VarianteResumen(varianteId, "POL-M-AZU", null, "M", "Azul", null,
                new BigDecimal("29.90"), 5, 10, false);
        return List.of(new ProductoResumen(10L, 5L, "Polo básico", null, "Polos", null, true, List.of(variante)));
    }

    @Test
    void unAjusteSobreUnaVarianteDeLaTiendaSeRegistra() {
        when(catalogo.listarProductos(5L, false)).thenReturn(productosConVariante(100L));

        fachada.ajustar(1L, 5L, 100L, TipoMovimiento.AJUSTE_NEGATIVO, 3, "Mercadería dañada");

        verify(ajusteService).registrarAjuste(100L, TipoMovimiento.AJUSTE_NEGATIVO, 3, "Mercadería dañada");
    }

    @Test
    void unaVarianteQueNoEsDeLaTiendaSeNiegaComoSiNoExistiera() {
        when(catalogo.listarProductos(5L, false)).thenReturn(productosConVariante(100L));

        assertThatThrownBy(() -> fachada.ajustar(1L, 5L, 999L, TipoMovimiento.AJUSTE_POSITIVO, 1, "Sobra"))
                .isInstanceOf(RecursoNoEncontradoException.class).hasMessageContaining("Variante");
        assertThatThrownBy(() -> fachada.historial(1L, 5L, 999L)).isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(ajusteService, inventario);
    }

    @Test
    void unaTiendaAjenaSeNiegaSinConsultarNada() {
        doThrow(new RecursoNoEncontradoException("Tienda", 5L)).when(tiendaService).verificarPropiedad(2L, 5L);

        assertThatThrownBy(() -> fachada.productos(2L, 5L)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> fachada.ajustar(2L, 5L, 100L, TipoMovimiento.AJUSTE_POSITIVO, 1, "x"))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(catalogo, ajusteService, inventario);
    }

    @Test
    void elHistorialSaleDelMovimientoMasRecienteAlMasAntiguoConSuEfecto() {
        when(catalogo.listarProductos(5L, false)).thenReturn(productosConVariante(100L));
        MovimientoInventario compra = new MovimientoInventario(1L, TipoMovimiento.COMPRA, 10,
                LocalDateTime.of(2026, 11, 6, 10, 0), "Compra inicial", null, null);
        MovimientoInventario ajuste = new MovimientoInventario(2L, TipoMovimiento.AJUSTE_NEGATIVO, 3,
                LocalDateTime.of(2026, 11, 7, 10, 0), "Mercadería dañada", null, null);
        when(inventario.obtenerHistorial(100L)).thenReturn(List.of(compra, ajuste));

        List<MovimientoResumen> historial = fachada.historial(1L, 5L, 100L);

        assertThat(historial).extracting(MovimientoResumen::efectoTexto).containsExactly("-3", "+10");
        assertThat(historial).extracting(MovimientoResumen::tipoTexto)
                .containsExactly("Ajuste (resta)", "Compra");
        assertThat(historial.get(0).motivo()).isEqualTo("Mercadería dañada");
    }
}
