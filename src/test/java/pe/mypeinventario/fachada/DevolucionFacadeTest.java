package pe.mypeinventario.fachada;

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
import pe.mypeinventario.consulta.VentaResumen;
import pe.mypeinventario.domain.MedioPago;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.service.ConsultaVentaService;
import pe.mypeinventario.service.DevolucionService;
import pe.mypeinventario.service.TiendaService;

/** Prueba unitaria con mocks: nadie repone stock con una venta que no es de su tienda. */
@ExtendWith(MockitoExtension.class)
class DevolucionFacadeTest {

    @Mock private DevolucionService devolucionService;
    @Mock private ConsultaVentaService consultaVenta;
    @Mock private TiendaService tiendaService;
    @InjectMocks private DevolucionFacade fachada;

    private static VentaResumen venta() {
        return new VentaResumen(20L, 5L, LocalDateTime.of(2026, 11, 6, 19, 30), null, MedioPago.EFECTIVO,
                new BigDecimal("29.90"), BigDecimal.ZERO, new BigDecimal("29.90"), new BigDecimal("4.56"), List.of());
    }

    @Test
    void unaDevolucionDeUnaVentaPropiaSeRegistra() {
        when(consultaVenta.obtenerVenta(20L)).thenReturn(venta());

        fachada.devolver(1L, 20L, 7L, 2, "Talla equivocada");

        verify(devolucionService).registrarDevolucion(20L, 7L, 2, "Talla equivocada");
    }

    @Test
    void unaVentaAjenaSeNiegaComoSiNoExistiera() {
        when(consultaVenta.obtenerVenta(20L)).thenReturn(venta());
        doThrow(new RecursoNoEncontradoException("Tienda", 5L)).when(tiendaService).verificarPropiedad(2L, 5L);

        assertThatThrownBy(() -> fachada.devolver(2L, 20L, 7L, 2, "x"))
                .isInstanceOf(RecursoNoEncontradoException.class).hasMessageContaining("Venta");
        verifyNoInteractions(devolucionService);
    }
}
