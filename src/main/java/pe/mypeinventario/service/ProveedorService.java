package pe.mypeinventario.service;

import java.util.List;
import pe.mypeinventario.consulta.ProveedorResumen;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.service.dto.SolicitudProveedor;

/** Proveedores mayoristas. Son compartidos por todas las tiendas. */
public interface ProveedorService {

    /**
     * Registra un proveedor. No puede repetirse ni el RUC ni el correo.
     *
     * @param solicitud datos del proveedor
     * @return el proveedor creado
     * @throws OperacionInvalidaException si el RUC o el correo ya existen
     */
    ProveedorResumen registrarProveedor(SolicitudProveedor solicitud);

    /** @return todos los proveedores, ordenados por razón social */
    List<ProveedorResumen> listarProveedores();
}
