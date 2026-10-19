package pe.mypeinventario.exception;

/** Se lanza cuando un recurso buscado por su identificador no existe. */
public class RecursoNoEncontradoException extends MypeInventarioException {

    /**
     * @param recurso nombre del recurso, por ejemplo "Variante"
     * @param id      identificador buscado
     */
    public RecursoNoEncontradoException(String recurso, Long id) {
        super(recurso + " con id " + id + " no existe");
    }
}
