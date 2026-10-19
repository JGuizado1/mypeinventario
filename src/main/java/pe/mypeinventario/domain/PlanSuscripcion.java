package pe.mypeinventario.domain;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Plan comercial de suscripción mensual (por ejemplo, de S/ 20 a S/ 59). */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlanSuscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 60)
    private String nombre;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precioMensual;
    private int limiteProductos;
}
