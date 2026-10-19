package pe.mypeinventario.exception;

public abstract class MypeInventarioException extends RuntimeException {
    protected MypeInventarioException(String mensaje) {
        super(mensaje);
    }

    protected MypeInventarioException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
