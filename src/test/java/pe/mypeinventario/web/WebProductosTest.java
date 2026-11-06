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
import pe.mypeinventario.service.ConsultaCatalogoService;
import pe.mypeinventario.service.CuentaService;
import pe.mypeinventario.service.TiendaService;

/** Recorre las pantallas de productos y del catálogo con la aplicación real, sin navegador. */
@SpringBootTest
@Transactional
class WebProductosTest {

    @Autowired private WebApplicationContext contexto;
    @Autowired private CuentaService cuentaService;
    @Autowired private TiendaService tiendaService;
    @Autowired private ConsultaCatalogoService consulta;

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

    private Long crearCategoria(MockHttpSession sesion, String nombre) throws Exception {
        mvc.perform(post("/catalogo/categorias").session(sesion).param("nombre", nombre))
                .andExpect(redirectedUrl("/catalogo"));
        return consulta.listarCategorias().stream().filter(c -> c.nombre().equals(nombre)).findFirst().orElseThrow().id();
    }

    /** Crea un producto con su primera variante y devuelve su identificador. */
    private Long crearProducto(MockHttpSession sesion, Long categoriaId, String nombre, String sku) throws Exception {
        String destino = mvc.perform(post("/productos").session(sesion).param("nombre", nombre)
                        .param("categoriaId", String.valueOf(categoriaId)).param("sku", sku)
                        .param("talla", "M").param("color", "Azul").param("precioCompra", "12.00")
                        .param("precioVenta", "29.90").param("stockMinimo", "5"))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        assertThat(destino).startsWith("/productos/");
        return Long.valueOf(destino.substring("/productos/".length()));
    }

    @Test
    void sinTiendaElegidaLasPantallasDeProductosPidenElegirUna() throws Exception {
        MockHttpSession sesion = new MockHttpSession();
        mvc.perform(post("/registro").session(sesion).param("nombre", "Rosa").param("correo", "rosa@correo.pe")
                .param("contrasena", "clave-segura-1"));

        mvc.perform(get("/productos").session(sesion)).andExpect(redirectedUrl("/tiendas"));
        mvc.perform(get("/productos/nuevo").session(sesion)).andExpect(redirectedUrl("/tiendas"));
        mvc.perform(get("/productos")).andExpect(redirectedUrl("/login"));
    }

    @Test
    void laListaVaciaInvitaARegistrarElPrimerProducto() throws Exception {
        MockHttpSession sesion = comercianteConTienda("rosa@correo.pe");

        assertThat(cuerpo(mvc.perform(get("/productos").session(sesion)).andExpect(status().isOk())))
                .contains("aún no tiene productos").contains("Nuevo producto");
    }

    @Test
    void sePuedeCrearUnaCategoriaYUnaMarcaYVerlasEnElCatalogo() throws Exception {
        MockHttpSession sesion = comercianteConTienda("rosa@correo.pe");
        crearCategoria(sesion, "Polos");
        mvc.perform(post("/catalogo/marcas").session(sesion).param("nombre", "Gamarra Style"))
                .andExpect(redirectedUrl("/catalogo"));

        String pantalla = cuerpo(mvc.perform(get("/catalogo").session(sesion)).andExpect(status().isOk()));
        assertThat(pantalla).contains("Polos").contains("Gamarra Style");
    }

    @Test
    void unaCategoriaRepetidaMuestraElMotivo() throws Exception {
        MockHttpSession sesion = comercianteConTienda("rosa@correo.pe");
        crearCategoria(sesion, "Polos");

        ResultActions pantalla = mvc.perform(post("/catalogo/categorias").session(sesion).param("nombre", "polos"))
                .andExpect(status().isOk());

        assertThat(cuerpo(pantalla)).contains("Ya existe una categoría");
    }

    @Test
    void elRecorridoCompletoDeUnProducto() throws Exception {
        MockHttpSession sesion = comercianteConTienda("rosa@correo.pe");
        Long categoriaId = crearCategoria(sesion, "Polos");

        assertThat(cuerpo(mvc.perform(get("/productos/nuevo").session(sesion)).andExpect(status().isOk())))
                .contains("Polos").contains("Primera variante");

        Long productoId = crearProducto(sesion, categoriaId, "Polo básico", "pol-m-azu");
        String detalle = cuerpo(mvc.perform(get("/productos/{id}", productoId).session(sesion)).andExpect(status().isOk()));
        assertThat(detalle).contains("Polo básico").contains("POL-M-AZU").contains("Poco stock");

        assertThat(cuerpo(mvc.perform(get("/productos").session(sesion))))
                .contains("Polo básico").contains("Poco stock");
    }

    @Test
    void sePuedeAgregarUnaVarianteYActualizarPrecios() throws Exception {
        MockHttpSession sesion = comercianteConTienda("rosa@correo.pe");
        Long productoId = crearProducto(sesion, crearCategoria(sesion, "Polos"), "Polo básico", "POL-M-AZU");

        mvc.perform(post("/productos/{id}/variantes", productoId).session(sesion).param("sku", "POL-L-AZU")
                        .param("talla", "L").param("precioVenta", "31.50"))
                .andExpect(redirectedUrl("/productos/" + productoId));
        String conVariante = cuerpo(mvc.perform(get("/productos/{id}", productoId).session(sesion)));
        assertThat(conVariante).contains("POL-L-AZU").contains("Variante agregada.");

        Long varianteId = consulta.obtenerProducto(productoId).variantes().get(0).id();
        mvc.perform(post("/productos/{id}/variantes/{vid}/precios", productoId, varianteId).session(sesion)
                        .param("precioCompra", "13.00").param("precioVenta", "34.90"))
                .andExpect(redirectedUrl("/productos/" + productoId));
        assertThat(consulta.obtenerProducto(productoId).variantes().get(0).precioVenta()).isEqualByComparingTo("34.90");
    }

    @Test
    void unSkuRepetidoOUnPrecioInvalidoMuestranElMotivoSinPerderLoEscrito() throws Exception {
        MockHttpSession sesion = comercianteConTienda("rosa@correo.pe");
        Long categoriaId = crearCategoria(sesion, "Polos");
        crearProducto(sesion, categoriaId, "Polo básico", "POL-M-AZU");

        String repetido = cuerpo(mvc.perform(post("/productos").session(sesion).param("nombre", "Otro polo")
                .param("categoriaId", String.valueOf(categoriaId)).param("sku", "POL-M-AZU")
                .param("precioVenta", "29.90")).andExpect(status().isOk()));
        assertThat(repetido).contains("Ya existe el SKU").contains("Otro polo");

        String invalido = cuerpo(mvc.perform(post("/productos").session(sesion).param("nombre", "Polo raro")
                .param("categoriaId", String.valueOf(categoriaId)).param("sku", "POL-X")
                .param("precioVenta", "abc")).andExpect(status().isOk()));
        assertThat(invalido).contains("no es un número válido").contains("Polo raro");
    }

    @Test
    void unProductoDesactivadoSoloApareceSiSePidenTodos() throws Exception {
        MockHttpSession sesion = comercianteConTienda("rosa@correo.pe");
        Long productoId = crearProducto(sesion, crearCategoria(sesion, "Polos"), "Polo básico", "POL-M-AZU");

        mvc.perform(post("/productos/{id}/estado", productoId).session(sesion).param("activo", "false"))
                .andExpect(redirectedUrl("/productos/" + productoId));

        assertThat(cuerpo(mvc.perform(get("/productos").session(sesion)))).doesNotContain("Polo básico");
        assertThat(cuerpo(mvc.perform(get("/productos").param("todos", "true").session(sesion))))
                .contains("Polo básico").contains("Inactivo");
    }

    @Test
    void nadiePuedeVerNiCambiarUnProductoDeOtroComerciante() throws Exception {
        MockHttpSession rosa = comercianteConTienda("rosa@correo.pe");
        Long productoId = crearProducto(rosa, crearCategoria(rosa, "Polos"), "Polo básico", "POL-M-AZU");
        MockHttpSession pedro = comercianteConTienda("pedro@correo.pe");

        mvc.perform(get("/productos/{id}", productoId).session(pedro)).andExpect(status().isNotFound());
        mvc.perform(post("/productos/{id}/estado", productoId).session(pedro).param("activo", "false"))
                .andExpect(status().isNotFound());
        assertThat(consulta.obtenerProducto(productoId).activo()).isTrue();
    }
}
