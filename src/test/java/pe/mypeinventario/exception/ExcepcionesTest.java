package pe.mypeinventario.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Las excepciones del negocio llevan el dato que explica el error. */
class ExcepcionesTest {

    @Test
    void elStockInsuficienteGuardaSkuDisponibleYSolicitado() {
        StockInsuficienteException e = new StockInsuficienteException("POL-M-AZU", 2, 5);

        assertThat(e.getSku()).isEqualTo("POL-M-AZU");
        assertThat(e.getDisponible()).isEqualTo(2);
        assertThat(e.getSolicitado()).isEqualTo(5);
        assertThat(e).hasMessage("Stock insuficiente para POL-M-AZU: disponible 2, solicitado 5")
                .isInstanceOf(MypeInventarioException.class);
    }

    @Test
    void elConflictoDeConcurrenciaConservaLaCausa() {
        RuntimeException causa = new RuntimeException("versión distinta");

        ConflictoConcurrenciaException e = new ConflictoConcurrenciaException("Otro usuario modificó el dato", causa);

        assertThat(e).hasMessage("Otro usuario modificó el dato").hasCause(causa)
                .isInstanceOf(MypeInventarioException.class);
    }
}
