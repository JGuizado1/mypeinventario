package pe.mypeinventario.observer;

/** Sujeto del patrón Observer: mantiene a sus observadores y los notifica. */
public interface SujetoAlertas {

    /**
     * Agrega un observador. Si ya estaba suscrito, no se duplica.
     *
     * @param observador observador a suscribir
     */
    void suscribir(ObservadorAlerta observador);

    /**
     * Quita un observador; si no estaba suscrito, no ocurre nada.
     *
     * @param observador observador a quitar
     */
    void desuscribir(ObservadorAlerta observador);

    /**
     * Notifica el evento a todos los observadores suscritos.
     *
     * @param evento alerta generada
     */
    void notificar(EventoAlerta evento);
}
