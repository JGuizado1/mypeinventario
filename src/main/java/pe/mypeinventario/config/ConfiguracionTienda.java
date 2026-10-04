package pe.mypeinventario.config;

import java.math.BigDecimal;

/**
 * Configuración global de la tienda (moneda, IGV y stock mínimo por defecto).
 * Implementa el patrón Singleton de forma thread-safe mediante
 * double-checked locking: existe una única instancia en toda la aplicación.
 */
public final class ConfiguracionTienda implements ParametrosStock {

    private static final String MONEDA_POR_DEFECTO = "PEN";
    private static final BigDecimal IGV_POR_DEFECTO = new BigDecimal("0.18");
    private static final int STOCK_MINIMO_POR_DEFECTO = 5;

    /** Única instancia. volatile evita que otro hilo vea un objeto a medio construir. */
    private static volatile ConfiguracionTienda instancia;

    private volatile String moneda;
    private volatile BigDecimal igv;
    private volatile int stockMinimoPorDefecto;

    /** Constructor privado: nadie fuera de esta clase puede crear instancias. */
    private ConfiguracionTienda() {
        if (instancia != null) {
            throw new IllegalStateException("ConfiguracionTienda ya fue instanciada");
        }
        this.moneda = MONEDA_POR_DEFECTO;
        this.igv = IGV_POR_DEFECTO;
        this.stockMinimoPorDefecto = STOCK_MINIMO_POR_DEFECTO;
    }

    /**
     * Devuelve la única instancia, creándola la primera vez que se pide.
     *
     * @return la instancia compartida de la configuración
     */
    public static ConfiguracionTienda getInstance() {
        if (instancia == null) {                              // 1.ª comprobación (sin bloqueo)
            synchronized (ConfiguracionTienda.class) {
                if (instancia == null) {                      // 2.ª comprobación (con bloqueo)
                    instancia = new ConfiguracionTienda();
                }
            }
        }
        return instancia;
    }

    public String getMoneda() {
        return moneda;
    }

    /**
     * @param moneda código de moneda, por ejemplo "PEN"
     * @throws IllegalArgumentException si es nulo o vacío
     */
    public void setMoneda(String moneda) {
        if (moneda == null || moneda.isBlank()) {
            throw new IllegalArgumentException("La moneda no puede estar vacía");
        }
        this.moneda = moneda;
    }

    public BigDecimal getIgv() {
        return igv;
    }

    /**
     * @param igv tasa expresada como fracción, por ejemplo 0.18 para 18%
     * @throws IllegalArgumentException si es nula o está fuera del rango 0 a 1
     */
    public void setIgv(BigDecimal igv) {
        if (igv == null || igv.signum() < 0 || igv.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("El IGV debe estar entre 0 y 1");
        }
        this.igv = igv;
    }

    @Override
    public int getStockMinimoPorDefecto() {
        return stockMinimoPorDefecto;
    }

    /**
     * @param stockMinimoPorDefecto unidades mínimas antes de generar una alerta
     * @throws IllegalArgumentException si es negativo
     */
    public void setStockMinimoPorDefecto(int stockMinimoPorDefecto) {
        if (stockMinimoPorDefecto < 0) {
            throw new IllegalArgumentException("El stock mínimo no puede ser negativo");
        }
        this.stockMinimoPorDefecto = stockMinimoPorDefecto;
    }
}
