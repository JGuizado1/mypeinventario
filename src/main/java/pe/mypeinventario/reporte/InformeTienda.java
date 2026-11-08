package pe.mypeinventario.reporte;

import java.util.List;

/**
 * Todo lo que muestra la pantalla de reportes de una tienda para un periodo.
 *
 * @param resumen     ventas, descuentos, IGV, costo y utilidad del periodo
 * @param masVendidos variantes más vendidas, de la más a la menos vendida
 * @param categorias  ventas por categoría, con las subcategorías aplanadas
 * @param sinRotacion variantes con stock que no se venden hace tiempo
 */
public record InformeTienda(ResumenVentas resumen, List<ProductoVendido> masVendidos,
                            List<FilaCategoria> categorias, List<VarianteSinRotacion> sinRotacion) {
}
