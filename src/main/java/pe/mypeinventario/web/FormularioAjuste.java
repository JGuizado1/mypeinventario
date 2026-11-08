package pe.mypeinventario.web;

/** Lo que la persona escribe al ajustar el stock de una variante. Todo llega como texto. */
public record FormularioAjuste(String varianteId, String tipo, String cantidad, String motivo) {

    public static FormularioAjuste vacio() {
        return new FormularioAjuste("", "SUMAR", "", "");
    }
}
