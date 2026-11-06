package pe.mypeinventario.consulta;

/**
 * Vista de solo lectura de un proveedor mayorista.
 *
 * @param id          identificador
 * @param razonSocial razón social
 * @param ruc         RUC de 11 dígitos
 * @param contacto    nombre de la persona de contacto
 * @param correo      correo de contacto
 * @param celular     celular, o {@code null}
 */
public record ProveedorResumen(Long id, String razonSocial, String ruc, String contacto,
                               String correo, String celular) {
}
