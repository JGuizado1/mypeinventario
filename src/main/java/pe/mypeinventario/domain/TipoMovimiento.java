package pe.mypeinventario.domain;

import java.util.Arrays;
import java.util.List;

/**
 * Tipos de movimiento de inventario. Cada tipo define si suma (+1) o resta (-1)
 * al stock, de modo que la cantidad del movimiento se guarda siempre positiva.
 */
public enum TipoMovimiento {
    COMPRA(1),
    VENTA(-1),
    DEVOLUCION_CLIENTE(1),
    AJUSTE_POSITIVO(1),
    AJUSTE_NEGATIVO(-1);

    private final int signo;

    TipoMovimiento(int signo) {
        this.signo = signo;
    }

    /** @return +1 si el movimiento aumenta el stock, -1 si lo disminuye. */
    public int getSigno() {
        return signo;
    }

    /**
     * Devuelve los tipos de movimiento que aumentan el stock. Permite que las
     * consultas calculen el stock sin repetir qué tipos suman.
     *
     * @return tipos con signo positivo
     */
    public static List<TipoMovimiento> entradas() {
        return Arrays.stream(values()).filter(tipo -> tipo.signo > 0).toList();
    }
}
