package pe.mypeinventario.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.mypeinventario.domain.EstadoToma;
import pe.mypeinventario.domain.TomaInventario;

/** Acceso a datos de {@link TomaInventario}. */
public interface TomaInventarioRepository extends JpaRepository<TomaInventario, Long> {

    /** @return {@code true} si la tienda tiene una toma en el estado indicado */
    boolean existsByTiendaIdAndEstado(Long tiendaId, EstadoToma estado);

    /** @return tomas de la tienda, de la más reciente a la más antigua */
    List<TomaInventario> findByTiendaIdOrderByFechaDesc(Long tiendaId);
}