package pe.mypeinventario.web;

/**
 * Lo que la persona escribe al ajustar el stock de una variante. Todo llega como texto.
 *
 * @param varianteId identificador de la variante a ajustar
 * @param tipo SUMAR o RESTAR
 * @param cantidad unidades a sumar o restar, como texto
 * @param motivo motivo del ajuste, obligatorio
 */
public record FormularioAjuste(String varianteId, String tipo, String cantidad, String motivo) {

    public static FormularioAjuste vacio() {
        return new FormularioAjuste("", "SUMAR", "", "");
    }
}
