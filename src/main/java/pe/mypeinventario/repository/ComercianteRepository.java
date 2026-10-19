package pe.mypeinventario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.mypeinventario.domain.Comerciante;

import java.util.Optional;

public interface ComercianteRepository extends JpaRepository<Comerciante, Long> {
    Optional<Comerciante> findByCorreo(String correo);
    boolean existsByCorreo(String correo);
}
