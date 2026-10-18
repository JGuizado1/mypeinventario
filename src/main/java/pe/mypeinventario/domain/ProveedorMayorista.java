package pe.mypeinventario.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Distribuidor mayorista que abastece de mercadería a los comerciantes.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class ProveedorMayorista extends Usuario {
    @Column(nullable = false, length = 150)
    private String razonSocial;
    @Column(unique = true, length = 11)
    private String ruc;

    public ProveedorMayorista(Long id, String nombre, String correo, String celular,
                              String contrasenaHash, String razonSocial, String ruc) {
        super(id, nombre, correo, celular, contrasenaHash);
        this.razonSocial = razonSocial;
        this.ruc = ruc;
    }
}