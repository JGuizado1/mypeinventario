package pe.mypeinventario.observer;

import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Observador que simula los avisos al comerciante (por ejemplo, un mensaje de WhatsApp).
 * Guarda en memoria los últimos mensajes de cada tienda. Un envío real sería otro observador.
 */
public class BandejaNotificaciones implements ObservadorAlerta {

    private static final int MAXIMO_POR_TIENDA = 100;

    private final Map<Long, Deque<String>> mensajes = new ConcurrentHashMap<>();

    @Override
    public void alertaGenerada(EventoAlerta evento) {
        String unidades = evento.stockActual() == 1 ? " unidad" : " unidades";
        String texto = "Stock bajo: " + evento.sku() + " (" + evento.producto() + " "
                + Objects.toString(evento.talla(), "-") + "/" + Objects.toString(evento.color(), "-")
                + "): quedan " + evento.stockActual() + unidades
                + "; el mínimo es " + evento.stockMinimo() + ".";
        Deque<String> cola = mensajes.computeIfAbsent(evento.tiendaId(), id -> new ConcurrentLinkedDeque<>());
        cola.addLast(texto);
        while (cola.size() > MAXIMO_POR_TIENDA) {
            cola.pollFirst();
        }
    }

    /**
     * @param tiendaId tienda consultada
     * @return mensajes de esa tienda, del más antiguo al más reciente; vacío si no hay
     */
    public List<String> mensajesDe(Long tiendaId) {
        Deque<String> cola = mensajes.get(tiendaId);
        return cola == null ? List.of() : List.copyOf(cola);
    }

    /** Vacía la bandeja de todas las tiendas. */
    public void limpiar() {
        mensajes.clear();
    }
}