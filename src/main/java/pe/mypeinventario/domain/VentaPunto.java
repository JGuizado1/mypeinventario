package pe.mypeinventario.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Venta realizada en el punto de venta de una {@link Tienda}. Mantiene una
 * composición con sus {@link DetalleVenta}. El {@link Cliente} es opcional
 * porque muchas ventas de mostrador son anónimas.
 * <p>
 * Los precios incluyen IGV: el subtotal es la suma de las líneas, el total es
 * lo que paga el cliente después de descuentos, y el IGV es la parte del total
 * que corresponde al impuesto.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VentaPunto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha;

    /** Suma de las líneas, antes de descuentos. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    /** Descuento total aplicado (subtotal menos total). */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal descuento = BigDecimal.ZERO;

    /** Lo que paga el cliente. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    /** IGV contenido en el total. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal igv = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MedioPago medioPago;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tienda_id")
    private Tienda tienda;

    /** Opcional (0..1): las ventas de mostrador pueden ser anónimas. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    /** Composición: las líneas viven y mueren con la venta. */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "venta_id", nullable = false)
    private List<DetalleVenta> detalles = new ArrayList<>();

    /**
     * @param id        identificador (puede ser {@code null} hasta que se persista)
     * @param fecha     fecha y hora de la venta
     * @param medioPago medio de pago utilizado
     * @param tienda    tienda donde se realiza la venta
     * @param cliente   cliente asociado, o {@code null} si es anónima
     * @throws IllegalArgumentException si falta algún dato obligatorio
     */
    public VentaPunto(Long id, LocalDateTime fecha, MedioPago medioPago, Tienda tienda, Cliente cliente) {
        if (fecha == null || medioPago == null || tienda == null) {
            throw new IllegalArgumentException("La fecha, el medio de pago y la tienda son obligatorios");
        }
        this.id = id;
        this.fecha = fecha;
        this.medioPago = medioPago;
        this.tienda = tienda;
        this.cliente = cliente;
    }

    /** @return vista de solo lectura de las líneas de la venta */
    public List<DetalleVenta> getDetalles() {
        return Collections.unmodifiableList(detalles);
    }

    /**
     * Agrega una línea y recalcula el subtotal. Mientras no se aplique un importe
     * final, el total es igual al subtotal; si ya había un descuento aplicado, se
     * descarta y debe aplicarse de nuevo.
     *
     * @param detalle línea a agregar
     * @throws IllegalArgumentException si el detalle es nulo
     */
    public void agregarDetalle(DetalleVenta detalle) {
        if (detalle == null) {
            throw new IllegalArgumentException("El detalle no puede ser nulo");
        }
        detalles.add(detalle);
        subtotal = subtotal.add(detalle.calcularSubtotal());
        total = subtotal;
        descuento = BigDecimal.ZERO;
        igv = BigDecimal.ZERO;
    }

    /**
     * Fija el importe final tras aplicar descuentos. El descuento queda como la
     * diferencia entre el subtotal y el total.
     *
     * @param totalFinal   lo que paga el cliente, entre 0 y el subtotal
     * @param igvIncluido  IGV contenido en el total, entre 0 y el total
     * @throws IllegalArgumentException si algún valor es nulo o está fuera de rango
     */
    public void aplicarImporteFinal(BigDecimal totalFinal, BigDecimal igvIncluido) {
        if (totalFinal == null || igvIncluido == null) {
            throw new IllegalArgumentException("El total y el IGV son obligatorios");
        }
        if (totalFinal.signum() < 0 || totalFinal.compareTo(subtotal) > 0) {
            throw new IllegalArgumentException("El total final debe estar entre 0 y el subtotal");
        }
        if (igvIncluido.signum() < 0 || igvIncluido.compareTo(totalFinal) > 0) {
            throw new IllegalArgumentException("El IGV debe estar entre 0 y el total");
        }
        this.descuento = subtotal.subtract(totalFinal);
        this.total = totalFinal;
        this.igv = igvIncluido;
    }
}