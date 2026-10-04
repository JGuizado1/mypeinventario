package pe.mypeinventario.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Usuario base del sistema. Concentra los datos de identidad comunes a todos
 * los roles. Es abstracta: no existe un "usuario genérico", solo comerciantes
 * y proveedores mayoristas.
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Usuario {

    private Long id;
    private String nombre;
    private String correo;
    private String celular;
    /** Hash de la contraseña. Nunca se guarda la contraseña en texto plano. */
    private String contrasenaHash;
}