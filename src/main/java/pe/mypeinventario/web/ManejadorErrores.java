package pe.mypeinventario.web;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import pe.mypeinventario.exception.MypeInventarioException;
import pe.mypeinventario.exception.RecursoNoEncontradoException;

/** Convierte las excepciones del negocio en una página de error clara. */
@ControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String noEncontrado(RecursoNoEncontradoException e, Model model) {
        model.addAttribute("titulo", "No encontramos lo que buscas");
        model.addAttribute("mensaje", e.getMessage());
        return "error";
    }

    @ExceptionHandler(MypeInventarioException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String operacionInvalida(MypeInventarioException e, Model model) {
        model.addAttribute("titulo", "No se pudo completar la operación");
        model.addAttribute("mensaje", e.getMessage());
        return "error";
    }
}
