package pe.mypeinventario.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.mypeinventario.exception.MypeInventarioException;
import pe.mypeinventario.fachada.VentaFacade;
import pe.mypeinventario.service.dto.SolicitudCliente;

/** Los clientes de la tienda activa: lista y alta. */
@Controller
@RequestMapping("/clientes")
public class ClienteController {

    private final VentaFacade fachada;

    public ClienteController(VentaFacade fachada) {
        this.fachada = fachada;
    }

    @GetMapping
    public String listar(HttpSession session, Model model) {
        cargarClientes(SesionWeb.de(session), model);
        return "clientes";
    }

    @PostMapping
    public String crear(@RequestParam String nombre, @RequestParam(required = false) String celular,
                        @RequestParam(required = false) String documento,
                        HttpSession session, Model model, RedirectAttributes redireccion) {
        SesionWeb sesion = SesionWeb.de(session);
        try {
            fachada.registrarCliente(sesion.exigirUsuario().id(),
                    new SolicitudCliente(sesion.exigirTienda().id(), nombre, celular, documento));
            redireccion.addFlashAttribute("mensaje", "Cliente registrado.");
            return "redirect:/clientes";
        } catch (MypeInventarioException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("nombre", nombre);
            model.addAttribute("celular", celular);
            model.addAttribute("documento", documento);
            cargarClientes(sesion, model);
            return "clientes";
        }
    }

    private void cargarClientes(SesionWeb sesion, Model model) {
        model.addAttribute("clientes",
                fachada.clientes(sesion.exigirUsuario().id(), sesion.exigirTienda().id()));
    }
}
