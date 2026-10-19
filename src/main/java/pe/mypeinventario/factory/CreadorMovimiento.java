package pe.mypeinventario.factory;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import pe.mypeinventario.domain.MovimientoInventario;
import pe.mypeinventario.domain.TipoMovimiento;
import pe.mypeinventario.exception.OperacionInvalidaException;

/**
 * Creador del patrón Factory Method. Cada subclase decide qué tipo de movimiento
 * construye y qué reglas debe cumplir. Quien lo usa trabaja con esta abstracción
 * y no conoce las clases concretas.
 */
public abstract class CreadorMovimiento {
    private final Clock reloj;

    /**
     * @param reloj fuente de la fecha del movimiento; recibirla permite probar con una fecha fija
     */
    protected CreadorMovimiento(Clock reloj) {
        this.reloj = Objects.requireNonNull(reloj, "El reloj es obligatorio");
    }

    /**
     * Método de fábrica: crea un movimiento validando las reglas de su tipo.
     *
     * @param origen   línea de venta o de compra que lo origina
     * @param cantidad unidades del movimiento, mayores que cero
     * @param motivo   descripción libre; algunos tipos la exigen
     * @return el movimiento creado, todavía sin persistir
     * @throws OperacionInvalidaException si no se cumplen las reglas del tipo
     */
    public abstract MovimientoInventario crear(OrigenMovimiento origen, int cantidad, String motivo);

    /** Construye el movimiento con la fecha actual del reloj. */
    protected MovimientoInventario construir(TipoMovimiento tipo, int cantidad, String motivo,
                                             OrigenMovimiento origen) {
        return new MovimientoInventario(null, tipo, cantidad, LocalDateTime.now(reloj), motivo,
                origen.detalleVenta(), origen.detalleCompra());
    }

    /** Exige una cantidad positiva que no supere la de la línea de origen. */
    protected static void validarCantidad(int cantidad, int maximo) {
        if (cantidad <= 0) {
            throw new OperacionInvalidaException("La cantidad debe ser mayor que cero");
        }
        if (cantidad > maximo) {
            throw new OperacionInvalidaException("La cantidad (" + cantidad
                    + ") no puede superar la de la línea de origen (" + maximo + ")");
        }
    }

    /** Devuelve el motivo recibido o, si está vacío, uno por defecto. */
    protected static String motivoOPorDefecto(String motivo, String porDefecto) {
        return (motivo == null || motivo.isBlank()) ? porDefecto : motivo.strip();
    }
}
