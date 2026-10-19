package pe.mypeinventario.decorator;

import java.util.Objects;

/**
 * Decorador abstracto: envuelve otro {@link CalculoImporte} y le agrega un
 * concepto. Como es también un {@code CalculoImporte}, los decoradores se
 * pueden anidar en cualquier orden.
 */
public abstract class DecoradorImporte implements CalculoImporte {

    /** Escala de los importes: dos decimales. */
    protected static final int ESCALA = 2;

    /** Importe que este decorador modifica. */
    protected final CalculoImporte envuelto;

    protected DecoradorImporte(CalculoImporte envuelto) {
        this.envuelto = Objects.requireNonNull(envuelto, "El importe a decorar es obligatorio");
    }
}
