package pe.mypeinventario.web;

import java.math.BigDecimal;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.service.dto.SolicitudVariante;

/**
 * Convierte el texto de los formularios en números. Un valor mal escrito se informa como
 * un error de la operación, con un mensaje que la persona entienda.
 */
final class Formularios {

    private Formularios() {
    }

    static BigDecimal decimal(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new OperacionInvalidaException("El " + campo + " es obligatorio");
        }
        try {
            return new BigDecimal(valor.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new OperacionInvalidaException("El " + campo + " no es un número válido");
        }
    }

    static BigDecimal decimalOpcional(String valor, String campo) {
        return valor == null || valor.isBlank() ? null : decimal(valor, campo);
    }

    static Integer enteroOpcional(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(valor.trim());
        } catch (NumberFormatException e) {
            throw new OperacionInvalidaException("El " + campo + " debe ser un número entero");
        }
    }

    static SolicitudVariante variante(String sku, String codigoBarras, String talla, String color,
                                      String precioCompra, String precioVenta, String stockMinimo) {
        return new SolicitudVariante(sku, codigoBarras, talla, color,
                decimalOpcional(precioCompra, "precio de compra"), decimal(precioVenta, "precio de venta"),
                enteroOpcional(stockMinimo, "stock mínimo"));
    }
}
