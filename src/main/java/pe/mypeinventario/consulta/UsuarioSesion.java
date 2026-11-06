package pe.mypeinventario.consulta;

/**
 * Datos mínimos de quien inició sesión. Nunca incluye la contraseña ni su hash.
 *
 * @param id     identificador del comerciante
 * @param nombre nombre
 * @param correo correo con el que ingresa
 */
public record UsuarioSesion(Long id, String nombre, String correo) {
}
