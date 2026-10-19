package pe.mypeinventario.domain;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Línea de una {@link VentaPunto}. Guarda el precio unitario del momento de la
 * venta, de modo que un cambio posterior de precio no altere ventas pasadas.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DetalleVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variante_id")
    private VarianteSKU variante;
    private int cantidad;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    /**
     * @param id             identificador (puede ser {@code null} hasta que se persista)
     * @param variante       variante vendida
     * @param cantidad       unidades, mayor que cero
     * @param precioUnitario precio por unidad, no negativo
     * @throws IllegalArgumentException si algún dato es inválido
     */
    public DetalleVenta(Long id, VarianteSKU variante, int cantidad, BigDecimal precioUnitario) {
        if (variante == null) {
            throw new IllegalArgumentException("La variante es obligatoria");
        }
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        }
        if (precioUnitario == null || precioUnitario.signum() < 0) {
            throw new IllegalArgumentException("El precio unitario no puede ser nulo ni negativo");
        }
        this.id = id;
        this.variante = variante;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    /** @return precio unitario multiplicado por la cantidad */
    public BigDecimal calcularSubtotal() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }
}
