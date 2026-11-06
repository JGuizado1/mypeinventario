package pe.mypeinventario.observer;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Datos de una alerta de reabastecimiento recién generada. Los observadores reciben
 * este evento y no necesitan conocer las entidades del dominio.
 *
 * @param alertaId    identificador de la alerta
 * @param tiendaId    tienda a la que pertenece la variante
 * @param varianteId  variante que alcanzó el stock mínimo
 * @param sku         código de la variante
 * @param producto    nombre del producto
 * @param talla       talla (puede ser nula)
 * @param color       color (puede ser nulo)
 * @param stockActual unidades en stock al generar la alerta
 * @param stockMinimo stock mínimo vigente
 * @param mensaje     texto de la alerta
 * @param fecha       fecha y hora de generación
 */
public record EventoAlerta(Long alertaId, Long tiendaId, Long varianteId, String sku, String producto,
                           String talla, String color, int stockActual, int stockMinimo,
                           String mensaje, LocalDateTime fecha) {

    public EventoAlerta {
        Objects.requireNonNull(tiendaId, "La tienda es obligatoria");
        Objects.requireNonNull(sku, "El sku es obligatorio");
    }
}
