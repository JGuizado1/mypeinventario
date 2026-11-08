package pe.mypeinventario.web;

import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.mypeinventario.consulta.CompraResumen;
import pe.mypeinventario.exception.MypeInventarioException;
import pe.mypeinventario.fachada.CompraFacade;
import pe.mypeinventario.service.dto.SolicitudCompra;

/** Lista las compras de la tienda activa, registra una nueva y muestra su detalle. */
@Controller
@RequestMapping("/compras")
public class CompraController {

    private final CompraFacade fachada;

    public CompraController(CompraFacade fachada) {
        this.fachada = fachada;
    }

    @GetMapping
    public String listar(HttpSession session, Model model) {
        SesionWeb sesion = SesionWeb.de(session);
        model.addAttribute("compras", fachada.compras(sesion.exigirUsuario().id(), sesion.exigirTienda().id()));
        return "compras";
    }

    @GetMapping("/nueva")
    public String mostrarFormulario(HttpSession session, Model model) {
        model.addAttribute("form", FormularioCompra.vacio());
        model.addAttribute("campos", Map.of());
        cargarOpciones(SesionWeb.de(session), model);
        return "compra-nueva";
    }

    @PostMapping
    public String registrar(@ModelAttribute("form") FormularioCompra form,
                            @RequestParam Map<String, String> parametros,
                            HttpSession session, Model model, RedirectAttributes redireccion) {
        SesionWeb sesion = SesionWeb.de(session);
        try {
            SolicitudCompra solicitud = new SolicitudCompra(sesion.exigirTienda().id(),
                    Formularios.id(form.proveedorId(), "proveedor"),
                    Formularios.lineasCompra(parametros));
            CompraResumen compra = fachada.registrarCompra(sesion.exigirUsuario().id(), solicitud);
            redireccion.addFlashAttribute("mensaje",
                    "Compra registrada por S/ " + compra.total().toPlainString() + ". El stock ya se actualizó.");
            return "redirect:/compras/" + compra.id();
        } catch (MypeInventarioException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("campos", parametros);
            cargarOpciones(sesion, model);
            return "compra-nueva";
        }
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, HttpSession session, Model model) {
        model.addAttribute("compra", fachada.compra(SesionWeb.de(session).exigirUsuario().id(), id));
        return "compra";
    }

    private void cargarOpciones(SesionWeb sesion, Model model) {
        model.addAttribute("productos",
                fachada.productosParaComprar(sesion.exigirUsuario().id(), sesion.exigirTienda().id()));
        model.addAttribute("proveedores", fachada.proveedores());
    }
}
