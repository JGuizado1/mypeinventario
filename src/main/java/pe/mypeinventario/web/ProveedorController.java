package pe.mypeinventario.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.mypeinventario.exception.MypeInventarioException;
import pe.mypeinventario.fachada.CompraFacade;
import pe.mypeinventario.service.dto.SolicitudProveedor;

/** Los proveedores mayoristas: lista y alta. Son compartidos por todas las tiendas. */
@Controller
@RequestMapping("/proveedores")
public class ProveedorController {

    private final CompraFacade fachada;

    public ProveedorController(CompraFacade fachada) {
        this.fachada = fachada;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("proveedores", fachada.proveedores());
        return "proveedores";
    }

    @PostMapping
    public String crear(@RequestParam String razonSocial, @RequestParam String ruc,
                        @RequestParam String contacto, @RequestParam String correo,
                        @RequestParam(required = false) String celular,
                        Model model, RedirectAttributes redireccion) {
        try {
            fachada.registrarProveedor(new SolicitudProveedor(contacto, correo, celular, razonSocial, ruc));
            redireccion.addFlashAttribute("mensaje", "Proveedor registrado.");
            return "redirect:/proveedores";
        } catch (MypeInventarioException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("razonSocial", razonSocial);
            model.addAttribute("ruc", ruc);
            model.addAttribute("contacto", contacto);
            model.addAttribute("correo", correo);
            model.addAttribute("celular", celular);
            model.addAttribute("proveedores", fachada.proveedores());
            return "proveedores";
        }
    }
}
