package pe.mypeinventario.domain;

import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Línea de una {@link Compra}: cuántas unidades de una variante se compraron y a qué costo. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DetalleCompra {

    private Long id;
    private VarianteSKU variante;
    private int cantidad;
    private BigDecimal costoUnitario;

    /**
     * @param id            identificador (puede ser {@code null} hasta que se persista)
     * @param variante      variante comprada
     * @param cantidad      unidades, mayor que cero
     * @param costoUnitario costo por unidad, no negativo
     * @throws IllegalArgumentException si algún dato es inválido
     */
    public DetalleCompra(Long id, VarianteSKU variante, int cantidad, BigDecimal costoUnitario) {
        if (variante == null) {
            throw new IllegalArgumentException("La variante es obligatoria");
        }
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        }
        if (costoUnitario == null || costoUnitario.signum() < 0) {
            throw new IllegalArgumentException("El costo unitario no puede ser nulo ni negativo");
        }
        this.id = id;
        this.variante = variante;
        this.cantidad = cantidad;
        this.costoUnitario = costoUnitario;
    }

    /** @return costo unitario multiplicado por la cantidad */
    public BigDecimal calcularSubtotal() {
        return costoUnitario.multiply(BigDecimal.valueOf(cantidad));
    }
}
