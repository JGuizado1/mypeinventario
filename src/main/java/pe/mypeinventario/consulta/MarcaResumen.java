package pe.mypeinventario.consulta;

/**
 * Vista de solo lectura de una marca.
 *
 * @param id     identificador
 * @param nombre nombre de la marca
 */
public record MarcaResumen(Long id, String nombre) {
}
