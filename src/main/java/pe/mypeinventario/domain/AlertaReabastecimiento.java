package pe.mypeinventario.domain;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Aviso de que una {@link VarianteSKU} alcanzó o bajó de su stock mínimo.
 * Tiene un ciclo de vida: PENDIENTE, VISTA y ATENDIDA.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlertaReabastecimiento {

    private Long id;
    private LocalDateTime fechaGeneracion;
    private int stockActual;
    private int stockMinimo;
    private String mensaje;
    private EstadoAlerta estado;

    /**
     * Genera una alerta pendiente con la fecha actual.
     *
     * @param id          identificador (puede ser {@code null} hasta que se persista)
     * @param stockActual stock al momento de generar la alerta
     * @param stockMinimo stock mínimo vigente de la variante
     * @throws IllegalArgumentException si alguno de los valores es negativo
     */
    public AlertaReabastecimiento(Long id, int stockActual, int stockMinimo) {
        if (stockActual < 0 || stockMinimo < 0) {
            throw new IllegalArgumentException("El stock actual y el mínimo no pueden ser negativos");
        }
        this.id = id;
        this.stockActual = stockActual;
        this.stockMinimo = stockMinimo;
        this.fechaGeneracion = LocalDateTime.now();
        this.estado = EstadoAlerta.PENDIENTE;
        this.mensaje = "Stock actual (" + stockActual + ") igual o inferior al mínimo (" + stockMinimo + ")";
    }

    /**
     * Marca la alerta como vista por el comerciante.
     *
     * @throws IllegalStateException si la alerta no estaba pendiente
     */
    public void marcarVista() {
        if (estado != EstadoAlerta.PENDIENTE) {
            throw new IllegalStateException("Solo una alerta pendiente puede marcarse como vista");
        }
        estado = EstadoAlerta.VISTA;
    }

    /**
     * Marca la alerta como atendida (ya se reabasteció).
     *
     * @throws IllegalStateException si la alerta ya estaba atendida
     */
    public void marcarAtendida() {
        if (estado == EstadoAlerta.ATENDIDA) {
            throw new IllegalStateException("La alerta ya está atendida");
        }
        estado = EstadoAlerta.ATENDIDA;
    }
}
