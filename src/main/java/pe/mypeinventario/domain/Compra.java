package pe.mypeinventario.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Compra de mercadería a un {@link ProveedorMayorista} para una {@link Tienda}.
 * Mantiene una composición con sus {@link DetalleCompra}: las líneas no existen
 * sin su compra.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Compra {

    private Long id;
    private LocalDateTime fecha;
    private BigDecimal total = BigDecimal.ZERO;
    private Tienda tienda;
    private ProveedorMayorista proveedor;
    private List<DetalleCompra> detalles = new ArrayList<>();

    /**
     * @param id        identificador (puede ser {@code null} hasta que se persista)
     * @param fecha     fecha y hora de la compra
     * @param tienda    tienda que compra
     * @param proveedor proveedor al que se compra
     * @throws IllegalArgumentException si falta algún dato obligatorio
     */
    public Compra(Long id, LocalDateTime fecha, Tienda tienda, ProveedorMayorista proveedor) {
        if (fecha == null || tienda == null || proveedor == null) {
            throw new IllegalArgumentException("La fecha, la tienda y el proveedor son obligatorios");
        }
        this.id = id;
        this.fecha = fecha;
        this.tienda = tienda;
        this.proveedor = proveedor;
    }

    /** @return vista de solo lectura de las líneas de la compra */
    public List<DetalleCompra> getDetalles() {
        return Collections.unmodifiableList(detalles);
    }

    /**
     * Agrega una línea a la compra y actualiza el total.
     * La validación de que la variante pertenezca a la misma tienda se hará en el service (Etapa 2).
     *
     * @param detalle línea a agregar
     * @throws IllegalArgumentException si el detalle es nulo
     */
    public void agregarDetalle(DetalleCompra detalle) {
        if (detalle == null) {
            throw new IllegalArgumentException("El detalle no puede ser nulo");
        }
        detalles.add(detalle);
        total = total.add(detalle.calcularSubtotal());
    }
}
