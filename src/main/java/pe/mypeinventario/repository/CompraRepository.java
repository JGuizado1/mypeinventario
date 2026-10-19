package pe.mypeinventario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.mypeinventario.domain.Compra;

import java.util.List;

public interface CompraRepository extends JpaRepository<Compra, Long> {
    List<Compra> findByTiendaIdOrderByFechaDesc(Long tiendaId);

    List<Compra> findByProveedorId(Long proveedorId);
}
