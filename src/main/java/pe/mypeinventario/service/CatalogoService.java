package pe.mypeinventario.service;

import java.math.BigDecimal;
import pe.mypeinventario.consulta.CategoriaResumen;
import pe.mypeinventario.consulta.ProductoResumen;
import pe.mypeinventario.domain.Marca;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.service.dto.SolicitudProducto;
import pe.mypeinventario.service.dto.SolicitudVariante;

/** Altas y cambios del catálogo: categorías, marcas, productos, variantes y precios. */
public interface CatalogoService {

    /**
     * Crea una categoría, raíz o hija de otra.
     *
     * @param nombre         nombre, obligatorio y sin repetirse (sin distinguir mayúsculas)
     * @param descripcion    descripción, o {@code null}
     * @param categoriaPadreId categoría padre, o {@code null} para una categoría raíz
     * @return la categoría creada
     * @throws OperacionInvalidaException   si el nombre está vacío o ya existe
     * @throws RecursoNoEncontradoException si la categoría padre no existe
     */
    CategoriaResumen crearCategoria(String nombre, String descripcion, Long categoriaPadreId);

    /**
     * Crea una marca.
     *
     * @param nombre nombre, obligatorio y sin repetirse (sin distinguir mayúsculas)
     * @return la marca creada
     * @throws OperacionInvalidaException si el nombre está vacío o ya existe
     */
    Marca crearMarca(String nombre);

    /**
     * Registra un producto con sus variantes en una tienda. El SKU y el código de barras
     * no pueden repetirse dentro de la tienda.
     *
     * @param solicitud datos del producto y sus variantes
     * @return el producto creado, con el stock de cada variante en cero
     * @throws OperacionInvalidaException   si algún SKU o código de barras ya existe
     * @throws RecursoNoEncontradoException si la tienda, la categoría o la marca no existen
     */
    ProductoResumen registrarProducto(SolicitudProducto solicitud);

    /**
     * Agrega una variante a un producto existente.
     *
     * @param productoId producto al que se agrega
     * @param solicitud  datos de la variante
     * @return el producto actualizado
     * @throws OperacionInvalidaException   si el SKU o el código de barras ya existe en la tienda
     * @throws RecursoNoEncontradoException si el producto no existe
     */
    ProductoResumen agregarVariante(Long productoId, SolicitudVariante solicitud);

    /**
     * Cambia los precios de una variante. Los movimientos y ventas ya registradas no se tocan:
     * cada venta guarda el costo y el precio con que se hizo.
     *
     * @param varianteId   variante a cambiar
     * @param precioCompra nuevo costo de compra, o {@code null} para dejarlo como está
     * @param precioVenta  nuevo precio de venta, mayor que cero
     * @throws OperacionInvalidaException   si algún precio es inválido
     * @throws RecursoNoEncontradoException si la variante no existe
     */
    void actualizarPrecios(Long varianteId, BigDecimal precioCompra, BigDecimal precioVenta);

    /**
     * Activa o desactiva un producto. Un producto inactivo conserva su historial.
     *
     * @param productoId producto a cambiar
     * @param activo     {@code true} para activarlo
     * @throws RecursoNoEncontradoException si el producto no existe
     */
    void cambiarEstadoProducto(Long productoId, boolean activo);
}
