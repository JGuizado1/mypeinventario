package pe.mypeinventario.observer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Valida el sujeto del patrón Observer sin usar Spring: suscripción, notificación
 * a todos los observadores y aislamiento de los que fallan.
 */
class PublicadorAlertasTest {

    private static EventoAlerta evento() {
        return new EventoAlerta(1L, 10L, 100L, "POL-M-AZU", "Polo básico", "M", "Azul",
                2, 2, "Stock actual (2) igual o inferior al mínimo (2)", LocalDateTime.of(2026, 11, 20, 10, 0));
    }

    @Test
    void notificaATodosLosObservadoresSuscritos() {
        PublicadorAlertas publicador = new PublicadorAlertas();
        List<EventoAlerta> recibidosPorA = new ArrayList<>();
        List<EventoAlerta> recibidosPorB = new ArrayList<>();
        publicador.suscribir(recibidosPorA::add);
        publicador.suscribir(recibidosPorB::add);

        EventoAlerta evento = evento();
        publicador.notificar(evento);

        assertThat(recibidosPorA).containsExactly(evento);
        assertThat(recibidosPorB).containsExactly(evento);
    }

    @Test
    void unObservadorDesuscritoDejaDeRecibirEventos() {
        PublicadorAlertas publicador = new PublicadorAlertas();
        List<EventoAlerta> recibidos = new ArrayList<>();
        ObservadorAlerta observador = recibidos::add;
        publicador.suscribir(observador);
        publicador.notificar(evento());

        publicador.desuscribir(observador);
        publicador.notificar(evento());

        assertThat(recibidos).hasSize(1);
    }

    @Test
    void suscribirDosVecesElMismoObservadorNoDuplicaLasNotificaciones() {
        PublicadorAlertas publicador = new PublicadorAlertas();
        List<EventoAlerta> recibidos = new ArrayList<>();
        ObservadorAlerta observador = recibidos::add;
        publicador.suscribir(observador);
        publicador.suscribir(observador);

        publicador.notificar(evento());

        assertThat(recibidos).hasSize(1);
    }

    @Test
    void unObservadorQueFallaNoImpideNotificarALosDemas() {
        PublicadorAlertas publicador = new PublicadorAlertas();
        List<EventoAlerta> recibidos = new ArrayList<>();
        publicador.suscribir(e -> {
            throw new IllegalStateException("Falla simulada del observador");
        });
        publicador.suscribir(recibidos::add);

        publicador.notificar(evento());

        assertThat(recibidos).hasSize(1);
    }

    @Test
    void notificarSinObservadoresNoFalla() {
        PublicadorAlertas publicador = new PublicadorAlertas();

        publicador.notificar(evento());

        assertThat(publicador.cantidadObservadores()).isZero();
    }

    @Test
    void losDatosNulosSeRechazan() {
        PublicadorAlertas publicador = new PublicadorAlertas();

        assertThatThrownBy(() -> publicador.suscribir(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> publicador.notificar(null)).isInstanceOf(NullPointerException.class);
    }
}
