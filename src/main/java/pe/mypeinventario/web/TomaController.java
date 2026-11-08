package pe.mypeinventario.web;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.mypeinventario.consulta.ConteoResumen;
import pe.mypeinventario.consulta.TomaResumen;
import pe.mypeinventario.exception.MypeInventarioException;
import pe.mypeinventario.fachada.TomaFacade;

/** Toma de inventario físico de la tienda activa: iniciarla, anotar lo contado y cerrarla. */
@Controller
@RequestMapping("/tomas")
public class TomaController {

    private final TomaFacade fachada;

    public TomaController(TomaFacade fachada) {
        this.fachada = fachada;
    }

    @GetMapping
    public String listar(HttpSession session, Model model) {
        SesionWeb sesion = SesionWeb.de(session);
        List<TomaResumen> tomas = fachada.tomas(sesion.exigirUsuario().id(), sesion.exigirTienda().id());
        model.addAttribute("tomas", tomas);
        model.addAttribute("enCurso", tomas.stream().filter(TomaResumen::enCurso).findFirst().orElse(null));
        return "tomas";
    }

    @PostMapping
    public String iniciar(HttpSession session, RedirectAttributes redireccion) {
        SesionWeb sesion = SesionWeb.de(session);
        try {
            TomaResumen toma = fachada.iniciar(sesion.exigirUsuario().id(), sesion.exigirTienda().id());
            redireccion.addFlashAttribute("mensaje", "Toma iniciada. Anota lo que cuentes de cada producto.");
            return "redirect:/tomas/" + toma.id();
        } catch (MypeInventarioException e) {
            redireccion.addFlashAttribute("error", e.getMessage());
            return "redirect:/tomas";
        }
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, HttpSession session, Model model) {
        cargar(SesionWeb.de(session), id, model);
        model.addAttribute("conteos", Map.of());
        return "toma";
    }

    @PostMapping("/{id}/conteos")
    public String anotar(@PathVariable Long id, @RequestParam Map<String, String> parametros,
                         HttpSession session, Model model, RedirectAttributes redireccion) {
        SesionWeb sesion = SesionWeb.de(session);
        try {
            int anotados = fachada.registrarConteos(sesion.exigirUsuario().id(), id, Formularios.conteos(parametros));
            redireccion.addFlashAttribute("mensaje",
                    anotados == 1 ? "Se anotó 1 conteo." : "Se anotaron " + anotados + " conteos.");
            return "redirect:/tomas/" + id;
        } catch (MypeInventarioException e) {
            model.addAttribute("error", e.getMessage());
            cargar(sesion, id, model);
            model.addAttribute("conteos", parametros);
            return "toma";
        }
    }

    @PostMapping("/{id}/cerrar")
    public String cerrar(@PathVariable Long id, HttpSession session, RedirectAttributes redireccion) {
        try {
            TomaResumen toma = fachada.cerrar(SesionWeb.de(session).exigirUsuario().id(), id);
            redireccion.addFlashAttribute("mensaje", toma.conDiferencia() == 0
                    ? "Toma cerrada. El stock coincidía con lo contado, no hubo ajustes."
                    : "Toma cerrada. Se ajustó el stock de " + toma.conDiferencia() + " variante(s).");
        } catch (MypeInventarioException e) {
            redireccion.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tomas/" + id;
    }

    private void cargar(SesionWeb sesion, Long tomaId, Model model) {
        Long comercianteId = sesion.exigirUsuario().id();
        TomaResumen toma = fachada.toma(comercianteId, tomaId);
        model.addAttribute("toma", toma);
        model.addAttribute("contados", toma.conteos().stream()
                .collect(Collectors.toMap(ConteoResumen::varianteId, Function.identity())));
        model.addAttribute("productos", toma.enCurso()
                ? fachada.productosParaContar(comercianteId, sesion.exigirTienda().id()) : List.of());
    }
}
