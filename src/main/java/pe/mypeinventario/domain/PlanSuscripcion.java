package pe.mypeinventario.domain;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Plan comercial de suscripción mensual (por ejemplo, de S/ 20 a S/ 59). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlanSuscripcion {

    private Long id;
    private String nombre;
    private BigDecimal precioMensual;
    private int limiteProductos;
}
