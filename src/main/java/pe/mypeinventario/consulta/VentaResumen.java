package pe.mypeinventario.consulta;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import pe.mypeinventario.domain.MedioPago;

/**
 * Vista de solo lectura de una venta ya registrada.
 *
 * @param id        identificador
 * @param tiendaId  tienda donde se vendió
 * @param fecha     fecha y hora de la venta
 * @param cliente   nombre del cliente, o {@code null} si fue una venta anónima
 * @param medioPago medio de pago
 * @param subtotal  suma de las líneas, antes de descuentos
 * @param descuento descuento total aplicado
 * @param total     lo que pagó el cliente
 * @param igv       IGV contenido en el total
 * @param lineas    líneas de la venta
 */
public record VentaResumen(Long id, Long tiendaId, LocalDateTime fecha, String cliente, MedioPago medioPago,
                           BigDecimal subtotal, BigDecimal descuento, BigDecimal total, BigDecimal igv,
                           List<LineaVentaResumen> lineas) {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** @return la fecha como «06/11/2026 19:30» */
    public String fechaTexto() {
        return FORMATO_FECHA.format(fecha);
    }

    /** @return el medio de pago con la inicial en mayúscula, por ejemplo «Yape» */
    public String medioPagoTexto() {
        String nombre = medioPago.name();
        return nombre.charAt(0) + nombre.substring(1).toLowerCase();
    }

    /** @return el total sin IGV */
    public BigDecimal baseImponible() {
        return total.subtract(igv);
    }

    /** @return unidades vendidas sumando todas las líneas */
    public int unidades() {
        return lineas.stream().mapToInt(LineaVentaResumen::cantidad).sum();
    }
}
