package pe.mypeinventario.config;

/**
 * Parámetros de configuración que necesita el dominio para evaluar el stock.
 * Expone solo lo que {@code VarianteSKU} necesita, sin acoplarla a toda la
 * configuración de la tienda.
 */
public interface ParametrosStock {

    /** @return stock mínimo que se aplica cuando una variante no define el suyo */
    int getStockMinimoPorDefecto();
}
