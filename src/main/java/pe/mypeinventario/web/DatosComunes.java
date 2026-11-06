package pe.mypeinventario.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import pe.mypeinventario.consulta.TiendaResumen;
import pe.mypeinventario.consulta.UsuarioSesion;

/** Pone en todas las páginas al usuario y la tienda activa, para el menú lateral. */
@ControllerAdvice
public class DatosComunes {

    @ModelAttribute("usuario")
    public UsuarioSesion usuario(HttpServletRequest request) {
        return SesionWeb.desde(request).usuario().orElse(null);
    }

    @ModelAttribute("tienda")
    public TiendaResumen tienda(HttpServletRequest request) {
        return SesionWeb.desde(request).tienda().orElse(null);
    }
}
