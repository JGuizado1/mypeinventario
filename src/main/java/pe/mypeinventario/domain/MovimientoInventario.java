package pe.mypeinventario.domain;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Registro histórico de una entrada o salida de stock de una {@link VarianteSKU}.
 * Es inmutable: una vez creado no se edita ni se elimina, de modo que el
 * historial sea una traza confiable. Para corregir un error se registra un
 * nuevo movimiento de ajuste.
 */
@Getter
// JPA (Etapa 2): el constructor protegido sin argumentos será el que use Hibernate.
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MovimientoInventario {

    private Long id;
    private TipoMovimiento tipo;
    /** Siempre positiva; el signo lo aporta {@link TipoMovimiento#getSigno()}. */
    private int cantidad;
    private LocalDateTime fecha;
    private String motivo;
    /** Línea de venta que originó el movimiento (puede ser {@code null}). */
    private DetalleVenta detalleVenta;
    /** Línea de compra que originó el movimiento (puede ser {@code null}). */
    private DetalleCompra detalleCompra;

    /**
     * Crea un movimiento de inventario.
     *
     * @param id            identificador (puede ser {@code null} hasta que se persista)
     * @param tipo          tipo de movimiento
     * @param cantidad      unidades, mayor que cero
     * @param fecha         fecha y hora del movimiento
     * @param motivo        descripción libre (opcional)
     * @param detalleVenta  línea de venta de origen, o {@code null}
     * @param detalleCompra línea de compra de origen, o {@code null}
     * @throws IllegalArgumentException si falta el tipo o la fecha, si la cantidad no es
     *                                  positiva o si se indican a la vez una venta y una compra de origen
     */
    public MovimientoInventario(Long id, TipoMovimiento tipo, int cantidad, LocalDateTime fecha,
                                String motivo, DetalleVenta detalleVenta, DetalleCompra detalleCompra) {
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de movimiento es obligatorio");
        }
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        }
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha es obligatoria");
        }
        if (detalleVenta != null && detalleCompra != null) {
            throw new IllegalArgumentException(
                    "Un movimiento no puede originarse en una venta y una compra a la vez");
        }
        this.id = id;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.fecha = fecha;
        this.motivo = motivo;
        this.detalleVenta = detalleVenta;
        this.detalleCompra = detalleCompra;
    }

    /**
     * Calcula cuánto modifica este movimiento al stock.
     *
     * @return la cantidad con signo (positiva si suma, negativa si resta)
     */
    public int calcularEfecto() {
        return tipo.getSigno() * cantidad;
    }
}
