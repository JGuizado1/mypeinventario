package pe.mypeinventario.service.dto;

import java.util.regex.Pattern;
import pe.mypeinventario.exception.OperacionInvalidaException;

/** Validaciones de texto que comparten las solicitudes. */
final class Validaciones {

    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s.]+\\.[^@\\s]+$");
    private static final Pattern CELULAR = Pattern.compile("\\d{9}");

    private Validaciones() {
    }

    static String exigirTexto(String texto, String mensaje) {
        if (texto == null || texto.isBlank()) {
            throw new OperacionInvalidaException(mensaje);
        }
        return texto.trim();
    }

    static String textoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    /** @return el correo sin espacios y en minúsculas, para que "A@x.pe" y "a@x.pe" sean el mismo */
    static String correoValido(String correo) {
        String limpio = exigirTexto(correo, "El correo es obligatorio").toLowerCase();
        if (!CORREO.matcher(limpio).matches()) {
            throw new OperacionInvalidaException("El correo no tiene un formato válido");
        }
        return limpio;
    }

    static String celularOpcional(String celular) {
        String limpio = textoOpcional(celular);
        if (limpio != null && !CELULAR.matcher(limpio).matches()) {
            throw new OperacionInvalidaException("El celular debe tener 9 dígitos");
        }
        return limpio;
    }

    static String digitosExactos(String valor, String nombre, int... largos) {
        String limpio = exigirTexto(valor, "El " + nombre + " es obligatorio");
        for (int largo : largos) {
            if (limpio.matches("\\d{" + largo + "}")) {
                return limpio;
            }
        }
        throw new OperacionInvalidaException("El " + nombre + " no tiene un formato válido");
    }
}
