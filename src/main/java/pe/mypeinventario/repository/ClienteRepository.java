package pe.mypeinventario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.mypeinventario.domain.Cliente;

import java.util.List;
import java.util.Optional;


public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    List<Cliente> findByTiendaId(Long tiendaId);
    Optional<Cliente> findByTiendaIdAndDocumento(Long tiendaId, String documento);
}
