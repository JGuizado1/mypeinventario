package pe.mypeinventario.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.mypeinventario.domain.MovimientoInventario;
import pe.mypeinventario.domain.TipoMovimiento;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.exception.StockInsuficienteException;
import pe.mypeinventario.factory.CreadorMovimientoAjuste;
import pe.mypeinventario.service.InventarioService;

/**
 * Prueba unitaria de {@link AjusteInventarioServiceImpl} con Mockito: el servicio de
 * inventario se simula, así que no hay Spring ni base de datos. Se comprueba qué
 * movimiento le envía el service y que no lo invoque cuando los datos son inválidos.
 */
@ExtendWith(MockitoExtension.class)
class AjusteInventarioServiceImplTest {

    private static final Clock RELOJ =
            Clock.fixed(Instant.parse("2026-10-10T15:00:00Z"), ZoneId.of("America/Lima"));
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 10, 10, 0);

    @Mock
    private InventarioService inventarioService;

    private AjusteInventarioServiceImpl servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new AjusteInventarioServiceImpl(inventarioService,
                new CreadorMovimientoAjuste(RELOJ, TipoMovimiento.AJUSTE_POSITIVO),
                new CreadorMovimientoAjuste(RELOJ, TipoMovimiento.AJUSTE_NEGATIVO));
    }

    @Test
    void unAjustePositivoSeEnviaAlInventarioComoEntradaConSuMotivoYFecha() {
        when(inventarioService.registrarMovimiento(eq(7L), any(MovimientoInventario.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(1));

        MovimientoInventario devuelto = servicio.registrarAjuste(7L, TipoMovimiento.AJUSTE_POSITIVO, 3, "Conteo físico");

        ArgumentCaptor<MovimientoInventario> captura = ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(inventarioService).registrarMovimiento(eq(7L), captura.capture());
        MovimientoInventario enviado = captura.getValue();
        assertThat(enviado.getTipo()).isEqualTo(TipoMovimiento.AJUSTE_POSITIVO);
        assertThat(enviado.getCantidad()).isEqualTo(3);
        assertThat(enviado.getMotivo()).isEqualTo("Conteo físico");
        assertThat(enviado.getFecha()).isEqualTo(AHORA);
        assertThat(enviado.getDetalleVenta()).isNull();
        assertThat(enviado.getDetalleCompra()).isNull();
        assertThat(devuelto).isSameAs(enviado);
    }

    @Test
    void unAjusteNegativoSeEnviaAlInventarioComoSalida() {
        when(inventarioService.registrarMovimiento(eq(7L), any(MovimientoInventario.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(1));

        servicio.registrarAjuste(7L, TipoMovimiento.AJUSTE_NEGATIVO, 2, "Prenda dañada");

        ArgumentCaptor<MovimientoInventario> captura = ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(inventarioService).registrarMovimiento(eq(7L), captura.capture());
        assertThat(captura.getValue().getTipo()).isEqualTo(TipoMovimiento.AJUSTE_NEGATIVO);
        assertThat(captura.getValue().calcularEfecto()).isEqualTo(-2);
    }

    @Test
    void unTipoQueNoEsAjusteSeRechazaSinTocarElInventario() {
        assertThatThrownBy(() -> servicio.registrarAjuste(7L, TipoMovimiento.COMPRA, 1, "Intento de compra"))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> servicio.registrarAjuste(7L, null, 1, "Sin tipo"))
                .isInstanceOf(OperacionInvalidaException.class);

        verifyNoInteractions(inventarioService);
    }

    @Test
    void unAjusteSinMotivoSeRechazaSinTocarElInventario() {
        assertThatThrownBy(() -> servicio.registrarAjuste(7L, TipoMovimiento.AJUSTE_POSITIVO, 1, "   "))
                .isInstanceOf(OperacionInvalidaException.class);

        verifyNoInteractions(inventarioService);
    }

    @Test
    void unaCantidadNoPositivaSeRechazaSinTocarElInventario() {
        assertThatThrownBy(() -> servicio.registrarAjuste(7L, TipoMovimiento.AJUSTE_NEGATIVO, 0, "Conteo"))
                .isInstanceOf(OperacionInvalidaException.class);

        verifyNoInteractions(inventarioService);
    }

    @Test
    void lasExcepcionesDelInventarioSePropagan() {
        when(inventarioService.registrarMovimiento(eq(7L), any(MovimientoInventario.class)))
                .thenThrow(new StockInsuficienteException("POL-M-AZU", 1, 5));

        assertThatThrownBy(() -> servicio.registrarAjuste(7L, TipoMovimiento.AJUSTE_NEGATIVO, 5, "Pérdida"))
                .isInstanceOf(StockInsuficienteException.class);
    }
}
