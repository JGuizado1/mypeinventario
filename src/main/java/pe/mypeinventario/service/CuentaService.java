package pe.mypeinventario.service;

import pe.mypeinventario.consulta.UsuarioSesion;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.service.dto.SolicitudRegistro;

/** Registro e ingreso de comerciantes. */
public interface CuentaService {

    /**
     * Crea la cuenta de un comerciante. La contraseña se guarda cifrada.
     *
     * @param solicitud datos de la cuenta
     * @return los datos de sesión del nuevo comerciante
     * @throws OperacionInvalidaException si el correo ya está en uso
     */
    UsuarioSesion registrarComerciante(SolicitudRegistro solicitud);

    /**
     * Comprueba el correo y la contraseña de un comerciante.
     *
     * @param correo     correo de la cuenta
     * @param contrasena contraseña en texto plano
     * @return los datos de sesión del comerciante
     * @throws OperacionInvalidaException si el correo o la contraseña no son correctos; el
     *                                    mensaje es el mismo en ambos casos para no revelar
     *                                    qué correos existen
     */
    UsuarioSesion autenticar(String correo, String contrasena);
}
