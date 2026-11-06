package pe.mypeinventario.service.dto;

import pe.mypeinventario.exception.OperacionInvalidaException;

/**
 * Datos para crear la cuenta de un comerciante.
 *
 * @param nombre     nombre, obligatorio
 * @param correo     correo, obligatorio y con formato válido
 * @param celular    celular de 9 dígitos, o {@code null}
 * @param contrasena contraseña de al menos 8 caracteres
 */
public record SolicitudRegistro(String nombre, String correo, String celular, String contrasena) {

    private static final int LARGO_MINIMO = 8;

    public SolicitudRegistro {
        nombre = Validaciones.exigirTexto(nombre, "El nombre es obligatorio");
        correo = Validaciones.correoValido(correo);
        celular = Validaciones.celularOpcional(celular);
        if (contrasena == null || contrasena.length() < LARGO_MINIMO) {
            throw new OperacionInvalidaException(
                    "La contraseña debe tener al menos " + LARGO_MINIMO + " caracteres");
        }
    }

    /** No muestra la contraseña, para que no termine en un registro (log) por accidente. */
    @Override
    public String toString() {
        return "SolicitudRegistro[nombre=" + nombre + ", correo=" + correo + "]";
    }
}
