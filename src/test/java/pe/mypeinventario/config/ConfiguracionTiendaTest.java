package pe.mypeinventario.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Singleton: una sola instancia, y sus valores solo cambian si son válidos. */
class ConfiguracionTiendaTest {

    private final ConfiguracionTienda config = ConfiguracionTienda.getInstance();
    private String monedaOriginal;
    private BigDecimal igvOriginal;
    private int stockOriginal;

    @BeforeEach
    void guardarValores() {
        monedaOriginal = config.getMoneda();
        igvOriginal = config.getIgv();
        stockOriginal = config.getStockMinimoPorDefecto();
    }

    @AfterEach
    void restaurarValores() {
        config.setMoneda(monedaOriginal);
        config.setIgv(igvOriginal);
        config.setStockMinimoPorDefecto(stockOriginal);
    }

    @Test
    void siempreSeDevuelveLaMismaInstancia() {
        assertThat(ConfiguracionTienda.getInstance()).isSameAs(config);
    }

    @Test
    void nadieMasPuedeCrearOtraInstanciaNiConReflexion() throws Exception {
        Constructor<ConfiguracionTienda> constructor = ConfiguracionTienda.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        assertThatThrownBy(constructor::newInstance).isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void losValoresValidosSeGuardan() {
        config.setMoneda("USD");
        config.setIgv(new BigDecimal("0.10"));
        config.setStockMinimoPorDefecto(8);

        assertThat(config.getMoneda()).isEqualTo("USD");
        assertThat(config.getIgv()).isEqualByComparingTo("0.10");
        assertThat(config.getStockMinimoPorDefecto()).isEqualTo(8);
    }

    @Test
    void losValoresInvalidosSeRechazanYNoCambianNada() {
        assertThatThrownBy(() -> config.setMoneda(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> config.setMoneda("  ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> config.setIgv(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> config.setIgv(new BigDecimal("-0.01"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> config.setIgv(new BigDecimal("1.01"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> config.setStockMinimoPorDefecto(-1)).isInstanceOf(IllegalArgumentException.class);

        assertThat(config.getMoneda()).isEqualTo(monedaOriginal);
        assertThat(config.getIgv()).isEqualByComparingTo(igvOriginal);
        assertThat(config.getStockMinimoPorDefecto()).isEqualTo(stockOriginal);
    }
}
