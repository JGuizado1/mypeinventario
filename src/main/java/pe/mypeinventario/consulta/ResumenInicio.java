package pe.mypeinventario.consulta;

/**
 * Cifras de la pantalla de inicio de una tienda.
 *
 * @param tienda                tienda consultada
 * @param productosActivos      productos que se pueden vender
 * @param variantesConStockBajo variantes cuyo stock alcanzó o bajó del mínimo
 * @param alertasPendientes     alertas de reabastecimiento sin revisar
 */
public record ResumenInicio(TiendaResumen tienda, int productosActivos, int variantesConStockBajo,
                            int alertasPendientes) {
}
