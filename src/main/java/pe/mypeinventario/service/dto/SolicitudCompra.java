package pe.mypeinventario.service.dto;

import java.util.List;
import pe.mypeinventario.exception.OperacionInvalidaException;

/**
 * Datos para registrar una compra a un proveedor.
 *
 * @param tiendaId    tienda que compra
 * @param proveedorId proveedor mayorista
 * @param lineas      líneas de la compra, al menos una
 */
public record SolicitudCompra(Long tiendaId, Long proveedorId, List<LineaCompra> lineas) {

    public SolicitudCompra {
        if (tiendaId == null || proveedorId == null) {
            throw new OperacionInvalidaException("La tienda y el proveedor son obligatorios");
        }
        if (lineas == null || lineas.isEmpty()) {
            throw new OperacionInvalidaException("Una compra debe tener al menos una línea");
        }
        lineas = List.copyOf(lineas);
    }
}
