package pe.mypeinventario.infraestructura;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pe.mypeinventario.seguridad.CodificadorBcrypt;
import pe.mypeinventario.seguridad.CodificadorContrasena;

/** Elige BCrypt como el cifrado de contraseñas que reciben los servicios. */
@Configuration(proxyBeanMethods = false)
public class SeguridadBeans {

    @Bean
    public CodificadorContrasena codificadorContrasena() {
        return new CodificadorBcrypt();
    }
}
