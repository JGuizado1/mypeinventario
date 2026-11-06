package pe.mypeinventario.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import pe.mypeinventario.consulta.TiendaResumen;
import pe.mypeinventario.fachada.InventarioFacade;

/** Pantalla de inicio de la tienda activa. */
@Controller
public class InicioController {

    private final InventarioFacade fachada;

    public InicioController(InventarioFacade fachada) {
        this.fachada = fachada;
    }

    @GetMapping("/")
    public String inicio(HttpSession session, Model model) {
        SesionWeb sesion = SesionWeb.de(session);
        TiendaResumen activa = sesion.tienda().orElse(null);
        if (activa == null) {
            return "redirect:/tiendas";
        }
        model.addAttribute("resumen", fachada.resumenInicio(sesion.exigirUsuario().id(), activa.id()));
        return "inicio";
    }
}
