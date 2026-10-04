package pe.mypeinventario.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Cliente final de una {@link Tienda}, usado para ventas frecuentes o con seguimiento. */
@Getter
@Setter
@NoArgsConstructor
public class Cliente {

    private Long id;
    private String nombre;
    private String celular;
    private String documento;
    private Tienda tienda;

    /**
     * @param id        identificador (puede ser {@code null} hasta que se persista)
     * @param nombre    nombre del cliente
     * @param celular   celular de contacto
     * @param documento DNI o RUC
     * @param tienda    tienda a la que pertenece el cliente
     * @throws IllegalArgumentException si la tienda es nula
     */
    public Cliente(Long id, String nombre, String celular, String documento, Tienda tienda) {
        if (tienda == null) {
            throw new IllegalArgumentException("Todo cliente debe pertenecer a una tienda");
        }
        this.id = id;
        this.nombre = nombre;
        this.celular = celular;
        this.documento = documento;
        this.tienda = tienda;
    }
}
