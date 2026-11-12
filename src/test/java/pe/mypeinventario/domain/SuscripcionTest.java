package pe.mypeinventario.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Reglas de la suscripción: fechas coherentes y ciclo prueba → activa → cancelada. */
class SuscripcionTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 10, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 10, 31);
    private final PlanSuscripcion plan = new PlanSuscripcion(null, "Basico", new BigDecimal("20.00"), 100);

    private Suscripcion nueva() {
        return new Suscripcion(null, plan, INICIO, FIN);
    }

    @Test
    void empiezaEnPruebaYEstaVigenteSoloDentroDeSusFechas() {
        Suscripcion suscripcion = nueva();

        assertThat(suscripcion.getEstado()).isEqualTo(EstadoSuscripcion.PRUEBA);
        assertThat(suscripcion.estaVigente(INICIO)).isTrue();
        assertThat(suscripcion.estaVigente(FIN)).isTrue();
        assertThat(suscripcion.estaVigente(INICIO.minusDays(1))).isFalse();
        assertThat(suscripcion.estaVigente(FIN.plusDays(1))).isFalse();
    }

    @Test
    void activarSoloSirveDesdePruebaYCancelarDejaDeEstarVigente() {
        Suscripcion suscripcion = nueva();

        suscripcion.activar();
        assertThat(suscripcion.getEstado()).isEqualTo(EstadoSuscripcion.ACTIVA);
        assertThat(suscripcion.estaVigente(INICIO)).isTrue();
        assertThatThrownBy(suscripcion::activar).isInstanceOf(IllegalStateException.class);

        suscripcion.cancelar();
        assertThat(suscripcion.estaVigente(INICIO)).isFalse();
        assertThatThrownBy(suscripcion::cancelar).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void sinPlanOFechasOConFinAnteriorAlInicioSeRechaza() {
        assertThatThrownBy(() -> new Suscripcion(null, null, INICIO, FIN)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Suscripcion(null, plan, null, FIN)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Suscripcion(null, plan, INICIO, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Suscripcion(null, plan, FIN, INICIO)).isInstanceOf(IllegalArgumentException.class);
    }
}
