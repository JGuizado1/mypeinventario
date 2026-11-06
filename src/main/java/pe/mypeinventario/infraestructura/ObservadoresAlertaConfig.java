package pe.mypeinventario.infraestructura;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pe.mypeinventario.observer.BandejaNotificaciones;
import pe.mypeinventario.observer.PublicadorAlertas;
import pe.mypeinventario.observer.RegistroAlertasLog;
import pe.mypeinventario.observer.SujetoAlertas;

/**
 * Crea el sujeto de las alertas y le suscribe sus observadores. Agregar un observador
 * nuevo es agregarlo aquí, sin modificar el service de inventario.
 */
@Configuration(proxyBeanMethods = false)
public class ObservadoresAlertaConfig {

    @Bean
    public RegistroAlertasLog registroAlertasLog() {
        return new RegistroAlertasLog();
    }

    @Bean
    public BandejaNotificaciones bandejaNotificaciones() {
        return new BandejaNotificaciones();
    }

    @Bean
    public SujetoAlertas sujetoAlertas(RegistroAlertasLog registro, BandejaNotificaciones bandeja) {
        PublicadorAlertas publicador = new PublicadorAlertas();
        publicador.suscribir(registro);
        publicador.suscribir(bandeja);
        return publicador;
    }
}
