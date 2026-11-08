package pe.mypeinventario.consulta;

/**
 * El conteo físico de una variante dentro de una toma de inventario.
 *
 * @param varianteId   variante contada
 * @param sku          código interno de la variante
 * @param producto     nombre del producto
 * @param talla        talla, o {@code null}
 * @param color        color, o {@code null}
 * @param stockSistema unidades que el sistema tenía al momento de contar
 * @param stockContado unidades contadas
 */
public record ConteoResumen(Long varianteId, String sku, String producto, String talla, String color,
                            int stockSistema, int stockContado) {

    /** @return la variante en una frase corta, por ejemplo «M / Azul» */
    public String variante() {
        return TextoVariante.de(talla, color);
    }

    /** @return contado menos sistema: positivo si sobra, negativo si falta */
    public int diferencia() {
        return stockContado - stockSistema;
    }

    /** @return la diferencia con su signo, por ejemplo «+2», «-3» o «0» */
    public String diferenciaTexto() {
        return diferencia() > 0 ? "+" + diferencia() : String.valueOf(diferencia());
    }
}
