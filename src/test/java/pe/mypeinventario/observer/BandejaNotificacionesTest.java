package pe.mypeinventario.observer;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** Valida la bandeja que simula los mensajes que recibiría el comerciante. */
class BandejaNotificacionesTest {

    private static EventoAlerta evento(Long tiendaId, String sku, int stockActual) {
        return new EventoAlerta(1L, tiendaId, 100L, sku, "Polo básico", "M", "Azul",
                stockActual, 2, "mensaje", LocalDateTime.of(2026, 11, 20, 10, 0));
    }

    @Test
    void guardaUnMensajeConElSkuElStockYElMinimo() {
        BandejaNotificaciones bandeja = new BandejaNotificaciones();

        bandeja.alertaGenerada(evento(10L, "POL-M-AZU", 1));

        assertThat(bandeja.mensajesDe(10L)).hasSize(1);
        assertThat(bandeja.mensajesDe(10L).get(0))
                .contains("POL-M-AZU").contains("Polo básico").contains("quedan 1").contains("mínimo es 2");
    }

    @Test
    void separaLosMensajesPorTienda() {
        BandejaNotificaciones bandeja = new BandejaNotificaciones();

        bandeja.alertaGenerada(evento(10L, "POL-M-AZU", 1));
        bandeja.alertaGenerada(evento(20L, "CAM-L-ROJ", 0));

        assertThat(bandeja.mensajesDe(10L)).hasSize(1);
        assertThat(bandeja.mensajesDe(20L)).hasSize(1);
        assertThat(bandeja.mensajesDe(30L)).isEmpty();
    }

    @Test
    void conservaSoloLosMensajesMasRecientesDeCadaTienda() {
        BandejaNotificaciones bandeja = new BandejaNotificaciones();

        for (int i = 0; i < 105; i++) {
            bandeja.alertaGenerada(evento(10L, "SKU-" + i, 1));
        }

        assertThat(bandeja.mensajesDe(10L)).hasSize(100);
        assertThat(bandeja.mensajesDe(10L).get(0)).contains("SKU-5");
        assertThat(bandeja.mensajesDe(10L).get(99)).contains("SKU-104");
    }

    @Test
    void limpiarVaciaLaBandeja() {
        BandejaNotificaciones bandeja = new BandejaNotificaciones();
        bandeja.alertaGenerada(evento(10L, "POL-M-AZU", 1));

        bandeja.limpiar();

        assertThat(bandeja.mensajesDe(10L)).isEmpty();
    }

    @Test
    void unaTallaOColorAusenteNoRompeElMensaje() {
        BandejaNotificaciones bandeja = new BandejaNotificaciones();

        bandeja.alertaGenerada(new EventoAlerta(1L, 10L, 100L, "BOL-UNI", "Bolso", null, null,
                1, 2, "mensaje", LocalDateTime.of(2026, 11, 20, 10, 0)));

        assertThat(bandeja.mensajesDe(10L).get(0)).contains("BOL-UNI").doesNotContain("null");
    }
}
