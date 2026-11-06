package pe.mypeinventario.consulta;

import java.util.List;

/**
 * Vista de solo lectura de un producto con sus variantes.
 *
 * @param id          identificador
 * @param tiendaId    tienda dueña del producto
 * @param nombre      nombre del producto
 * @param descripcion descripción, o {@code null}
 * @param categoria   nombre de la categoría
 * @param marca       nombre de la marca, o {@code null} si no tiene
 * @param activo      {@code true} si se puede vender
 * @param variantes   variantes del producto
 */
public record ProductoResumen(Long id, Long tiendaId, String nombre, String descripcion, String categoria,
                              String marca, boolean activo, List<VarianteResumen> variantes) {
}
