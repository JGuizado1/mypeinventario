package pe.mypeinventario.config;

import java.math.BigDecimal;

/** Parámetros tributarios que necesita el cálculo de importes. */
public interface ParametrosTributarios {

    /** @return tasa del IGV como fracción, por ejemplo 0.18 para 18 % */
    BigDecimal getIgv();
}
