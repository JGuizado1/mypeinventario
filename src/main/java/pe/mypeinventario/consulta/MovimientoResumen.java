package pe.mypeinventario.consulta;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import pe.mypeinventario.domain.TipoMovimiento;

/**
 * Vista de solo lectura de un movimiento del historial de una variante.
 *
 * @param id       identificador
 * @param tipo     tipo de movimiento
 * @param cantidad unidades del movimiento (siempre positivas)
 * @param efecto   unidades que suma (positivo) o resta (negativo) al stock
 * @param fecha    fecha y hora del movimiento
 * @param motivo   explicación, o {@code null}
 */
public record MovimientoResumen(Long id, TipoMovimiento tipo, int cantidad, int efecto, LocalDateTime fecha,
                                String motivo) {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** @return la fecha como «06/11/2026 19:30» */
    public String fechaTexto() {
        return FORMATO_FECHA.format(fecha);
    }

    /** @return el tipo en palabras, por ejemplo «Devolución de cliente» */
    public String tipoTexto() {
        return switch (tipo) {
            case COMPRA -> "Compra";
            case VENTA -> "Venta";
            case DEVOLUCION_CLIENTE -> "Devolución de cliente";
            case AJUSTE_POSITIVO -> "Ajuste (suma)";
            case AJUSTE_NEGATIVO -> "Ajuste (resta)";
        };
    }

    /** @return el efecto con su signo, por ejemplo «+8» o «-3» */
    public String efectoTexto() {
        return efecto > 0 ? "+" + efecto : String.valueOf(efecto);
    }
}
