package pe.mypeinventario.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import pe.mypeinventario.service.ClienteService;
import pe.mypeinventario.service.ConsultaCatalogoService;
import pe.mypeinventario.service.CuentaService;
import pe.mypeinventario.service.ProveedorService;
import pe.mypeinventario.service.TiendaService;

/** Recorre las pantallas de ventas, compras, clientes y proveedores con la aplicación real, sin navegador. */
@SpringBootTest
@Transactional
class WebVentasComprasTest {

    @Autowired private WebApplicationContext contexto;
    @Autowired private CuentaService cuentaService;
    @Autowired private TiendaService tiendaService;
    @Autowired private ConsultaCatalogoService consulta;
    @Autowired private ProveedorService proveedorService;
    @Autowired private ClienteService clienteService;

    private MockMvc mvc;

    @BeforeEach
    void prepararMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    private String cuerpo(ResultActions resultado) throws Exception {
        return resultado.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    /** Registra a un comerciante, le crea una tienda y la deja elegida. */
    private MockHttpSession comercianteConTienda(String correo) throws Exception {
        MockHttpSession sesion = new MockHttpSession();
        mvc.perform(post("/registro").session(sesion).param("nombre", "Rosa Quispe").param("correo", correo)
                .param("contrasena", "clave-segura-1")).andExpect(redirectedUrl("/tiendas"));
        mvc.perform(post("/tiendas").session(sesion).param("nombre", "Moda Rosa")).andExpect(redirectedUrl("/tiendas"));
        Long comercianteId = cuentaService.autenticar(correo, "clave-segura-1").id();
        Long tiendaId = tiendaService.listarTiendas(comercianteId).get(0).id();
        mvc.perform(post("/tiendas/{id}/elegir", tiendaId).session(sesion)).andExpect(redirectedUrl("/"));
        return sesion;
    }

    /** Crea una categoría y un producto (stock mínimo 5, venta a 29.90) y devuelve el id de su variante. */
    private Long crearVariante(MockHttpSession sesion) throws Exception {
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

    private Long crearProveedor(MockHttpSession sesion) throws Exception {
        mvc.perform(post("/proveedores").session(sesion).param("razonSocial", "Textiles Gamarra SAC")
                .param("ruc", "20123456789").param("contacto", "Luis Paredes")
                .param("correo", "ventas@textiles.pe")).andExpect(redirectedUrl("/proveedores"));
        return proveedorService.listarProveedores().stream()
                .filter(p -> p.ruc().equals("20123456789")).findFirst().orElseThrow().id();
    }

    /** Compra {@code cantidad} unidades de la variante a 12.00 y devuelve la dirección de la compra. */
    private String comprar(MockHttpSession sesion, Long proveedorId, Long varianteId, int cantidad) throws Exception {
        return mvc.perform(post("/compras").session(sesion).param("proveedorId", String.valueOf(proveedorId))
                        .param("cantidad-" + varianteId, String.valueOf(cantidad))
                        .param("costo-" + varianteId, "12.00"))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
    }

    @Test
    void sinTiendaElegidaLasPantallasDeOperacionesPidenElegirUna() throws Exception {
        MockHttpSession sesion = new MockHttpSession();
        mvc.perform(post("/registro").session(sesion).param("nombre", "Rosa").param("correo", "rosa@correo.pe")
                .param("contrasena", "clave-segura-1"));

        mvc.perform(get("/ventas").session(sesion)).andExpect(redirectedUrl("/tiendas"));
        mvc.perform(get("/ventas/nueva").session(sesion)).andExpect(redirectedUrl("/tiendas"));
        mvc.perform(get("/compras").session(sesion)).andExpect(redirectedUrl("/tiendas"));
        mvc.perform(get("/clientes").session(sesion)).andExpect(redirectedUrl("/tiendas"));
        mvc.perform(get("/proveedores").session(sesion)).andExpect(status().isOk());
        mvc.perform(get("/ventas")).andExpect(redirectedUrl("/login"));
    }

    @Test
    void lasListasVaciasInvitanAEmpezar() throws Exception {
        MockHttpSession sesion = comercianteConTienda("rosa@correo.pe");

        assertThat(cuerpo(mvc.perform(get("/ventas").session(sesion)).andExpect(status().isOk())))
                .contains("aún no registra ventas").contains("Nueva venta");
        assertThat(cuerpo(mvc.perform(get("/compras").session(sesion)).andExpect(status().isOk())))
                .contains("aún no registra compras").contains("Nueva compra");
        assertThat(cuerpo(mvc.perform(get("/ventas/nueva").session(sesion)).andExpect(status().isOk())))
                .contains("No hay productos activos");
        assertThat(cuerpo(mvc.perform(get("/compras/nueva").session(sesion)).andExpect(status().isOk())))
                .contains("registra un proveedor");
    }

    @Test
    void unaCompraSubeElStockYUnaVentaLoBajaYAvisaDelPocoStock() throws Exception {
        MockHttpSession sesion = comercianteConTienda("rosa@correo.pe");
        Long varianteId = crearVariante(sesion);
        Long proveedorId = crearProveedor(sesion);

        String destinoCompra = comprar(sesion, proveedorId, varianteId, 10);
        assertThat(destinoCompra).startsWith("/compras/");
        String compra = cuerpo(mvc.perform(get(destinoCompra).session(sesion)).andExpect(status().isOk()));
        assertThat(compra).contains("Textiles Gamarra SAC").contains("POL-M-AZU").contains("S/ 120.00")
                .contains("Compra registrada");
        assertThat(consulta.obtenerProducto(consulta.listarProductos(
                tiendaDeLaSesion(sesion), false).get(0).id()).variantes().get(0).stock()).isEqualTo(10);

        assertThat(cuerpo(mvc.perform(get("/ventas/nueva").session(sesion)).andExpect(status().isOk())))
                .contains("Polo básico").contains("POL-M-AZU").contains("Cliente de mostrador");

        String destinoVenta = mvc.perform(post("/ventas").session(sesion).param("medioPago", "YAPE")
                        .param("descuentoPorcentaje", "10").param("cantidad-" + varianteId, "8"))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        assertThat(destinoVenta).startsWith("/ventas/");

        // 8 x 29.90 = 239.20; con 10 % de descuento paga 215.28. Quedan 2 y el mínimo es 5.
        String venta = cuerpo(mvc.perform(get(destinoVenta).session(sesion)).andExpect(status().isOk()));
        assertThat(venta).contains("S/ 239.20").contains("S/ 215.28").contains("IGV incluido")
                .contains("Yape").contains("Poco stock después de esta venta")
                .contains("Polo básico (POL-M-AZU): quedan 2, el mínimo es 5");
        assertThat(cuerpo(mvc.perform(get("/ventas").session(sesion)))).contains("Yape").contains("S/ 215.28");
        assertThat(cuerpo(mvc.perform(get("/compras").session(sesion)))).contains("Textiles Gamarra SAC");
    }

    private Long tiendaDeLaSesion(MockHttpSession sesion) {
        return SesionWeb.de(sesion).exigirTienda().id();
    }

    @Test
    void unaVentaPuedeLlevarClienteYMuestraSuNombre() throws Exception {
        MockHttpSession sesion = comercianteConTienda("rosa@correo.pe");
        Long varianteId = crearVariante(sesion);
        comprar(sesion, crearProveedor(sesion), varianteId, 10);
        mvc.perform(post("/clientes").session(sesion).param("nombre", "Carmen Flores").param("documento", "12345678"))
                .andExpect(redirectedUrl("/clientes"));
        Long clienteId = clienteService.listarClientes(tiendaDeLaSesion(sesion)).get(0).id();

        String destino = mvc.perform(post("/ventas").session(sesion).param("clienteId", String.valueOf(clienteId))
                        .param("medioPago", "EFECTIVO").param("cantidad-" + varianteId, "1"))
                .andReturn().getResponse().getRedirectedUrl();

        assertThat(cuerpo(mvc.perform(get(destino).session(sesion)))).contains("Carmen Flores").contains("S/ 29.90");
    }

    @Test
    void unaVentaInvalidaMuestraElMotivoSinPerderLoEscrito() throws Exception {
        MockHttpSession sesion = comercianteConTienda("rosa@correo.pe");
        Long varianteId = crearVariante(sesion);
        comprar(sesion, crearProveedor(sesion), varianteId, 10);

        String sinCantidades = cuerpo(mvc.perform(post("/ventas").session(sesion).param("medioPago", "EFECTIVO"))
                .andExpect(status().isOk()));
        assertThat(sinCantidades).contains("Escribe la cantidad de al menos un producto");

        String sinStock = cuerpo(mvc.perform(post("/ventas").session(sesion).param("medioPago", "PLIN")
                .param("cantidad-" + varianteId, "50")).andExpect(status().isOk()));
        assertThat(sinStock).contains("Stock insuficiente").contains("value=\"50\"");

        String descuentoMalo = cuerpo(mvc.perform(post("/ventas").session(sesion).param("medioPago", "EFECTIVO")
                .param("descuentoPorcentaje", "150").param("cantidad-" + varianteId, "1")).andExpect(status().isOk()));
        assertThat(descuentoMalo).contains("porcentaje de descuento");

        String cantidadRara = cuerpo(mvc.perform(post("/ventas").session(sesion).param("medioPago", "EFECTIVO")
                .param("cantidad-" + varianteId, "dos")).andExpect(status().isOk()));
        assertThat(cantidadRara).contains("La cantidad debe ser un número entero");
        assertThat(consulta.obtenerProducto(consulta.listarProductos(tiendaDeLaSesion(sesion), false).get(0).id())
                .variantes().get(0).stock()).isEqualTo(10);
    }

    @Test
    void unaCompraInvalidaMuestraElMotivo() throws Exception {
        MockHttpSession sesion = comercianteConTienda("rosa@correo.pe");
        Long varianteId = crearVariante(sesion);
        Long proveedorId = crearProveedor(sesion);

        String sinProveedor = cuerpo(mvc.perform(post("/compras").session(sesion)
                .param("cantidad-" + varianteId, "5").param("costo-" + varianteId, "12.00")).andExpect(status().isOk()));
        assertThat(sinProveedor).contains("Elige un proveedor");

        String sinCosto = cuerpo(mvc.perform(post("/compras").session(sesion).param("proveedorId", String.valueOf(proveedorId))
                .param("cantidad-" + varianteId, "5")).andExpect(status().isOk()));
        assertThat(sinCosto).contains("El costo unitario es obligatorio");
    }

    @Test
    void nadiePuedeVerLasVentasNiLasComprasDeOtroComerciante() throws Exception {
        MockHttpSession rosa = comercianteConTienda("rosa@correo.pe");
        Long varianteId = crearVariante(rosa);
        String destinoCompra = comprar(rosa, crearProveedor(rosa), varianteId, 10);
        String destinoVenta = mvc.perform(post("/ventas").session(rosa).param("medioPago", "EFECTIVO")
                .param("cantidad-" + varianteId, "1")).andReturn().getResponse().getRedirectedUrl();
        MockHttpSession pedro = comercianteConTienda("pedro@correo.pe");

        mvc.perform(get(destinoVenta).session(pedro)).andExpect(status().isNotFound());
        mvc.perform(get(destinoCompra).session(pedro)).andExpect(status().isNotFound());
        assertThat(cuerpo(mvc.perform(get("/ventas").session(pedro)))).doesNotContain("S/ 29.90");
    }

    @Test
    void sePuedenRegistrarClientesYProveedoresYSeRechazanLosRepetidosOInvalidos() throws Exception {
        MockHttpSession sesion = comercianteConTienda("rosa@correo.pe");

        mvc.perform(post("/clientes").session(sesion).param("nombre", "Carmen Flores").param("documento", "12345678"))
                .andExpect(redirectedUrl("/clientes"));
        assertThat(cuerpo(mvc.perform(get("/clientes").session(sesion)))).contains("Carmen Flores").contains("Cliente registrado.");
        assertThat(cuerpo(mvc.perform(post("/clientes").session(sesion).param("nombre", "Otra Carmen")
                .param("documento", "12345678")).andExpect(status().isOk())))
                .contains("Ya existe un cliente").contains("Otra Carmen");

        crearProveedor(sesion);
        assertThat(cuerpo(mvc.perform(get("/proveedores").session(sesion)))).contains("Textiles Gamarra SAC");
        assertThat(cuerpo(mvc.perform(post("/proveedores").session(sesion).param("razonSocial", "Otro SAC")
                .param("ruc", "123").param("contacto", "Ana").param("correo", "ana@otro.pe"))
                .andExpect(status().isOk()))).contains("RUC").contains("Otro SAC");
    }
}
