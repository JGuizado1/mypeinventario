package pe.mypeinventario.service.dto;

/**
 * Datos para registrar un proveedor mayorista.
 *
 * @param contacto    nombre de la persona de contacto, obligatorio
 * @param correo      correo de contacto, obligatorio y con formato válido
 * @param celular     celular de 9 dígitos, o {@code null}
 * @param razonSocial razón social, obligatoria
 * @param ruc         RUC de 11 dígitos, obligatorio
 */
public record SolicitudProveedor(String contacto, String correo, String celular,
                                 String razonSocial, String ruc) {

    public SolicitudProveedor {
        contacto = Validaciones.exigirTexto(contacto, "El contacto es obligatorio");
        correo = Validaciones.correoValido(correo);
        celular = Validaciones.celularOpcional(celular);
        razonSocial = Validaciones.exigirTexto(razonSocial, "La razón social es obligatoria");
        ruc = Validaciones.digitosExactos(ruc, "RUC", 11);
    }
}
