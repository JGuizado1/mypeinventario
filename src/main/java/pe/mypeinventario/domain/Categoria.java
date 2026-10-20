package pe.mypeinventario.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Categoría de productos (por ejemplo, "Calzado" o "Zapatillas"). Puede tener una
 * categoría padre y agrupa productos mediante agregación: los productos existen
 * aunque la categoría desaparezca. Es un catálogo global compartido entre tiendas.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 80)
    private String nombre;
    private String descripcion;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_padre_id")
    private Categoria categoriaPadre;

    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "categoria")
    private List<ProductoItem> productos = new ArrayList<>();

    public Categoria(Long id, String nombre, String descripcion, Categoria categoriaPadre) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        setCategoriaPadre(categoriaPadre);
    }

    /**
     * Asigna la categoría padre.
     *
     * @param categoriaPadre la categoría padre, o {@code null} si es una categoría raíz
     * @throws IllegalArgumentException si se intenta ser padre de sí misma
     */
    public void setCategoriaPadre(Categoria categoriaPadre) {
        if (categoriaPadre == this) {
            throw new IllegalArgumentException("Una categoría no puede ser su propia padre");
        }
        this.categoriaPadre = categoriaPadre;
    }

    /** @return vista de solo lectura de los productos de esta categoría */
    public List<ProductoItem> getProductos() {
        return Collections.unmodifiableList(productos);
    }

    /**
     * Agrega un producto a la categoría y mantiene sincronizada la relación
     * inversa mediante el atributo categoria de {@link ProductoItem}.
     *
     * @param producto producto que se desea asociar a esta categoría
     * @throws IllegalArgumentException si el producto es nulo
     */
    public void agregarProducto(ProductoItem producto) {
        if (producto == null) {
            throw new IllegalArgumentException("El producto no puede ser nulo");
        }
        if (!productos.contains(producto)) {
            productos.add(producto);
        }
        producto.setCategoria(this);
    }
}
