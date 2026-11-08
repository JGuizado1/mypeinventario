package pe.mypeinventario.consulta;

/** Arma el texto corto de una variante a partir de su talla y su color. */
final class TextoVariante {

    private TextoVariante() {
    }

    /** @return «M / Azul», «M», «Azul» o «—» si no tiene ninguno de los dos */
    static String de(String talla, String color) {
        boolean hayTalla = talla != null && !talla.isBlank();
        boolean hayColor = color != null && !color.isBlank();
        if (hayTalla && hayColor) {
            return talla + " / " + color;
        }
        if (hayTalla) {
            return talla;
        }
        return hayColor ? color : "—";
    }
}
