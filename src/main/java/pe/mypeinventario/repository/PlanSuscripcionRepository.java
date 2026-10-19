package pe.mypeinventario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.mypeinventario.domain.PlanSuscripcion;

import java.util.Optional;


public interface PlanSuscripcionRepository extends JpaRepository<PlanSuscripcion, Long> {
    Optional<PlanSuscripcion> findByNombre(String nombre);
}
