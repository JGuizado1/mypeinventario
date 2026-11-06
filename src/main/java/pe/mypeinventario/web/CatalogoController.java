package pe.mypeinventario.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.fachada.CatalogoFacade;

/** Categorías y marcas: el catálogo compartido que usan los productos. */
@Controller
public class CatalogoController {

    private final CatalogoFacade fachada;

    public CatalogoController(CatalogoFacade fachada) {
        this.fachada = fachada;
    }

    @GetMapping("/catalogo")
    public String mostrar(Model model) {
        cargarListas(model);
        return "catalogo";
    }

    @PostMapping("/catalogo/categorias")
    public String crearCategoria(@RequestParam String nombre, @RequestParam(required = false) String descripcion,
                                 @RequestParam(required = false) Long padreId,
                                 Model model, RedirectAttributes redireccion) {
        try {
            fachada.crearCategoria(nombre, descripcion, padreId);
            redireccion.addFlashAttribute("mensaje", "Categoría creada.");
            return "redirect:/catalogo";
        } catch (OperacionInvalidaException e) {
            model.addAttribute("errorCategoria", e.getMessage());
            model.addAttribute("nombreCategoria", nombre);
            model.addAttribute("descripcionCategoria", descripcion);
            cargarListas(model);
            return "catalogo";
        }
    }

    @PostMapping("/catalogo/marcas")
    public String crearMarca(@RequestParam String nombre, Model model, RedirectAttributes redireccion) {
        try {
            fachada.crearMarca(nombre);
            redireccion.addFlashAttribute("mensaje", "Marca creada.");
            return "redirect:/catalogo";
        } catch (OperacionInvalidaException e) {
            model.addAttribute("errorMarca", e.getMessage());
            model.addAttribute("nombreMarca", nombre);
            cargarListas(model);
            return "catalogo";
        }
    }

    private void cargarListas(Model model) {
        model.addAttribute("categorias", fachada.categorias());
        model.addAttribute("marcas", fachada.marcas());
    }
}
