package pe.mypeinventario.consulta;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import pe.mypeinventario.domain.EstadoAlerta;

/**
 * Vista de solo lectura de una alerta de reabastecimiento, con el producto y la variante a
 * los que se refiere.
 *
 * @param id          identificador de la alerta
 * @param tiendaId    tienda dueña del producto
 * @param varianteId  variante que llegó a su stock mínimo
 * @param sku         código interno de la variante
 * @param producto    nombre del producto
 * @param talla       talla, o {@code null}
 * @param color       color, o {@code null}
 * @param fecha       cuándo se generó la alerta
 * @param stockActual unidades que había al generarse
 * @param stockMinimo mínimo vigente al generarse
 * @param estado      pendiente, vista o atendida
 * @param mensaje     texto de la alerta
 */
public record AlertaResumen(Long id, Long tiendaId, Long varianteId, String sku, String producto, String talla,
                            String color, LocalDateTime fecha, int stockActual, int stockMinimo,
                            EstadoAlerta estado, String mensaje) {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** @return la variante en una frase corta, por ejemplo «M / Azul» */
    public String variante() {
        return TextoVariante.de(talla, color);
    }

    /** @return la fecha como «06/11/2026 19:30» */
    public String fechaTexto() {
        return FORMATO_FECHA.format(fecha);
    }

    /** @return el estado con la inicial en mayúscula, por ejemplo «Pendiente» */
    public String estadoTexto() {
        String nombre = estado.name();
        return nombre.charAt(0) + nombre.substring(1).toLowerCase();
    }

    /** @return {@code true} si todavía no se vio */
    public boolean pendiente() {
        return estado == EstadoAlerta.PENDIENTE;
    }

    /** @return {@code true} si aún no se atendió (está pendiente o vista) */
    public boolean abierta() {
        return estado != EstadoAlerta.ATENDIDA;
    }
}
