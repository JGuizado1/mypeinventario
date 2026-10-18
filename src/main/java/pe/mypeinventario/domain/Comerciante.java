package pe.mypeinventario.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Dueño de uno o más negocios que usa el sistema para gestionar su inventario.
 * Mantiene una agregación con sus {@link Tienda} (existen independientemente de
 * él) y una asociación con sus {@link Suscripcion}.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Comerciante extends Usuario {

    private LocalDate fechaRegistro;
    /** Agregación: sin cascada, la tienda existe por sí sola. */
    @Setter(AccessLevel.NONE)
    @OneToMany
    @JoinColumn(name = "comerciante_id")
    private List<Tienda> tiendas = new ArrayList<>();

    @Setter(AccessLevel.NONE)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "comerciante_id", nullable = false)
    private List<Suscripcion> suscripciones = new ArrayList<>();

    public Comerciante(Long id, String nombre, String correo, String celular,
                       String contrasenaHash, LocalDate fechaRegistro) {
        super(id, nombre, correo, celular, contrasenaHash);
        this.fechaRegistro = fechaRegistro;
    }

    /** @return vista de solo lectura de las tiendas del comerciante */
    public List<Tienda> getTiendas() {
        return Collections.unmodifiableList(tiendas);
    }

    /** @return vista de solo lectura del historial de suscripciones */
    public List<Suscripcion> getSuscripciones() {
        return Collections.unmodifiableList(suscripciones);
    }

    /**
     * Asocia una tienda al comerciante.
     *
     * @param tienda tienda a administrar
     * @throws IllegalArgumentException si la tienda es nula
     */
    public void agregarTienda(Tienda tienda) {
        if (tienda == null) {
            throw new IllegalArgumentException("La tienda no puede ser nula");
        }
        if (!tiendas.contains(tienda)) {
            tiendas.add(tienda);
        }
    }

    /**
     * Registra una suscripción contratada por el comerciante.
     *
     * @param suscripcion suscripción a registrar
     * @throws IllegalArgumentException si la suscripción es nula
     */
    public void agregarSuscripcion(Suscripcion suscripcion) {
        if (suscripcion == null) {
            throw new IllegalArgumentException("La suscripción no puede ser nula");
        }
        suscripciones.add(suscripcion);
    }
}