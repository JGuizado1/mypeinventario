package pe.mypeinventario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.mypeinventario.domain.VentaPunto;

import java.time.LocalDateTime;
import java.util.List;

public interface VentaPuntoRepository extends JpaRepository<VentaPunto, Long> {
    List<VentaPunto> findByTiendaIdOrderByFechaDesc(Long tiendaId);
    List<VentaPunto> findByTiendaIdAndFechaBetween(Long tiendaId, LocalDateTime desde, LocalDateTime hasta);
}
