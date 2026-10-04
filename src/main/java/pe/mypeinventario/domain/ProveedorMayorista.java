package pe.mypeinventario.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Distribuidor mayorista que abastece de mercadería a los comerciantes.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProveedorMayorista extends Usuario {

    private String razonSocial;
    private String ruc;

    public ProveedorMayorista(Long id, String nombre, String correo, String celular,
                              String contrasenaHash, String razonSocial, String ruc) {
        super(id, nombre, correo, celular, contrasenaHash);
        this.razonSocial = razonSocial;
        this.ruc = ruc;
    }
}