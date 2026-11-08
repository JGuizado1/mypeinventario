package pe.mypeinventario.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.mypeinventario.exception.MypeInventarioException;
import pe.mypeinventario.fachada.DevolucionFacade;

/** Devoluciones de clientes: se registran desde el detalle de cada venta. */
@Controller
public class DevolucionController {

    private final DevolucionFacade fachada;

    public DevolucionController(DevolucionFacade fachada) {
        this.fachada = fachada;
    }

    @PostMapping("/ventas/{ventaId}/lineas/{detalleId}/devolucion")
    public String devolver(@PathVariable Long ventaId, @PathVariable Long detalleId,
                           @RequestParam String cantidad, @RequestParam(required = false) String motivo,
                           HttpSession session, RedirectAttributes redireccion) {
        try {
            int unidades = Formularios.cantidadObligatoria(cantidad);
            fachada.devolver(SesionWeb.de(session).exigirUsuario().id(), ventaId, detalleId, unidades, motivo);
            redireccion.addFlashAttribute("mensaje", unidades == 1
                    ? "Devolución registrada: volvió 1 unidad al stock."
                    : "Devolución registrada: volvieron " + unidades + " unidades al stock.");
        } catch (MypeInventarioException e) {
            redireccion.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/ventas/" + ventaId;
    }
}
