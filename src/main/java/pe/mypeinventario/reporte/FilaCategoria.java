package pe.mypeinventario.reporte;

import java.math.BigDecimal;

/**
 * Una fila del reporte de ventas por categoría: el árbol de categorías aplanado, con su
 * nivel de sangría.
 *
 * @param nombre    nombre de la categoría
 * @param nivel     0 para una categoría principal, 1 para su subcategoría, y así
 * @param unidades  unidades vendidas, incluidas las de sus subcategorías
 * @param ingresos  ingresos, incluidos los de sus subcategorías
 * @param costo     costo, incluido el de sus subcategorías
 * @param utilidad  ingresos menos costo
 */
public record FilaCategoria(String nombre, int nivel, long unidades, BigDecimal ingresos, BigDecimal costo,
                            BigDecimal utilidad) {
}
