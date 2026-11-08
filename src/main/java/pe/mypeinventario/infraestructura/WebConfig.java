package pe.mypeinventario.infraestructura;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import pe.mypeinventario.web.AccesoInterceptor;
import pe.mypeinventario.web.TiendaActivaInterceptor;

/**
 * Reglas de acceso de la web, en este orden: primero haber ingresado y, para las pantallas
 * de una tienda, haber elegido una. Los proveedores y el catálogo son compartidos y no
 * necesitan una tienda elegida.
 */
@Configuration(proxyBeanMethods = false)
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AccesoInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/registro", "/css/**", "/js/**", "/error", "/h2-console/**");
        registry.addInterceptor(new TiendaActivaInterceptor())
                .addPathPatterns("/productos/**", "/ventas/**", "/compras/**", "/clientes/**");
    }
}
