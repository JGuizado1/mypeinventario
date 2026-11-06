package pe.mypeinventario.consulta;

/**
 * Vista de solo lectura de un cliente.
 *
 * @param id        identificador
 * @param nombre    nombre
 * @param celular   celular, o {@code null}
 * @param documento DNI o RUC, o {@code null}
 */
public record ClienteResumen(Long id, String nombre, String celular, String documento) {
}
