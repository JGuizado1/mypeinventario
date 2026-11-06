package pe.mypeinventario.service.dto;

import pe.mypeinventario.exception.OperacionInvalidaException;

/**
 * Datos para registrar un cliente de una tienda.
 *
 * @param tiendaId  tienda a la que pertenece
 * @param nombre    nombre, obligatorio
 * @param celular   celular de 9 dígitos, o {@code null}
 * @param documento DNI (8 dígitos) o RUC (11 dígitos), o {@code null}
 */
public record SolicitudCliente(Long tiendaId, String nombre, String celular, String documento) {

    public SolicitudCliente {
        if (tiendaId == null) {
            throw new OperacionInvalidaException("La tienda es obligatoria");
        }
        nombre = Validaciones.exigirTexto(nombre, "El nombre del cliente es obligatorio");
        celular = Validaciones.celularOpcional(celular);
        documento = Validaciones.textoOpcional(documento) == null
                ? null : Validaciones.digitosExactos(documento, "documento", 8, 11);
    }
}
