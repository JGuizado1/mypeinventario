package pe.mypeinventario.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Línea de una {@link TomaInventario}: lo que el sistema decía y lo que se contó de una
 * variante. Guarda ambos valores para que la diferencia no cambie con movimientos posteriores.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DetalleTomaInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variante_id")
    private VarianteSKU variante;

    private int stockSistema;
    private int stockContado;

    /**
     * @param id           identificador (puede ser {@code null} hasta que se persista)
     * @param variante     variante contada
     * @param stockSistema stock calculado por el sistema al contar, no negativo
     * @param stockContado stock contado físicamente, no negativo
     * @throws IllegalArgumentException si falta la variante o algún stock es negativo
     */
    public DetalleTomaInventario(Long id, VarianteSKU variante, int stockSistema, int stockContado) {
        if (variante == null) {
            throw new IllegalArgumentException("La variante es obligatoria");
        }
        if (stockSistema < 0 || stockContado < 0) {
            throw new IllegalArgumentException("Los stocks no pueden ser negativos");
        }
        this.id = id;
        this.variante = variante;
        this.stockSistema = stockSistema;
        this.stockContado = stockContado;
    }

    /** @return diferencia entre lo contado y lo que decía el sistema (negativa si faltan unidades) */
    public int calcularDiferencia() {
        return stockContado - stockSistema;
    }
}
