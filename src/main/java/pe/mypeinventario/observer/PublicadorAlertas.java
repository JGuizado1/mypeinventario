package pe.mypeinventario.observer;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sujeto concreto del patrón Observer. Notifica a cada observador de forma aislada:
 * si uno falla, los demás reciben igualmente el evento.
 */
public class PublicadorAlertas implements SujetoAlertas {

    private static final Logger LOG = LoggerFactory.getLogger(PublicadorAlertas.class);

    private final CopyOnWriteArrayList<ObservadorAlerta> observadores = new CopyOnWriteArrayList<>();

    @Override
    public void suscribir(ObservadorAlerta observador) {
        observadores.addIfAbsent(Objects.requireNonNull(observador, "El observador es obligatorio"));
    }

    @Override
    public void desuscribir(ObservadorAlerta observador) {
        observadores.remove(observador);
    }

    @Override
    public void notificar(EventoAlerta evento) {
        Objects.requireNonNull(evento, "El evento es obligatorio");
        for (ObservadorAlerta observador : observadores) {
            try {
                observador.alertaGenerada(evento);
            } catch (RuntimeException e) {
                // Un observador defectuoso no debe impedir que los demás se enteren.
                LOG.error("El observador {} falló al procesar la alerta {}",
                        observador.getClass().getSimpleName(), evento.alertaId(), e);
            }
        }
    }

    /** @return cantidad de observadores suscritos */
    public int cantidadObservadores() {
        return observadores.size();
    }
}
