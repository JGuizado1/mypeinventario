package pe.mypeinventario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.mypeinventario.domain.Categoria;

import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    /** @return categorías raíz, es decir, las que no tienen categoría padre */
    List<Categoria> findByCategoriaPadreIsNull();

    List<Categoria> findByCategoriaPadreId(Long categoriaPadreId);
}
