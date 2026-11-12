package pe.mypeinventario.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Conteo físico del inventario de una {@link Tienda}. Mantiene una composición con sus
 * {@link DetalleTomaInventario}. Nace en curso y, una vez cerrada, no admite más cambios.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TomaInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoToma estado = EstadoToma.EN_CURSO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tienda_id")
    private Tienda tienda;

    /** Composición: las líneas viven y mueren con la toma. */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "toma_id", nullable = false)
    private List<DetalleTomaInventario> detalles = new ArrayList<>();

    /**
     * @param id     identificador (puede ser {@code null} hasta que se persista)
     * @param fecha  fecha y hora de inicio de la toma
     * @param tienda tienda donde se cuenta
     * @throws IllegalArgumentException si falta la fecha o la tienda
     */
    public TomaInventario(Long id, LocalDateTime fecha, Tienda tienda) {
        if (fecha == null || tienda == null) {
            throw new IllegalArgumentException("La fecha y la tienda son obligatorias");
        }
        this.id = id;
        this.fecha = fecha;
        this.tienda = tienda;
    }

    /** @return vista de solo lectura de las líneas contadas */
    public List<DetalleTomaInventario> getDetalles() {
        return Collections.unmodifiableList(detalles);
    }

    /**
     * Registra el conteo de una variante.
     *
     * @param variante     variante contada
     * @param stockSistema stock del sistema al momento de contar
     * @param stockContado stock contado físicamente
     * @throws IllegalArgumentException si falta la variante o algún stock es negativo
     * @throws IllegalStateException    si la toma está cerrada o la variante ya fue contada
     */
    public void agregarConteo(VarianteSKU variante, int stockSistema, int stockContado) {
        exigirEnCurso();
        if (variante == null) {
            throw new IllegalArgumentException("La variante es obligatoria");
        }
        if (detalles.stream().anyMatch(d -> mismaVariante(d.getVariante(), variante))) {
            throw new IllegalStateException("La variante " + variante.getSku() + " ya fue contada en esta toma");
        }
        detalles.add(new DetalleTomaInventario(null, variante, stockSistema, stockContado));
    }

    /**
     * Cierra la toma. Aplicar los ajustes de las diferencias es responsabilidad del service.
     *
     * @throws IllegalStateException si ya estaba cerrada o no tiene conteos
     */
    public void cerrar() {
        exigirEnCurso();
        if (detalles.isEmpty()) {
            throw new IllegalStateException("No se puede cerrar una toma sin conteos");
        }
        estado = EstadoToma.CERRADA;
    }

    /** @return {@code true} si la toma ya fue cerrada */
    public boolean estaCerrada() {
        return estado == EstadoToma.CERRADA;
    }

    private void exigirEnCurso() {
        if (estado != EstadoToma.EN_CURSO) {
            throw new IllegalStateException("La toma de inventario ya está cerrada");
        }
    }

    /** Compara por identificador para no depender de si la variante llegó como proxy. */
    private static boolean mismaVariante(VarianteSKU a, VarianteSKU b) {
        return a == b || (a.getId() != null && a.getId().equals(b.getId()));
    }
}
