package pe.mypeinventario.web;

import jakarta.servlet.http.HttpSession;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pe.mypeinventario.exception.MypeInventarioException;
import pe.mypeinventario.fachada.ReporteFacade;

/** Reportes de la tienda activa: ventas del periodo, más vendidos, categorías y productos parados. */
@Controller
@RequestMapping("/reportes")
public class ReporteController {

    private static final int DIAS_SIN_ROTACION = 30;
    private static final int MAS_VENDIDOS = 10;

    private final ReporteFacade fachada;
    private final Clock reloj;

    public ReporteController(ReporteFacade fachada, Clock reloj) {
        this.fachada = fachada;
        this.reloj = reloj;
    }

    @GetMapping
    public String ver(@RequestParam(required = false) String desde, @RequestParam(required = false) String hasta,
                      @RequestParam(required = false) String dias, @RequestParam(required = false) String limite,
                      HttpSession session, Model model) {
        SesionWeb sesion = SesionWeb.de(session);
        LocalDate hoy = LocalDate.now(reloj);
        model.addAttribute("hoy", hoy);
        model.addAttribute("hace7", hoy.minusDays(6));
        model.addAttribute("hace30", hoy.minusDays(29));
        model.addAttribute("inicioMes", hoy.withDayOfMonth(1));
        model.addAttribute("desde", desde == null || desde.isBlank() ? hoy.withDayOfMonth(1).toString() : desde);
        model.addAttribute("hasta", hasta == null || hasta.isBlank() ? hoy.toString() : hasta);
        model.addAttribute("dias", dias == null || dias.isBlank() ? String.valueOf(DIAS_SIN_ROTACION) : dias);
        model.addAttribute("limite", limite == null || limite.isBlank() ? String.valueOf(MAS_VENDIDOS) : limite);
        try {
            model.addAttribute("informe", fachada.informe(sesion.exigirUsuario().id(), sesion.exigirTienda().id(),
                    Formularios.fecha(desde, "inicial", hoy.withDayOfMonth(1)),
                    Formularios.fecha(hasta, "final", hoy),
                    Formularios.enteroConDefecto(dias, "número de días", DIAS_SIN_ROTACION),
                    Formularios.enteroConDefecto(limite, "límite de más vendidos", MAS_VENDIDOS)));
        } catch (MypeInventarioException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "reportes";
    }
}
