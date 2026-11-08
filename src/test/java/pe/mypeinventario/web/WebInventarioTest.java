package pe.mypeinventario.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import pe.mypeinventario.service.ConsultaCatalogoService;
import pe.mypeinventario.service.ConsultaVentaService;
import pe.mypeinventario.service.CuentaService;
import pe.mypeinventario.service.ProveedorService;
import pe.mypeinventario.service.TiendaService;

/** Recorre las pantallas de ajustes, toma de inventario y devoluciones con la aplicación real, sin navegador. */
@SpringBootTest
@Transactional
class WebInventarioTest {

    @Autowired private WebApplicationContext contexto;
    @Autowired private CuentaService cuentaService;
    @Autowired private TiendaService tiendaService;
    @Autowired private ConsultaCatalogoService consulta;
    @Autowired private ProveedorService proveedorService;
    @Autowired private ConsultaVentaService consultaVenta;

    private MockMvc mvc;
    private PasosWeb pasos;

    @BeforeEach
    void prepararMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        pasos = new PasosWeb(mvc, cuentaService, tiendaService, consulta, proveedorService);
    }

    private int stock(MockHttpSession sesion) {
        return consulta.listarProductos(pasos.tiendaDe(sesion), false).get(0).variantes().get(0).stock();
    }

    private static String flash(org.springframework.test.web.servlet.ResultActions resultado, String clave) {
        Object valor = resultado.andReturn().getFlashMap().get(clave);
        return valor == null ? "" : valor.toString();
    }

    // ---------------------------------------------------------------- ajustes

    @Test
    void sinSesionOSinTiendaLasPantallasDeInventarioPidenEntrarOElegir() throws Exception {
        mvc.perform(get("/ajustes")).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/tomas")).andExpect(redirectedUrl("/login"));

        MockHttpSession sesion = new MockHttpSession();
        mvc.perform(post("/registro").session(sesion).param("nombre", "Rosa").param("correo", "rosa@correo.pe")
                .param("contrasena", "clave-segura-1"));
        mvc.perform(get("/ajustes").session(sesion)).andExpect(redirectedUrl("/tiendas"));
        mvc.perform(get("/tomas").session(sesion)).andExpect(redirectedUrl("/tiendas"));
    }

    @Test
    void unAjusteSumaOrestaStockYQuedaEnElHistorial() throws Exception {
        MockHttpSession sesion = pasos.comercianteConTienda("rosa@correo.pe");
        Long varianteId = pasos.productoConPocoStock(sesion);
        assertThat(stock(sesion)).isEqualTo(2);

        mvc.perform(post("/ajustes").session(sesion).param("varianteId", String.valueOf(varianteId))
                        .param("tipo", "SUMAR").param("cantidad", "4").param("motivo", "Encontré una caja"))
                .andExpect(redirectedUrl("/ajustes?varianteId=" + varianteId));
        mvc.perform(post("/ajustes").session(sesion).param("varianteId", String.valueOf(varianteId))
                        .param("tipo", "RESTAR").param("cantidad", "1").param("motivo", "Prenda manchada"))
                .andExpect(redirectedUrl("/ajustes?varianteId=" + varianteId));

        assertThat(stock(sesion)).isEqualTo(5);
        String pagina = pasos.pagina(sesion, "/ajustes?varianteId=" + varianteId);
        assertThat(pagina).contains("Encontré una caja", "Prenda manchada", "Ajuste (suma)", "Ajuste (resta)");
    }

    @Test
    void elAjusteExigeMotivoYNoDejaElStockEnNegativo() throws Exception {
        MockHttpSession sesion = pasos.comercianteConTienda("rosa@correo.pe");
        Long varianteId = pasos.productoConPocoStock(sesion);

        String sinMotivo = PasosWeb.cuerpo(mvc.perform(post("/ajustes").session(sesion)
                .param("varianteId", String.valueOf(varianteId)).param("tipo", "SUMAR").param("cantidad", "3")
                .param("motivo", "")).andExpect(status().isOk()));
        String demasiado = PasosWeb.cuerpo(mvc.perform(post("/ajustes").session(sesion)
                .param("varianteId", String.valueOf(varianteId)).param("tipo", "RESTAR").param("cantidad", "50")
                .param("motivo", "Prueba")).andExpect(status().isOk()));
        String sinCantidad = PasosWeb.cuerpo(mvc.perform(post("/ajustes").session(sesion)
                .param("varianteId", String.valueOf(varianteId)).param("tipo", "SUMAR").param("cantidad", "")
                .param("motivo", "Prueba")).andExpect(status().isOk()));

        assertThat(sinMotivo).contains("class=\"error\"");
        assertThat(demasiado).contains("Stock insuficiente");
        assertThat(sinCantidad).contains("class=\"error\"");
        assertThat(stock(sesion)).isEqualTo(2);
    }

    @Test
    void unaVarianteAjenaNoSePuedeAjustarNiVerSuHistorial() throws Exception {
        MockHttpSession rosa = pasos.comercianteConTienda("rosa@correo.pe");
        Long varianteDeRosa = pasos.productoConPocoStock(rosa);
        MockHttpSession pedro = pasos.comercianteConTienda("pedro@correo.pe");

        mvc.perform(get("/ajustes").session(pedro).param("varianteId", String.valueOf(varianteDeRosa)))
                .andExpect(status().isOk());
        mvc.perform(post("/ajustes").session(pedro).param("varianteId", String.valueOf(varianteDeRosa))
                .param("tipo", "SUMAR").param("cantidad", "9").param("motivo", "Intruso")).andExpect(status().isOk());

        assertThat(stock(rosa)).isEqualTo(2);
    }

    // ------------------------------------------------------------------- toma

    @Test
    void unaTomaCuentaCierraYAjustaElStockALoContado() throws Exception {
        MockHttpSession sesion = pasos.comercianteConTienda("rosa@correo.pe");
        Long varianteId = pasos.productoConPocoStock(sesion);

        String destino = mvc.perform(post("/tomas").session(sesion))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        assertThat(destino).startsWith("/tomas/");
        assertThat(pasos.pagina(sesion, destino)).contains("POL-M-AZU");

        mvc.perform(post(destino + "/conteos").session(sesion).param("conteo-" + varianteId, "6"))
                .andExpect(redirectedUrl(destino));
        assertThat(stock(sesion)).as("contar no toca el stock hasta cerrar").isEqualTo(2);

        var cierre = mvc.perform(post(destino + "/cerrar").session(sesion)).andExpect(redirectedUrl(destino));
        assertThat(flash(cierre, "mensaje")).contains("Toma cerrada", "1 variante");
        assertThat(stock(sesion)).isEqualTo(6);
        assertThat(pasos.pagina(sesion, "/tomas")).contains("Cerrada");
    }

    @Test
    void noSeAbreOtraTomaMientrasHayUnaEnCursoNiSeCierraSinConteos() throws Exception {
        MockHttpSession sesion = pasos.comercianteConTienda("rosa@correo.pe");
        pasos.productoConPocoStock(sesion);
        String destino = mvc.perform(post("/tomas").session(sesion))
                .andReturn().getResponse().getRedirectedUrl();

        var segunda = mvc.perform(post("/tomas").session(sesion)).andExpect(redirectedUrl("/tomas"));
        var sinConteos = mvc.perform(post(destino + "/cerrar").session(sesion)).andExpect(redirectedUrl(destino));

        assertThat(flash(segunda, "error")).contains("en curso");
        assertThat(flash(sinConteos, "error")).contains("sin conteos");
        assertThat(stock(sesion)).isEqualTo(2);
    }

    @Test
    void unConteoMalEscritoSeRechazaYLaPantallaSigueMostrandoLaToma() throws Exception {
        MockHttpSession sesion = pasos.comercianteConTienda("rosa@correo.pe");
        Long varianteId = pasos.productoConPocoStock(sesion);
        String destino = mvc.perform(post("/tomas").session(sesion)).andReturn().getResponse().getRedirectedUrl();

        String texto = PasosWeb.cuerpo(mvc.perform(post(destino + "/conteos").session(sesion)
                .param("conteo-" + varianteId, "abc")).andExpect(status().isOk()));
        String negativo = PasosWeb.cuerpo(mvc.perform(post(destino + "/conteos").session(sesion)
                .param("conteo-" + varianteId, "-1")).andExpect(status().isOk()));

        assertThat(texto).contains("número entero");
        assertThat(negativo).contains("negativo");
    }

    @Test
    void laTomaDeOtroComercianteNoExiste() throws Exception {
        MockHttpSession rosa = pasos.comercianteConTienda("rosa@correo.pe");
        Long varianteId = pasos.productoConPocoStock(rosa);
        String destino = mvc.perform(post("/tomas").session(rosa)).andReturn().getResponse().getRedirectedUrl();
        MockHttpSession pedro = pasos.comercianteConTienda("pedro@correo.pe");

        mvc.perform(get(destino).session(pedro)).andExpect(status().isNotFound());
        mvc.perform(post(destino + "/conteos").session(pedro).param("conteo-" + varianteId, "1"))
                .andExpect(status().isNotFound());
        var cierre = mvc.perform(post(destino + "/cerrar").session(pedro));
        assertThat(cierre.andReturn().getResponse().getStatus()).isIn(302, 404);
        assertThat(stock(rosa)).isEqualTo(2);
    }

    // ------------------------------------------------------------ devoluciones

    @Test
    void unaDevolucionReponeStockYLaLineaMuestraLoDevuelto() throws Exception {
        MockHttpSession sesion = pasos.comercianteConTienda("rosa@correo.pe");
        pasos.productoConPocoStock(sesion);
        String destinoVenta = pasos.vender(sesion, consulta.listarProductos(pasos.tiendaDe(sesion), false)
                .get(0).variantes().get(0).id(), 2);
        Long ventaId = Long.valueOf(destinoVenta.substring("/ventas/".length()));
        Long detalleId = consultaVenta.obtenerVenta(ventaId).lineas().get(0).detalleId();
        assertThat(stock(sesion)).isZero();

        var devolucion = mvc.perform(post("/ventas/{v}/lineas/{d}/devolucion", ventaId, detalleId).session(sesion)
                .param("cantidad", "1").param("motivo", "Talla equivocada")).andExpect(redirectedUrl(destinoVenta));

        assertThat(flash(devolucion, "mensaje")).contains("1 unidad");
        assertThat(stock(sesion)).isEqualTo(1);
        assertThat(consultaVenta.obtenerVenta(ventaId).lineas().get(0).devueltas()).isEqualTo(1);
        assertThat(pasos.pagina(sesion, destinoVenta)).contains("Devueltas");
    }

    @Test
    void noSePuedeDevolverMasDeLoVendido() throws Exception {
        MockHttpSession sesion = pasos.comercianteConTienda("rosa@correo.pe");
        Long varianteId = pasos.productoConPocoStock(sesion);
        String destinoVenta = pasos.vender(sesion, varianteId, 1);
        Long ventaId = Long.valueOf(destinoVenta.substring("/ventas/".length()));
        Long detalleId = consultaVenta.obtenerVenta(ventaId).lineas().get(0).detalleId();
        int antes = stock(sesion);

        var exceso = mvc.perform(post("/ventas/{v}/lineas/{d}/devolucion", ventaId, detalleId).session(sesion)
                .param("cantidad", "5").param("motivo", "x")).andExpect(redirectedUrl(destinoVenta));
        var vacia = mvc.perform(post("/ventas/{v}/lineas/{d}/devolucion", ventaId, detalleId).session(sesion)
                .param("cantidad", "").param("motivo", "x")).andExpect(redirectedUrl(destinoVenta));

        assertThat(flash(exceso, "error")).contains("Solo quedan 1");
        assertThat(flash(vacia, "error")).isNotEmpty();
        assertThat(stock(sesion)).isEqualTo(antes);
    }

    @Test
    void laVentaDeOtroComercianteNoSePuedeDevolver() throws Exception {
        MockHttpSession rosa = pasos.comercianteConTienda("rosa@correo.pe");
        Long varianteId = pasos.productoConPocoStock(rosa);
        String destinoVenta = pasos.vender(rosa, varianteId, 1);
        Long ventaId = Long.valueOf(destinoVenta.substring("/ventas/".length()));
        Long detalleId = consultaVenta.obtenerVenta(ventaId).lineas().get(0).detalleId();
        int antes = stock(rosa);
        MockHttpSession pedro = pasos.comercianteConTienda("pedro@correo.pe");

        var intento = mvc.perform(post("/ventas/{v}/lineas/{d}/devolucion", ventaId, detalleId).session(pedro)
                .param("cantidad", "1").param("motivo", "x"));

        assertThat(flash(intento, "error")).contains("Venta");
        assertThat(stock(rosa)).isEqualTo(antes);
    }
}
