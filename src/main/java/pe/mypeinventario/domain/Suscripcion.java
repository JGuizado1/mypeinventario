package pe.mypeinventario.domain;

import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Suscripción de un {@link Comerciante} a un {@link PlanSuscripcion}. Empieza en
 * período de prueba gratuita y puede activarse o cancelarse.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Suscripcion {

    private Long id;
    private PlanSuscripcion plan;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private EstadoSuscripcion estado;

    /**
     * Crea una suscripción en estado de prueba.
     *
     * @param id          identificador (puede ser {@code null} hasta que se persista)
     * @param plan        plan contratado
     * @param fechaInicio inicio de la vigencia
     * @param fechaFin    fin de la vigencia, no anterior al inicio
     * @throws IllegalArgumentException si falta algún dato o las fechas son incoherentes
     */
    public Suscripcion(Long id, PlanSuscripcion plan, LocalDate fechaInicio, LocalDate fechaFin) {
        if (plan == null || fechaInicio == null || fechaFin == null) {
            throw new IllegalArgumentException("El plan y las fechas son obligatorios");
        }
        if (fechaFin.isBefore(fechaInicio)) {
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la de inicio");
        }
        this.id = id;
        this.plan = plan;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.estado = EstadoSuscripcion.PRUEBA;
    }

    /**
     * Indica si la suscripción permite usar el sistema en la fecha dada.
     *
     * @param fecha fecha a consultar
     * @return {@code true} si está en prueba o activa y la fecha cae dentro de su vigencia
     */
    public boolean estaVigente(LocalDate fecha) {
        boolean estadoValido = estado == EstadoSuscripcion.PRUEBA || estado == EstadoSuscripcion.ACTIVA;
        return estadoValido && !fecha.isBefore(fechaInicio) && !fecha.isAfter(fechaFin);
    }

    /**
     * Pasa de prueba a activa (el comerciante empezó a pagar).
     *
     * @throws IllegalStateException si no estaba en período de prueba
     */
    public void activar() {
        if (estado != EstadoSuscripcion.PRUEBA) {
            throw new IllegalStateException("Solo una suscripción en prueba puede activarse");
        }
        estado = EstadoSuscripcion.ACTIVA;
    }

    /**
     * Cancela la suscripción.
     *
     * @throws IllegalStateException si ya estaba cancelada
     */
    public void cancelar() {
        if (estado == EstadoSuscripcion.CANCELADA) {
            throw new IllegalStateException("La suscripción ya está cancelada");
        }
        estado = EstadoSuscripcion.CANCELADA;
    }
}
