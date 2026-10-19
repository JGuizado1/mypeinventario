package pe.mypeinventario.factory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import pe.mypeinventario.domain.DetalleCompra;
import pe.mypeinventario.domain.DetalleVenta;
import pe.mypeinventario.domain.MovimientoInventario;
import pe.mypeinventario.domain.TipoMovimiento;
import pe.mypeinventario.domain.VarianteSKU;
import pe.mypeinventario.exception.OperacionInvalidaException;

/**
 * Valida las reglas de creación de cada tipo de movimiento. No usa Spring:
 * los creadores son clases simples, así que la prueba es inmediata.
 */
class CreadorMovimientoTest {

    private static final Clock RELOJ =
            Clock.fixed(Instant.parse("2026-10-10T15:00:00Z"), ZoneId.of("America/Lima"));
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 10, 10, 0);

    private DetalleVenta lineaVenta() {
        return new DetalleVenta(null, new VarianteSKU(), 2, new BigDecimal("29.90"));
    }

    private DetalleCompra lineaCompra() {
        return new DetalleCompra(null, new VarianteSKU(), 10, new BigDecimal("15.00"));
    }

    @Test
    void laCompraCreaUnMovimientoDeEntradaConSuLineaDeOrigen() {
        DetalleCompra linea = lineaCompra();
        CreadorMovimiento creador = new CreadorMovimientoCompra(RELOJ);

        MovimientoInventario movimiento = creador.crear(OrigenMovimiento.deCompra(linea), 10, null);

        assertThat(movimiento.getTipo()).isEqualTo(TipoMovimiento.COMPRA);
        assertThat(movimiento.calcularEfecto()).isEqualTo(10);
        assertThat(movimiento.getDetalleCompra()).isSameAs(linea);
        assertThat(movimiento.getDetalleVenta()).isNull();
        assertThat(movimiento.getFecha()).isEqualTo(AHORA);
        assertThat(movimiento.getMotivo()).isNotBlank();
    }

    @Test
    void laVentaCreaUnMovimientoDeSalida() {
        CreadorMovimiento creador = new CreadorMovimientoVenta(RELOJ);

        MovimientoInventario movimiento = creador.crear(OrigenMovimiento.deVenta(lineaVenta()), 2, null);

        assertThat(movimiento.getTipo()).isEqualTo(TipoMovimiento.VENTA);
        assertThat(movimiento.calcularEfecto()).isEqualTo(-2);
    }

    @Test
    void laDevolucionCreaUnMovimientoDeEntradaDesdeLaVenta() {
        CreadorMovimiento creador = new CreadorMovimientoDevolucion(RELOJ);

        MovimientoInventario movimiento = creador.crear(OrigenMovimiento.deVenta(lineaVenta()), 1, "Talla incorrecta");

        assertThat(movimiento.getTipo()).isEqualTo(TipoMovimiento.DEVOLUCION_CLIENTE);
        assertThat(movimiento.calcularEfecto()).isEqualTo(1);
        assertThat(movimiento.getMotivo()).isEqualTo("Talla incorrecta");
    }

    @Test
    void elAjusteUsaElTipoConElQueSeConfigura() {
        MovimientoInventario positivo = new CreadorMovimientoAjuste(RELOJ, TipoMovimiento.AJUSTE_POSITIVO)
                .crear(OrigenMovimiento.ninguno(), 3, "Conteo físico");
        MovimientoInventario negativo = new CreadorMovimientoAjuste(RELOJ, TipoMovimiento.AJUSTE_NEGATIVO)
                .crear(OrigenMovimiento.ninguno(), 3, "Prenda dañada");

        assertThat(positivo.calcularEfecto()).isEqualTo(3);
        assertThat(negativo.calcularEfecto()).isEqualTo(-3);
    }

    @Test
    void cadaCreadorExigeElOrigenQueLeCorresponde() {
        assertThatThrownBy(() -> new CreadorMovimientoCompra(RELOJ)
                .crear(OrigenMovimiento.deVenta(lineaVenta()), 1, null))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> new CreadorMovimientoVenta(RELOJ)
                .crear(OrigenMovimiento.deCompra(lineaCompra()), 1, null))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> new CreadorMovimientoDevolucion(RELOJ)
                .crear(OrigenMovimiento.ninguno(), 1, null))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> new CreadorMovimientoVenta(RELOJ).crear(null, 1, null))
                .isInstanceOf(OperacionInvalidaException.class);
    }

    @Test
    void laCantidadNoPuedeSerCeroNiSuperarLaDeLaLineaDeOrigen() {
        CreadorMovimiento creador = new CreadorMovimientoVenta(RELOJ);

        assertThatThrownBy(() -> creador.crear(OrigenMovimiento.deVenta(lineaVenta()), 3, null))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> creador.crear(OrigenMovimiento.deVenta(lineaVenta()), 0, null))
                .isInstanceOf(OperacionInvalidaException.class);
    }

    @Test
    void elAjusteExigeMotivoYNoAdmiteOrigen() {
        CreadorMovimiento creador = new CreadorMovimientoAjuste(RELOJ, TipoMovimiento.AJUSTE_NEGATIVO);

        assertThatThrownBy(() -> creador.crear(OrigenMovimiento.ninguno(), 1, "   "))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> creador.crear(OrigenMovimiento.ninguno(), 1, null))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> creador.crear(OrigenMovimiento.deVenta(lineaVenta()), 1, "Con origen"))
                .isInstanceOf(OperacionInvalidaException.class);
    }

    @Test
    void elAjusteSoloAceptaTiposDeAjuste() {
        assertThatThrownBy(() -> new CreadorMovimientoAjuste(RELOJ, TipoMovimiento.COMPRA))
                .isInstanceOf(OperacionInvalidaException.class);
    }

    @Test
    void elOrigenNoPuedeSerAlMismoTiempoVentaYCompra() {
        assertThatThrownBy(() -> new OrigenMovimiento(lineaVenta(), lineaCompra()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
