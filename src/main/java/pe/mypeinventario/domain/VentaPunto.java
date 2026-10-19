package pe.mypeinventario.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Venta realizada en el punto de venta de una {@link Tienda}. Mantiene una
 * composición con sus {@link DetalleVenta}. El {@link Cliente} es opcional
 * porque muchas ventas de mostrador son anónimas.
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
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MedioPago medioPago;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tienda_id")
    private Tienda tienda;
    /** Opcional (0..1). */
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
     * Agrega una línea a la venta y actualiza el total (suma simple; los descuentos
     * e IGV se tratarán en la Etapa 2).
     *
     * @param detalle línea a agregar
     * @throws IllegalArgumentException si el detalle es nulo
     */
    public void agregarDetalle(DetalleVenta detalle) {
        if (detalle == null) {
            throw new IllegalArgumentException("El detalle no puede ser nulo");
        }
        detalles.add(detalle);
        total = total.add(detalle.calcularSubtotal());
    }
}
