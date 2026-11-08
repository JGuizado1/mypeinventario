package pe.mypeinventario.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import pe.mypeinventario.service.ConsultaCatalogoService;
import pe.mypeinventario.service.CuentaService;
import pe.mypeinventario.service.ProveedorService;
import pe.mypeinventario.service.TiendaService;

/**
 * Pasos comunes de las pruebas de pantallas: dejar listo a un comerciante con su tienda,
 * un producto con stock y vender. Así cada prueba cuenta solo lo que le importa.
 */
final class PasosWeb {

    private final MockMvc mvc;
    private final CuentaService cuentaService;
    private final TiendaService tiendaService;
    private final ConsultaCatalogoService consulta;
    private final ProveedorService proveedorService;

    PasosWeb(MockMvc mvc, CuentaService cuentaService, TiendaService tiendaService,
             ConsultaCatalogoService consulta, ProveedorService proveedorService) {
        this.mvc = mvc;
        this.cuentaService = cuentaService;
        this.tiendaService = tiendaService;
        this.consulta = consulta;
        this.proveedorService = proveedorService;
    }

    /** El cuerpo de la respuesta como texto. */
    static String cuerpo(ResultActions resultado) throws Exception {
        return resultado.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    /** Pide una dirección y devuelve el texto de la página. */
    String pagina(MockHttpSession sesion, String direccion) throws Exception {
        return cuerpo(mvc.perform(get(direccion).session(sesion)).andExpect(status().isOk()));
    }

    /** Registra a un comerciante, le crea una tienda y la deja elegida. */
    MockHttpSession comercianteConTienda(String correo) throws Exception {
        MockHttpSession sesion = new MockHttpSession();
        mvc.perform(post("/registro").session(sesion).param("nombre", "Rosa Quispe").param("correo", correo)
                .param("contrasena", "clave-segura-1")).andExpect(redirectedUrl("/tiendas"));
        mvc.perform(post("/tiendas").session(sesion).param("nombre", "Moda Rosa")).andExpect(redirectedUrl("/tiendas"));
        Long comercianteId = cuentaService.autenticar(correo, "clave-segura-1").id();
        Long tiendaId = tiendaService.listarTiendas(comercianteId).get(0).id();
        mvc.perform(post("/tiendas/{id}/elegir", tiendaId).session(sesion)).andExpect(redirectedUrl("/"));
        return sesion;
    }

    /** @return el identificador de la tienda elegida en la sesión */
    Long tiendaDe(MockHttpSession sesion) {
        return SesionWeb.de(sesion).exigirTienda().id();
    }

    /** Crea la categoría «Polos» y un producto (stock mínimo 5, costo 12.00, venta 29.90); devuelve su variante. */
    Long crearVariante(MockHttpSession sesion) throws Exception {
        mvc.perform(post("/catalogo/categorias").session(sesion).param("nombre", "Polos"))
                .andExpect(redirectedUrl("/catalogo"));
        Long categoriaId = consulta.listarCategorias().stream()
                .filter(c -> c.nombre().equals("Polos")).findFirst().orElseThrow().id();
        String destino = mvc.perform(post("/productos").session(sesion).param("nombre", "Polo básico")
                        .param("categoriaId", String.valueOf(categoriaId)).param("sku", "POL-M-AZU")
                        .param("talla", "M").param("color", "Azul").param("precioCompra", "12.00")
                        .param("precioVenta", "29.90").param("stockMinimo", "5"))
                .andReturn().getResponse().getRedirectedUrl();
        Long productoId = Long.valueOf(destino.substring("/productos/".length()));
        return consulta.obtenerProducto(productoId).variantes().get(0).id();
    }

    Long crearProveedor(MockHttpSession sesion) throws Exception {
        mvc.perform(post("/proveedores").session(sesion).param("razonSocial", "Textiles Gamarra SAC")
                .param("ruc", "20123456789").param("contacto", "Luis Paredes")
                .param("correo", "ventas@textiles.pe")).andExpect(redirectedUrl("/proveedores"));
        return proveedorService.listarProveedores().stream()
                .filter(p -> p.ruc().equals("20123456789")).findFirst().orElseThrow().id();
    }

    /** Compra unidades de la variante a 12.00 cada una; devuelve la dirección de la compra. */
    String comprar(MockHttpSession sesion, Long proveedorId, Long varianteId, int cantidad) throws Exception {
        return mvc.perform(post("/compras").session(sesion).param("proveedorId", String.valueOf(proveedorId))
                        .param("cantidad-" + varianteId, String.valueOf(cantidad))
                        .param("costo-" + varianteId, "12.00"))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
    }

    /** Vende unidades de la variante en efectivo y sin descuento; devuelve la dirección de la venta. */
    String vender(MockHttpSession sesion, Long varianteId, int cantidad) throws Exception {
        return mvc.perform(post("/ventas").session(sesion).param("medioPago", "EFECTIVO")
                        .param("cantidad-" + varianteId, String.valueOf(cantidad)))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
    }

    /** Deja un producto con 10 unidades, vende 8 (queda con 2, bajo el mínimo de 5) y devuelve su variante. */
    Long productoConPocoStock(MockHttpSession sesion) throws Exception {
        Long varianteId = crearVariante(sesion);
        comprar(sesion, crearProveedor(sesion), varianteId, 10);
        vender(sesion, varianteId, 8);
        return varianteId;
    }
}
