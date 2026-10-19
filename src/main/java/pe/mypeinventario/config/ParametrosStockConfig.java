package pe.mypeinventario.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Expone el Singleton {@link ConfiguracionTienda} como dependencia inyectable.
 * Spring solo conserva una referencia: la instancia única sigue siendo la del patrón.
 */
@Configuration(proxyBeanMethods = false)
public class ParametrosStockConfig {

    @Bean
    public ParametrosStock parametrosStock() {
        return ConfiguracionTienda.getInstance();
    }
}
