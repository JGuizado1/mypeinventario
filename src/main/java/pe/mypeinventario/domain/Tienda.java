package pe.mypeinventario.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Local comercial administrado por un {@link Comerciante}. Las ventas, compras,
 * productos y clientes guardan una referencia a su tienda, no al revés.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Tienda {

    private Long id;
    private String nombre;
    private String direccion;
    /** Galería o centro comercial donde opera (por ejemplo, Gamarra). */
    private String galeria;
}
