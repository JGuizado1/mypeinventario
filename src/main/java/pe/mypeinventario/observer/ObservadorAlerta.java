package pe.mypeinventario.observer;

/** Observador del patrón Observer: reacciona cuando se genera una alerta de reabastecimiento. */
@FunctionalInterface
public interface ObservadorAlerta {

    /**
     * Se invoca una vez por cada alerta generada, después de confirmar la transacción.
     *
     * @param evento datos de la alerta
     */
    void alertaGenerada(EventoAlerta evento);
}
