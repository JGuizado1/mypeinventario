package pe.mypeinventario.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.fachada.AlertaFacade;

/** Las alertas de reabastecimiento de la tienda activa: verlas y marcarlas como vistas o atendidas. */
@Controller
@RequestMapping("/alertas")
public class AlertaController {

    private final AlertaFacade fachada;

    public AlertaController(AlertaFacade fachada) {
        this.fachada = fachada;
    }

    @GetMapping
    public String listar(@RequestParam(defaultValue = "false") boolean atendidas, HttpSession session, Model model) {
        SesionWeb sesion = SesionWeb.de(session);
        Long comercianteId = sesion.exigirUsuario().id();
        Long tiendaId = sesion.exigirTienda().id();
        model.addAttribute("alertas", fachada.alertas(comercianteId, tiendaId, atendidas));
        model.addAttribute("avisos", fachada.avisosRecientes(comercianteId, tiendaId));
        model.addAttribute("atendidas", atendidas);
        return "alertas";
    }

    @PostMapping("/{id}/vista")
    public String marcarVista(@PathVariable Long id, HttpSession session, RedirectAttributes redireccion) {
        try {
            fachada.marcarVista(SesionWeb.de(session).exigirUsuario().id(), id);
            redireccion.addFlashAttribute("mensaje", "Alerta marcada como vista.");
        } catch (OperacionInvalidaException e) {
            redireccion.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/alertas";
    }

    @PostMapping("/{id}/atendida")
    public String marcarAtendida(@PathVariable Long id, HttpSession session, RedirectAttributes redireccion) {
        try {
            fachada.marcarAtendida(SesionWeb.de(session).exigirUsuario().id(), id);
            redireccion.addFlashAttribute("mensaje", "Alerta marcada como atendida.");
        } catch (OperacionInvalidaException e) {
            redireccion.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/alertas";
    }
}
