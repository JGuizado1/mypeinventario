package pe.mypeinventario.service.dto;

import java.util.List;
import pe.mypeinventario.exception.OperacionInvalidaException;

/**
 * Datos para registrar un producto con sus variantes.
 *
 * @param tiendaId    tienda dueña del producto
 * @param nombre      nombre del producto, obligatorio
 * @param descripcion descripción, o {@code null}
 * @param categoriaId categoría, obligatoria
 * @param marcaId     marca, o {@code null} si no tiene
 * @param variantes   variantes del producto, al menos una
 */
public record SolicitudProducto(Long tiendaId, String nombre, String descripcion, Long categoriaId,
                                Long marcaId, List<SolicitudVariante> variantes) {

    public SolicitudProducto {
        if (tiendaId == null) {
            throw new OperacionInvalidaException("La tienda es obligatoria");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new OperacionInvalidaException("El nombre del producto es obligatorio");
        }
        nombre = nombre.trim();
        descripcion = descripcion == null || descripcion.isBlank() ? null : descripcion.trim();
        if (categoriaId == null) {
            throw new OperacionInvalidaException("La categoría es obligatoria");
        }
        if (variantes == null || variantes.isEmpty()) {
            throw new OperacionInvalidaException("Un producto debe tener al menos una variante");
        }
        variantes = List.copyOf(variantes);
    }
}
