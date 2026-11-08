package pe.mypeinventario.fachada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
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
import pe.mypeinventario.consulta.CompraResumen;
import pe.mypeinventario.consulta.LineaCompraResumen;
import pe.mypeinventario.consulta.ProveedorResumen;
import pe.mypeinventario.domain.Compra;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.service.CompraService;
import pe.mypeinventario.service.ConsultaCatalogoService;
import pe.mypeinventario.service.ConsultaCompraService;
import pe.mypeinventario.service.ProveedorService;
import pe.mypeinventario.service.TiendaService;
import pe.mypeinventario.service.dto.LineaCompra;
import pe.mypeinventario.service.dto.SolicitudCompra;
import pe.mypeinventario.service.dto.SolicitudProveedor;

/** Prueba unitaria con mocks: nadie compra para una tienda ajena ni ve compras de otro comerciante. */
@ExtendWith(MockitoExtension.class)
class CompraFacadeTest {

    @Mock private CompraService compraService;
    @Mock private ConsultaCompraService consultaCompra;
    @Mock private ConsultaCatalogoService catalogo;
    @Mock private ProveedorService proveedorService;
    @Mock private TiendaService tiendaService;
    @InjectMocks private CompraFacade fachada;

    private static CompraResumen compra(Long id, Long tiendaId) {
        LineaCompraResumen linea = new LineaCompraResumen(100L, "POL-M-NEG", "Polo básico", "M", "Negro",
                10, new BigDecimal("12.00"), new BigDecimal("120.00"));
        return new CompraResumen(id, tiendaId, LocalDateTime.of(2026, 11, 6, 19, 30), "Textiles Gamarra SAC",
                new BigDecimal("120.00"), List.of(linea));
    }

    private static SolicitudCompra solicitud() {
        return new SolicitudCompra(5L, 7L, List.of(new LineaCompra(100L, 10, new BigDecimal("12.00"))));
    }

    @Test
    void lasComprasDeUnaTiendaAjenaSeNiegan() {
        doThrow(new RecursoNoEncontradoException("Tienda", 5L)).when(tiendaService).verificarPropiedad(2L, 5L);

        assertThatThrownBy(() -> fachada.compras(2L, 5L)).isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(consultaCompra);
    }

    @Test
    void unaCompraPropiaSeDevuelve() {
        when(consultaCompra.obtenerCompra(30L)).thenReturn(compra(30L, 5L));

        assertThat(fachada.compra(1L, 30L).id()).isEqualTo(30L);
    }

    @Test
    void unaCompraAjenaSeNiegaComoSiNoExistiera() {
        when(consultaCompra.obtenerCompra(30L)).thenReturn(compra(30L, 5L));
        doThrow(new RecursoNoEncontradoException("Tienda", 5L)).when(tiendaService).verificarPropiedad(2L, 5L);

        assertThatThrownBy(() -> fachada.compra(2L, 30L))
                .isInstanceOf(RecursoNoEncontradoException.class).hasMessageContaining("Compra");
    }

    @Test
    void noSeCompraParaUnaTiendaAjena() {
        doThrow(new RecursoNoEncontradoException("Tienda", 5L)).when(tiendaService).verificarPropiedad(2L, 5L);

        assertThatThrownBy(() -> fachada.registrarCompra(2L, solicitud()))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(compraService);
    }

    @Test
    void alComprarSeDevuelveElResumenDeLaCompraRegistrada() {
        Compra registrada = mock(Compra.class);
        when(registrada.getId()).thenReturn(30L);
        when(compraService.registrarCompra(solicitud())).thenReturn(registrada);
        when(consultaCompra.obtenerCompra(30L)).thenReturn(compra(30L, 5L));

        CompraResumen resumen = fachada.registrarCompra(1L, solicitud());

        assertThat(resumen.id()).isEqualTo(30L);
        assertThat(resumen.unidades()).isEqualTo(10);
    }

    @Test
    void paraComprarSeListanTambienLosProductosInactivos() {
        when(catalogo.listarProductos(5L, false)).thenReturn(List.of());

        assertThat(fachada.productosParaComprar(1L, 5L)).isEmpty();
    }

    @Test
    void losProveedoresSonCompartidosYNoPidenTienda() {
        ProveedorResumen proveedor = new ProveedorResumen(7L, "Textiles Gamarra SAC", "20123456789",
                "Luis Paredes", "ventas@textiles.pe", null);
        SolicitudProveedor solicitudProveedor = new SolicitudProveedor("Luis Paredes", "ventas@textiles.pe", null,
                "Textiles Gamarra SAC", "20123456789");
        when(proveedorService.listarProveedores()).thenReturn(List.of(proveedor));
        when(proveedorService.registrarProveedor(solicitudProveedor)).thenReturn(proveedor);

        assertThat(fachada.proveedores()).containsExactly(proveedor);
        assertThat(fachada.registrarProveedor(solicitudProveedor)).isEqualTo(proveedor);
        verifyNoInteractions(tiendaService);
    }
}
