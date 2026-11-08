package pe.mypeinventario.web;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import pe.mypeinventario.domain.MedioPago;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.service.dto.LineaCompra;
import pe.mypeinventario.service.dto.LineaVenta;
import pe.mypeinventario.service.dto.SolicitudVariante;

/**
 * Convierte el texto de los formularios en números y líneas. Un valor mal escrito se informa
 * como un error de la operación, con un mensaje que la persona entienda.
 */
final class Formularios {

    /** Cada cantidad llega como un campo llamado «cantidad-» seguido del id de la variante. */
    static final String CANTIDAD = "cantidad-";
    /** Cada costo de compra llega como un campo llamado «costo-» seguido del id de la variante. */
    static final String COSTO = "costo-";

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

    /** @return el identificador escrito, o {@code null} si el campo está vacío */
    static Long idOpcional(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(valor.trim());
        } catch (NumberFormatException e) {
            throw new OperacionInvalidaException("El " + campo + " no es válido");
        }
    }

    static Long id(String valor, String campo) {
        Long id = idOpcional(valor, campo);
        if (id == null) {
            throw new OperacionInvalidaException("Elige un " + campo);
        }
        return id;
    }

    static MedioPago medioPago(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new OperacionInvalidaException("Elige un medio de pago");
        }
        try {
            return MedioPago.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new OperacionInvalidaException("El medio de pago no es válido");
        }
    }

    static SolicitudVariante variante(String sku, String codigoBarras, String talla, String color,
                                      String precioCompra, String precioVenta, String stockMinimo) {
        return new SolicitudVariante(sku, codigoBarras, talla, color,
                decimalOpcional(precioCompra, "precio de compra"), decimal(precioVenta, "precio de venta"),
                enteroOpcional(stockMinimo, "stock mínimo"));
    }

    /** Arma las líneas de una venta con los campos «cantidad-ID» que tengan una cantidad escrita. */
    static List<LineaVenta> lineasVenta(Map<String, String> parametros) {
        return cantidades(parametros).entrySet().stream()
                .map(linea -> new LineaVenta(linea.getKey(), linea.getValue()))
                .toList();
    }

    /** Arma las líneas de una compra: cada cantidad escrita debe traer también su costo. */
    static List<LineaCompra> lineasCompra(Map<String, String> parametros) {
        return cantidades(parametros).entrySet().stream()
                .map(linea -> new LineaCompra(linea.getKey(), linea.getValue(),
                        decimal(parametros.get(COSTO + linea.getKey()), "costo unitario")))
                .toList();
    }

    /** Variantes con cantidad escrita (distinta de cero), ordenadas por identificador. */
    private static SortedMap<Long, Integer> cantidades(Map<String, String> parametros) {
        SortedMap<Long, Integer> cantidades = new TreeMap<>();
        parametros.forEach((nombre, valor) -> {
            if (!nombre.startsWith(CANTIDAD) || valor == null || valor.isBlank()) {
                return;
            }
            int cantidad = entero(valor);
            if (cantidad != 0) {
                cantidades.put(id(nombre.substring(CANTIDAD.length()), "producto"), cantidad);
            }
        });
        if (cantidades.isEmpty()) {
            throw new OperacionInvalidaException("Escribe la cantidad de al menos un producto");
        }
        return cantidades;
    }

    private static int entero(String valor) {
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            throw new OperacionInvalidaException("La cantidad debe ser un número entero");
        }
    }
}
