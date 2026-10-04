package pe.mypeinventario.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Marca comercial de un producto. Catálogo global compartido entre tiendas. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Marca {

    private Long id;
    private String nombre;
}
