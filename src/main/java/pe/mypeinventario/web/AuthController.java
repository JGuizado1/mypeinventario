package pe.mypeinventario.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pe.mypeinventario.consulta.UsuarioSesion;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.fachada.InventarioFacade;
import pe.mypeinventario.service.dto.SolicitudRegistro;

/** Entrada, registro y salida de comerciantes. */
@Controller
public class AuthController {

    private final InventarioFacade fachada;

    public AuthController(InventarioFacade fachada) {
        this.fachada = fachada;
    }

    @GetMapping("/login")
    public String mostrarLogin(HttpServletRequest request) {
        return yaIngreso(request) ? "redirect:/" : "login";
    }

    @PostMapping("/login")
    public String ingresar(@RequestParam String correo, @RequestParam String contrasena,
                           HttpServletRequest request, Model model) {
        try {
            iniciarSesion(request, fachada.ingresar(correo, contrasena));
            return "redirect:/";
        } catch (OperacionInvalidaException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("correo", correo);
            return "login";
        }
    }

    @GetMapping("/registro")
    public String mostrarRegistro(HttpServletRequest request) {
        return yaIngreso(request) ? "redirect:/" : "registro";
    }

    @PostMapping("/registro")
    public String registrarse(@RequestParam String nombre, @RequestParam String correo,
                              @RequestParam(required = false) String celular, @RequestParam String contrasena,
                              HttpServletRequest request, Model model) {
        try {
            UsuarioSesion usuario = fachada.registrarse(new SolicitudRegistro(nombre, correo, celular, contrasena));
            iniciarSesion(request, usuario);
            return "redirect:/tiendas";
        } catch (OperacionInvalidaException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("nombre", nombre);
            model.addAttribute("correo", correo);
            model.addAttribute("celular", celular);
            return "registro";
        }
    }

    @PostMapping("/salir")
    public String salir(HttpSession session) {
        SesionWeb.de(session).cerrar();
        return "redirect:/login";
    }

    private static boolean yaIngreso(HttpServletRequest request) {
        return SesionWeb.desde(request).usuario().isPresent();
    }

    /** Cambia el identificador de la sesión al ingresar, para evitar que se reutilice uno ajeno. */
    private static void iniciarSesion(HttpServletRequest request, UsuarioSesion usuario) {
        HttpSession session = request.getSession(true);
        request.changeSessionId();
        SesionWeb.de(session).iniciar(usuario);
    }
}
