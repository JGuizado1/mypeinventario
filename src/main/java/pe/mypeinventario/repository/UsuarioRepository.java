package pe.mypeinventario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.mypeinventario.domain.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    /** El correo es único entre todos los usuarios, sean comerciantes o proveedores. */
    boolean existsByCorreo(String correo);
}
