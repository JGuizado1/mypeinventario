package pe.mypeinventario.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pe.mypeinventario.consulta.TiendaResumen;
import pe.mypeinventario.consulta.UsuarioSesion;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.fachada.InventarioFacade;
import pe.mypeinventario.service.dto.SolicitudTienda;

/** Lista, crea y elige la tienda con la que se trabaja. */
@Controller
public class TiendaController {

    private final InventarioFacade fachada;

    public TiendaController(InventarioFacade fachada) {
        this.fachada = fachada;
    }

    @GetMapping("/tiendas")
    public String listar(HttpSession session, Model model) {
        model.addAttribute("tiendas", fachada.tiendasDe(SesionWeb.de(session).exigirUsuario().id()));
        return "tiendas";
    }

    @PostMapping("/tiendas")
    public String crear(@RequestParam String nombre, @RequestParam(required = false) String direccion,
                        @RequestParam(required = false) String galeria, HttpSession session, Model model) {
        UsuarioSesion usuario = SesionWeb.de(session).exigirUsuario();
        try {
            fachada.crearTienda(new SolicitudTienda(usuario.id(), nombre, direccion, galeria));
            return "redirect:/tiendas";
        } catch (OperacionInvalidaException e) {
            model.addAttribute("tiendas", fachada.tiendasDe(usuario.id()));
            model.addAttribute("error", e.getMessage());
            model.addAttribute("nombre", nombre);
            model.addAttribute("direccion", direccion);
            model.addAttribute("galeria", galeria);
            return "tiendas";
        }
    }

    @PostMapping("/tiendas/{id}/elegir")
    public String elegir(@PathVariable Long id, HttpSession session) {
        SesionWeb sesion = SesionWeb.de(session);
        TiendaResumen tienda = fachada.tienda(sesion.exigirUsuario().id(), id);
        sesion.elegirTienda(tienda);
        return "redirect:/";
    }
}
