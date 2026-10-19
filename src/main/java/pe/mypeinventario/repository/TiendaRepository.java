package pe.mypeinventario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.mypeinventario.domain.Tienda;

import java.util.List;

public interface TiendaRepository extends JpaRepository<Tienda, Long> {
    /**
     * Lista las tiendas de un comerciante. La relación se guarda en el lado del
     * comerciante, por eso se consulta con una unión.
     *
     * @param comercianteId identificador del comerciante
     * @return tiendas administradas por ese comerciante
     */
    @Query("select t from Comerciante c join c.tiendas t where c.id = :comercianteId")
    List<Tienda> listarPorComerciante(@Param("comercianteId") Long comercianteId);
}
