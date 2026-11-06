package pe.mypeinventario.infraestructura;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import pe.mypeinventario.web.AccesoInterceptor;

/** Protege todas las páginas menos la entrada, el registro y los archivos estáticos. */
@Configuration(proxyBeanMethods = false)
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AccesoInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/registro", "/css/**", "/error", "/h2-console/**");
    }
}
