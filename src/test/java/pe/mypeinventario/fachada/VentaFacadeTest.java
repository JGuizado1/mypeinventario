package pe.mypeinventario.fachada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.mypeinventario.consulta.ClienteResumen;
import pe.mypeinventario.consulta.LineaVentaResumen;
import pe.mypeinventario.consulta.ProductoResumen;
import pe.mypeinventario.consulta.ResultadoVenta;
import pe.mypeinventario.consulta.VarianteResumen;
import pe.mypeinventario.consulta.VentaResumen;
import pe.mypeinventario.domain.MedioPago;
import pe.mypeinventario.domain.VentaPunto;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.service.ClienteService;
import pe.mypeinventario.service.ConsultaCatalogoService;
import pe.mypeinventario.service.ConsultaVentaService;
import pe.mypeinventario.service.TiendaService;
import pe.mypeinventario.service.VentaService;
import pe.mypeinventario.service.dto.LineaVenta;
import pe.mypeinventario.service.dto.SolicitudCliente;
import pe.mypeinventario.service.dto.SolicitudVenta;

/** Prueba unitaria con mocks: nadie vende en una tienda ajena ni ve ventas de otro comerciante. */
@ExtendWith(MockitoExtension.class)
class VentaFacadeTest {

    @Mock private VentaService ventaService;
    @Mock private ConsultaVentaService consultaVenta;
    @Mock private ConsultaCatalogoService catalogo;
    @Mock private ClienteService clienteService;
    @Mock private TiendaService tiendaService;
    @InjectMocks private VentaFacade fachada;

    private static VentaResumen venta(Long id, Long tiendaId, Long varianteId) {
        LineaVentaResumen linea = new LineaVentaResumen(1L, varianteId, "POL-M-NEG", "Polo básico", "M", "Negro",
                8, new BigDecimal("29.90"), new BigDecimal("239.20"));
        return new VentaResumen(id, tiendaId, LocalDateTime.of(2026, 11, 6, 19, 30), null, MedioPago.YAPE,
                new BigDecimal("239.20"), BigDecimal.ZERO, new BigDecimal("239.20"), new BigDecimal("36.49"),
                List.of(linea));
    }

    private static VarianteResumen variante(Long id, String sku, int stock, boolean stockBajo) {
        return new VarianteResumen(id, sku, null, "M", "Negro", null, new BigDecimal("29.90"), 5, stock, stockBajo);
    }

    private static ProductoResumen producto(VarianteResumen... variantes) {
        return new ProductoResumen(10L, 5L, "Polo básico", null, "Polos", null, true, List.of(variantes));
    }

    private static SolicitudVenta solicitud() {
        return new SolicitudVenta(5L, null, MedioPago.YAPE, List.of(new LineaVenta(100L, 8)), null, null);
    }

    @Test
    void lasVentasDeUnaTiendaAjenaSeNiegan() {
        doThrow(new RecursoNoEncontradoException("Tienda", 5L)).when(tiendaService).verificarPropiedad(2L, 5L);

        assertThatThrownBy(() -> fachada.ventas(2L, 5L)).isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(consultaVenta);
    }

    @Test
    void unaVentaPropiaSeDevuelve() {
        when(consultaVenta.obtenerVenta(20L)).thenReturn(venta(20L, 5L, 100L));

        assertThat(fachada.venta(1L, 20L).id()).isEqualTo(20L);
    }

    @Test
    void unaVentaAjenaSeNiegaComoSiNoExistiera() {
        when(consultaVenta.obtenerVenta(20L)).thenReturn(venta(20L, 5L, 100L));
        doThrow(new RecursoNoEncontradoException("Tienda", 5L)).when(tiendaService).verificarPropiedad(2L, 5L);

        assertThatThrownBy(() -> fachada.venta(2L, 20L))
                .isInstanceOf(RecursoNoEncontradoException.class).hasMessageContaining("Venta");
    }

    @Test
    void noSePuedeVenderEnUnaTiendaAjena() {
        doThrow(new RecursoNoEncontradoException("Tienda", 5L)).when(tiendaService).verificarPropiedad(2L, 5L);

        assertThatThrownBy(() -> fachada.registrarVenta(2L, solicitud()))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(ventaService);
    }

    @Test
    void alVenderSeAvisaQueVariantesQuedaronConPocoStock() {
        VentaPunto registrada = mock(VentaPunto.class);
        when(registrada.getId()).thenReturn(20L);
        when(ventaService.registrarVenta(solicitud())).thenReturn(registrada);
        when(consultaVenta.obtenerVenta(20L)).thenReturn(venta(20L, 5L, 100L));
        // La variante 101 también tiene poco stock, pero no se vendió: no debe avisarse.
        when(catalogo.listarProductos(5L, true)).thenReturn(List.of(
                producto(variante(100L, "POL-M-NEG", 2, true), variante(101L, "POL-L-NEG", 1, true))));

        ResultadoVenta resultado = fachada.registrarVenta(1L, solicitud());

        assertThat(resultado.venta().id()).isEqualTo(20L);
        assertThat(resultado.avisos()).containsExactly("Polo básico (POL-M-NEG): quedan 2, el mínimo es 5");
        InOrder orden = inOrder(tiendaService, ventaService);
        orden.verify(tiendaService).verificarPropiedad(1L, 5L);
        orden.verify(ventaService).registrarVenta(solicitud());
    }

    @Test
    void sinPocoStockNoHayAvisos() {
        VentaPunto registrada = mock(VentaPunto.class);
        when(registrada.getId()).thenReturn(20L);
        when(ventaService.registrarVenta(solicitud())).thenReturn(registrada);
        when(consultaVenta.obtenerVenta(20L)).thenReturn(venta(20L, 5L, 100L));
        when(catalogo.listarProductos(5L, true)).thenReturn(List.of(producto(variante(100L, "POL-M-NEG", 30, false))));

        assertThat(fachada.registrarVenta(1L, solicitud()).avisos()).isEmpty();
    }

    @Test
    void losClientesSoloSeVenYSeCreanEnTiendasPropias() {
        ClienteResumen cliente = new ClienteResumen(3L, "Rosa Quispe", null, null);
        SolicitudCliente solicitudCliente = new SolicitudCliente(5L, "Rosa Quispe", null, null);
        when(clienteService.listarClientes(5L)).thenReturn(List.of(cliente));
        when(clienteService.registrarCliente(solicitudCliente)).thenReturn(cliente);

        assertThat(fachada.clientes(1L, 5L)).containsExactly(cliente);
        assertThat(fachada.registrarCliente(1L, solicitudCliente)).isEqualTo(cliente);
    }

    @Test
    void noSeRegistraUnClienteEnUnaTiendaAjena() {
        doThrow(new RecursoNoEncontradoException("Tienda", 5L)).when(tiendaService).verificarPropiedad(2L, 5L);

        assertThatThrownBy(() -> fachada.registrarCliente(2L, new SolicitudCliente(5L, "Rosa", null, null)))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(clienteService);
    }

    @Test
    void losProductosParaVenderSonLosActivosDeLaTienda() {
        when(catalogo.listarProductos(5L, true)).thenReturn(List.of(producto(variante(100L, "POL-M-NEG", 3, true))));

        assertThat(fachada.productosParaVender(1L, 5L)).hasSize(1);
    }
}
