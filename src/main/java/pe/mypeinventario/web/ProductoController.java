package pe.mypeinventario.web;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.mypeinventario.consulta.ProductoResumen;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.fachada.CatalogoFacade;
import pe.mypeinventario.service.dto.SolicitudProducto;

/** Lista, crea y edita los productos de la tienda activa. */
@Controller
@RequestMapping("/productos")
public class ProductoController {

    private final CatalogoFacade fachada;

    public ProductoController(CatalogoFacade fachada) {
        this.fachada = fachada;
    }

    @GetMapping
    public String listar(@RequestParam(defaultValue = "false") boolean todos, HttpSession session, Model model) {
        SesionWeb sesion = SesionWeb.de(session);
        model.addAttribute("productos",
                fachada.productos(sesion.exigirUsuario().id(), sesion.exigirTienda().id(), !todos));
        model.addAttribute("todos", todos);
        return "productos";
    }

    @GetMapping("/nuevo")
    public String mostrarFormulario(Model model) {
        model.addAttribute("form", FormularioProducto.vacio());
        cargarListas(model);
        return "producto-nuevo";
    }

    @PostMapping
    public String crear(@ModelAttribute("form") FormularioProducto form, HttpSession session, Model model) {
        SesionWeb sesion = SesionWeb.de(session);
        try {
            ProductoResumen producto = fachada.registrarProducto(sesion.exigirUsuario().id(), new SolicitudProducto(
                    sesion.exigirTienda().id(), form.nombre(), form.descripcion(), form.categoriaId(),
                    form.marcaId(), List.of(Formularios.variante(form.sku(), form.codigoBarras(), form.talla(),
                    form.color(), form.precioCompra(), form.precioVenta(), form.stockMinimo()))));
            return "redirect:/productos/" + producto.id();
        } catch (OperacionInvalidaException e) {
            model.addAttribute("error", e.getMessage());
            cargarListas(model);
            return "producto-nuevo";
        }
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, HttpSession session, Model model) {
        model.addAttribute("producto", fachada.producto(SesionWeb.de(session).exigirUsuario().id(), id));
        model.addAttribute("form", FormularioVariante.vacio());
        return "producto";
    }

    @PostMapping("/{id}/variantes")
    public String agregarVariante(@PathVariable Long id, @ModelAttribute("form") FormularioVariante form,
                                  HttpSession session, Model model, RedirectAttributes redireccion) {
        Long comercianteId = SesionWeb.de(session).exigirUsuario().id();
        try {
            fachada.agregarVariante(comercianteId, id, Formularios.variante(form.sku(), form.codigoBarras(),
                    form.talla(), form.color(), form.precioCompra(), form.precioVenta(), form.stockMinimo()));
            redireccion.addFlashAttribute("mensaje", "Variante agregada.");
            return "redirect:/productos/" + id;
        } catch (OperacionInvalidaException e) {
            model.addAttribute("producto", fachada.producto(comercianteId, id));
            model.addAttribute("error", e.getMessage());
            return "producto";
        }
    }

    @PostMapping("/{id}/variantes/{varianteId}/precios")
    public String actualizarPrecios(@PathVariable Long id, @PathVariable Long varianteId,
                                    @RequestParam(required = false) String precioCompra,
                                    @RequestParam String precioVenta,
                                    HttpSession session, RedirectAttributes redireccion) {
        try {
            fachada.actualizarPrecios(SesionWeb.de(session).exigirUsuario().id(), id, varianteId,
                    Formularios.decimalOpcional(precioCompra, "precio de compra"),
                    Formularios.decimal(precioVenta, "precio de venta"));
            redireccion.addFlashAttribute("mensaje", "Precios actualizados.");
        } catch (OperacionInvalidaException e) {
            redireccion.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/productos/" + id;
    }

    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable Long id, @RequestParam boolean activo,
                                HttpSession session, RedirectAttributes redireccion) {
        fachada.cambiarEstado(SesionWeb.de(session).exigirUsuario().id(), id, activo);
        redireccion.addFlashAttribute("mensaje", activo ? "Producto activado." : "Producto desactivado.");
        return "redirect:/productos/" + id;
    }

    private void cargarListas(Model model) {
        model.addAttribute("categorias", fachada.categorias());
        model.addAttribute("marcas", fachada.marcas());
    }
}
