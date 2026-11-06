package pe.mypeinventario.consulta;

/**
 * Vista de solo lectura de una categoría.
 *
 * @param id          identificador
 * @param nombre      nombre de la categoría
 * @param descripcion descripción, o {@code null}
 * @param padreId     identificador de la categoría padre, o {@code null} si es raíz
 * @param padre       nombre de la categoría padre, o {@code null} si es raíz
 */
public record CategoriaResumen(Long id, String nombre, String descripcion, Long padreId, String padre) {
}
