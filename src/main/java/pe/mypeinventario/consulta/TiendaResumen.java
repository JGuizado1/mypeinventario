package pe.mypeinventario.consulta;

/**
 * Vista de solo lectura de una tienda.
 *
 * @param id        identificador
 * @param nombre    nombre de la tienda
 * @param direccion dirección, o {@code null}
 * @param galeria   galería o centro comercial, o {@code null}
 */
public record TiendaResumen(Long id, String nombre, String direccion, String galeria) {
}
