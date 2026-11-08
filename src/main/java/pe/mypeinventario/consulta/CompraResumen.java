package pe.mypeinventario.consulta;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Vista de solo lectura de una compra ya registrada.
 *
 * @param id        identificador
 * @param tiendaId  tienda que compró
 * @param fecha     fecha y hora de la compra
 * @param proveedor razón social del proveedor
 * @param total     total pagado al proveedor
 * @param lineas    líneas de la compra
 */
public record CompraResumen(Long id, Long tiendaId, LocalDateTime fecha, String proveedor, BigDecimal total,
                            List<LineaCompraResumen> lineas) {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** @return la fecha como «06/11/2026 19:30» */
    public String fechaTexto() {
        return FORMATO_FECHA.format(fecha);
    }

    /** @return unidades compradas sumando todas las líneas */
    public int unidades() {
        return lineas.stream().mapToInt(LineaCompraResumen::cantidad).sum();
    }
}
