package pe.mypeinventario.service.dto;

import pe.mypeinventario.exception.OperacionInvalidaException;

/**
 * Datos para crear una tienda.
 *
 * @param comercianteId dueño de la tienda
 * @param nombre        nombre, obligatorio
 * @param direccion     dirección, o {@code null}
 * @param galeria       galería o centro comercial, o {@code null}
 */
public record SolicitudTienda(Long comercianteId, String nombre, String direccion, String galeria) {

    public SolicitudTienda {
        if (comercianteId == null) {
            throw new OperacionInvalidaException("El comerciante es obligatorio");
        }
        nombre = Validaciones.exigirTexto(nombre, "El nombre de la tienda es obligatorio");
        direccion = Validaciones.textoOpcional(direccion);
        galeria = Validaciones.textoOpcional(galeria);
    }
}
