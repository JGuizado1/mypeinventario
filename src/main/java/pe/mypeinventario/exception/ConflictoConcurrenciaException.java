package pe.mypeinventario.exception;

/** Se lanza cuando otra operación modificó el mismo recurso al mismo tiempo. */
public class ConflictoConcurrenciaException extends MypeInventarioException {

    public ConflictoConcurrenciaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}