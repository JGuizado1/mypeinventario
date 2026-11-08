package pe.mypeinventario.consulta;

import java.util.List;

/**
 * Lo que la web recibe al registrar una venta: la venta y los avisos de stock bajo que
 * dejó, para decírselo a la persona de inmediato.
 *
 * @param venta  la venta registrada
 * @param avisos frases como «Polo básico (POL-M-NEG): quedan 2, el mínimo es 5»; puede estar vacía
 */
public record ResultadoVenta(VentaResumen venta, List<String> avisos) {
}
