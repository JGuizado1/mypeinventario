package pe.mypeinventario.web;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.mypeinventario.consulta.MovimientoResumen;
import pe.mypeinventario.consulta.ProductoResumen;
import pe.mypeinventario.consulta.VarianteResumen;
import pe.mypeinventario.exception.MypeInventarioException;
import pe.mypeinventario.fachada.AjusteFacade;

/** Ajustes manuales de stock de la tienda activa, con el historial de movimientos de la variante elegida. */
@Controller
@RequestMapping("/ajustes")
public class AjusteController {

    /**
     * La variante elegida junto con el producto al que pertenece, para mostrarla en la pantalla.
     *
     * @param producto producto al que pertenece la variante
     * @param variante variante elegida
     */
    public record Seleccion(ProductoResumen producto, VarianteResumen variante) {
    }

    private final AjusteFacade fachada;

    public AjusteController(AjusteFacade fachada) {
        this.fachada = fachada;
    }

    @GetMapping
    public String mostrar(@RequestParam(required = false) Long varianteId, HttpSession session, Model model) {
        FormularioAjuste form = FormularioAjuste.vacio();
        cargar(SesionWeb.de(session), varianteId, model);
        model.addAttribute("form", form);
        return "ajustes";
    }

    @PostMapping
    public String ajustar(@ModelAttribute("form") FormularioAjuste form, HttpSession session, Model model,
                          RedirectAttributes redireccion) {
        SesionWeb sesion = SesionWeb.de(session);
        Long varianteId = null;
        try {
            varianteId = Formularios.id(form.varianteId(), "producto");
            fachada.ajustar(sesion.exigirUsuario().id(), sesion.exigirTienda().id(), varianteId,
                    Formularios.tipoAjuste(form.tipo()), Formularios.cantidadObligatoria(form.cantidad()),
                    form.motivo());
            redireccion.addFlashAttribute("mensaje", "Ajuste registrado. El stock ya se actualizó.");
            return "redirect:/ajustes?varianteId=" + varianteId;
        } catch (MypeInventarioException e) {
            model.addAttribute("error", e.getMessage());
            cargar(sesion, varianteId, model);
            return "ajustes";
        }
    }

    private void cargar(SesionWeb sesion, Long varianteId, Model model) {
        Long comercianteId = sesion.exigirUsuario().id();
        Long tiendaId = sesion.exigirTienda().id();
        List<ProductoResumen> productos = fachada.productos(comercianteId, tiendaId);
        Optional<Seleccion> elegida = buscar(productos, varianteId);
        model.addAttribute("productos", productos);
        model.addAttribute("elegida", elegida.orElse(null));
        List<MovimientoResumen> historial = elegida.isPresent()
                ? fachada.historial(comercianteId, tiendaId, varianteId) : List.of();
        model.addAttribute("historial", historial);
    }

    private static Optional<Seleccion> buscar(List<ProductoResumen> productos, Long varianteId) {
        if (varianteId == null) {
            return Optional.empty();
        }
        return productos.stream()
                .flatMap(producto -> producto.variantes().stream()
                        .filter(variante -> variante.id().equals(varianteId))
                        .map(variante -> new Seleccion(producto, variante)))
                .findFirst();
    }
}
