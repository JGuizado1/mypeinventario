package pe.mypeinventario.seguridad;

/** Cifra contraseñas y comprueba que una contraseña coincida con su hash. */
public interface CodificadorContrasena {

    /**
     * Cifra una contraseña. Dos llamadas con la misma contraseña dan hashes distintos.
     *
     * @param contrasena contraseña en texto plano
     * @return hash que se puede guardar
     */
    String codificar(String contrasena);

    /**
     * Comprueba una contraseña contra un hash guardado.
     *
     * @param contrasena contraseña en texto plano
     * @param hash       hash guardado
     * @return {@code true} si coinciden
     */
    boolean coincide(String contrasena, String hash);
}
