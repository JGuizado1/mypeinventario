package pe.mypeinventario.infraestructura;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pe.mypeinventario.config.ConfiguracionTienda;
import pe.mypeinventario.config.ParametrosStock;
import pe.mypeinventario.config.ParametrosTributarios;

/**
 * Expone el Singleton {@link ConfiguracionTienda} como dependencia inyectable.
 * Spring solo conserva una referencia: la instancia única sigue siendo la del patrón.
 */
@Configuration(proxyBeanMethods = false)
public class ConfiguracionTiendaBeans {

    @Bean
    public ParametrosStock parametrosStock() {
        return ConfiguracionTienda.getInstance();
    }

    @Bean
    public ParametrosTributarios parametrosTributarios() {
        return ConfiguracionTienda.getInstance();
    }
}
