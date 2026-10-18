package pe.mypeinventario.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Local comercial administrado por un {@link Comerciante}. Las ventas, compras,
 * productos y clientes guardan una referencia a su tienda, no al revés.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Tienda {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String nombre;
    private String direccion;
    /** Galería o centro comercial donde opera (por ejemplo, Gamarra). */
    private String galeria;
}
