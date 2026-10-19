package pe.mypeinventario.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pe.mypeinventario.domain.TipoMovimiento;
import pe.mypeinventario.factory.CreadorMovimiento;
import pe.mypeinventario.factory.CreadorMovimientoAjuste;
import pe.mypeinventario.factory.CreadorMovimientoCompra;
import pe.mypeinventario.factory.CreadorMovimientoDevolucion;
import pe.mypeinventario.factory.CreadorMovimientoVenta;

/**
 * Registra los creadores de movimientos como dependencias inyectables. Los
 * creadores son clases del patrón; Spring solo guarda sus referencias.
 */
@Configuration(proxyBeanMethods = false)
public class CreadoresMovimientoConfig {

    @Bean
    public Clock reloj() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public CreadorMovimiento creadorCompra(Clock reloj) {
        return new CreadorMovimientoCompra(reloj);
    }

    @Bean
    public CreadorMovimiento creadorVenta(Clock reloj) {
        return new CreadorMovimientoVenta(reloj);
    }

    @Bean
    public CreadorMovimiento creadorDevolucion(Clock reloj) {
        return new CreadorMovimientoDevolucion(reloj);
    }

    @Bean
    public CreadorMovimiento creadorAjustePositivo(Clock reloj) {
        return new CreadorMovimientoAjuste(reloj, TipoMovimiento.AJUSTE_POSITIVO);
    }

    @Bean
    public CreadorMovimiento creadorAjusteNegativo(Clock reloj) {
        return new CreadorMovimientoAjuste(reloj, TipoMovimiento.AJUSTE_NEGATIVO);
    }
}
