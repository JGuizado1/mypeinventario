package pe.mypeinventario.service.dto;

import java.math.BigDecimal;
import pe.mypeinventario.exception.OperacionInvalidaException;

/**
 * Datos de una variante nueva. El SKU se guarda sin espacios y en mayúsculas, para que
 * "zap-42" y "ZAP-42" se consideren el mismo código.
 *
 * @param sku          código interno, obligatorio
 * @param codigoBarras código de barras, o {@code null}
 * @param talla        talla, o {@code null}
 * @param color        color, o {@code null}
 * @param precioCompra costo de compra, o {@code null} si no se conoce
 * @param precioVenta  precio de venta, mayor que cero
 * @param stockMinimo  stock mínimo propio, o {@code null} para usar el de la tienda
 */
public record SolicitudVariante(String sku, String codigoBarras, String talla, String color,
                                BigDecimal precioCompra, BigDecimal precioVenta, Integer stockMinimo) {

    public SolicitudVariante {
        if (sku == null || sku.isBlank()) {
            throw new OperacionInvalidaException("El SKU es obligatorio");
        }
        sku = sku.trim().toUpperCase();
        codigoBarras = textoOpcional(codigoBarras);
        talla = textoOpcional(talla);
        color = textoOpcional(color);
        if (precioVenta == null || precioVenta.signum() <= 0) {
            throw new OperacionInvalidaException("El precio de venta debe ser mayor que cero");
        }
        if (precioCompra != null && precioCompra.signum() < 0) {
            throw new OperacionInvalidaException("El precio de compra no puede ser negativo");
        }
        if (stockMinimo != null && stockMinimo < 0) {
            throw new OperacionInvalidaException("El stock mínimo no puede ser negativo");
        }
    }

    private static String textoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
