package pe.mypeinventario.domain;

import jakarta.persistence.*;
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
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, unique = true, length = 120)
    private String correo;

    @Column(length = 20)
    private String celular;

    /** Hash de la contraseña. Nunca se guarda la contraseña en texto plano. */
    @Column(nullable = false)
    private String contrasenaHash;
}