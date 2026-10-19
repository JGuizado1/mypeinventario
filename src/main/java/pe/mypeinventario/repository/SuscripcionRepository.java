package pe.mypeinventario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.mypeinventario.domain.Suscripcion;

import java.util.List;

public interface SuscripcionRepository extends JpaRepository<Suscripcion, Long> {
    /**
     * Historial de suscripciones de un comerciante, de la más reciente a la más antigua.
     *
     * @param comercianteId identificador del comerciante
     * @return suscripciones del comerciante
     */
    @Query("""
            select s from Comerciante c join c.suscripciones s
            where c.id = :comercianteId
            order by s.fechaInicio desc
            """)
    List<Suscripcion> listarPorComerciante(@Param("comercianteId") Long comercianteId);
}
