package pe.mypeinventario.exception;

/** Se lanza cuando una operación incumple una regla de negocio o llega con datos inválidos. */
public class OperacionInvalidaException extends MypeInventarioException {

    public OperacionInvalidaException(String mensaje) {
        super(mensaje);
    }

    public OperacionInvalidaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
