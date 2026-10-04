package pe.mypeinventario.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Producto del catálogo (por ejemplo, "Zapatilla Runner"). Se vende en una o más
 * {@link VarianteSKU} (talla y color), con las que mantiene una relación de
 * composición: las variantes no existen sin su producto.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductoItem {

    private Long id;
    private String nombre;
    private String descripcion;
    private boolean activo = true;
    private Tienda tienda;
    /** Opcional (0..1). */
    private Marca marca;
    /** Se asigna al agregar el producto a una categoría ({@link Categoria#agregarProducto}). */
    private Categoria categoria;

    @Setter(AccessLevel.NONE)
    private List<VarianteSKU> variantes = new ArrayList<>();

    public ProductoItem(Long id, String nombre, String descripcion, Tienda tienda, Marca marca) {
        if (tienda == null) {
            throw new IllegalArgumentException("Todo producto debe pertenecer a una tienda");
        }
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.tienda = tienda;
        this.marca = marca;
    }

    /** @return vista de solo lectura de las variantes del producto */
    public List<VarianteSKU> getVariantes() {
        return Collections.unmodifiableList(variantes);
    }

    /**
     * Agrega una variante (talla y color) al producto.
     *
     * @param variante variante a agregar
     * @throws IllegalArgumentException si la variante es nula
     */
    public void agregarVariante(VarianteSKU variante) {
        if (variante == null) {
            throw new IllegalArgumentException("La variante no puede ser nula");
        }
        variantes.add(variante);
    }
}
