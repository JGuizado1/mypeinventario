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
import pe.mypeinventario.consulta.ResultadoVenta;
import pe.mypeinventario.domain.MedioPago;
import pe.mypeinventario.exception.MypeInventarioException;
import pe.mypeinventario.fachada.VentaFacade;
import pe.mypeinventario.service.dto.SolicitudVenta;

/** Lista las ventas de la tienda activa, registra una nueva y muestra su detalle. */
@Controller
@RequestMapping("/ventas")
public class VentaController {

    private final VentaFacade fachada;

    public VentaController(VentaFacade fachada) {
        this.fachada = fachada;
    }

    @GetMapping
    public String listar(HttpSession session, Model model) {
        SesionWeb sesion = SesionWeb.de(session);
        model.addAttribute("ventas", fachada.ventas(sesion.exigirUsuario().id(), sesion.exigirTienda().id()));
        return "ventas";
    }

    @GetMapping("/nueva")
    public String mostrarFormulario(HttpSession session, Model model) {
        model.addAttribute("form", FormularioVenta.vacio());
        model.addAttribute("cantidades", Map.of());
        cargarOpciones(SesionWeb.de(session), model);
        return "venta-nueva";
    }

    @PostMapping
    public String registrar(@ModelAttribute("form") FormularioVenta form,
                            @RequestParam Map<String, String> parametros,
                            HttpSession session, Model model, RedirectAttributes redireccion) {
        SesionWeb sesion = SesionWeb.de(session);
        try {
            SolicitudVenta solicitud = new SolicitudVenta(sesion.exigirTienda().id(),
                    Formularios.idOpcional(form.clienteId(), "cliente"),
                    Formularios.medioPago(form.medioPago()),
                    Formularios.lineasVenta(parametros),
                    Formularios.decimalOpcional(form.descuentoPorcentaje(), "descuento porcentual"),
                    Formularios.decimalOpcional(form.descuentoMonto(), "descuento en soles"));
            ResultadoVenta resultado = fachada.registrarVenta(sesion.exigirUsuario().id(), solicitud);
            redireccion.addFlashAttribute("mensaje",
                    "Venta registrada por S/ " + resultado.venta().total().toPlainString() + ".");
            redireccion.addFlashAttribute("avisos", resultado.avisos());
            return "redirect:/ventas/" + resultado.venta().id();
        } catch (MypeInventarioException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("cantidades", parametros);
            cargarOpciones(sesion, model);
            return "venta-nueva";
        }
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, HttpSession session, Model model) {
        model.addAttribute("venta", fachada.venta(SesionWeb.de(session).exigirUsuario().id(), id));
        return "venta";
    }

    private void cargarOpciones(SesionWeb sesion, Model model) {
        Long comercianteId = sesion.exigirUsuario().id();
        Long tiendaId = sesion.exigirTienda().id();
        model.addAttribute("productos", fachada.productosParaVender(comercianteId, tiendaId));
        model.addAttribute("clientes", fachada.clientes(comercianteId, tiendaId));
        model.addAttribute("medios", MedioPago.values());
    }
}
