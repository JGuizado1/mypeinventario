package pe.mypeinventario.seguridad;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Implementación de {@link CodificadorContrasena} con BCrypt. */
public class CodificadorBcrypt implements CodificadorContrasena {

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Override
    public String codificar(String contrasena) {
        return bcrypt.encode(contrasena);
    }

    @Override
    public boolean coincide(String contrasena, String hash) {
        return contrasena != null && hash != null && bcrypt.matches(contrasena, hash);
    }
}
