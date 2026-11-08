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
import pe.mypeinventario.service.CuentaService;
import pe.mypeinventario.service.TiendaService;

/** Recorre las pantallas con la aplicación real (H2, servicios, plantillas), sin abrir un navegador. */
@SpringBootTest
@Transactional
class WebAccesoTest {

    @Autowired private WebApplicationContext contexto;
    @Autowired private CuentaService cuentaService;
    @Autowired private TiendaService tiendaService;

    private MockMvc mvc;

    @BeforeEach
    void prepararMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    private String cuerpo(ResultActions resultado) throws Exception {
        return resultado.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private void registrar(MockHttpSession sesion, String nombre, String correo) throws Exception {
        mvc.perform(post("/registro").session(sesion).param("nombre", nombre).param("correo", correo)
                        .param("celular", "987654321").param("contrasena", "clave-segura-1"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/tiendas"));
    }

    private Long crearTienda(MockHttpSession sesion, String correo, String nombre) throws Exception {
        mvc.perform(post("/tiendas").session(sesion).param("nombre", nombre)
                        .param("direccion", "Jr. Gamarra 450").param("galeria", "Gamarra"))
                .andExpect(redirectedUrl("/tiendas"));
        Long comercianteId = cuentaService.autenticar(correo, "clave-segura-1").id();
        return tiendaService.listarTiendas(comercianteId).get(0).id();
    }

    @Test
    void sinSesionLasPaginasSeRedirigenALaEntrada() throws Exception {
        mvc.perform(get("/")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/tiendas")).andExpect(redirectedUrl("/login"));
        mvc.perform(post("/tiendas").param("nombre", "Moda")).andExpect(redirectedUrl("/login"));
    }

    @Test
    void laPantallaDeEntradaSeMuestraSinSesion() throws Exception {
        ResultActions pantalla = mvc.perform(get("/login")).andExpect(status().isOk());

        assertThat(cuerpo(pantalla)).contains("Entrar").contains("Crear cuenta");
    }

    @Test
    void lasCredencialesIncorrectasMuestranUnErrorClaro() throws Exception {
        ResultActions pantalla = mvc.perform(post("/login").param("correo", "nadie@correo.pe")
                .param("contrasena", "clave-equivocada")).andExpect(status().isOk());

        assertThat(cuerpo(pantalla)).contains("Correo o contraseña incorrectos").contains("nadie@correo.pe");
    }

    @Test
    void elRegistroConDatosInvalidosVuelveAlFormularioConElMotivo() throws Exception {
        ResultActions pantalla = mvc.perform(post("/registro").param("nombre", "Rosa").param("correo", "rosa@correo.pe")
                .param("contrasena", "corta")).andExpect(status().isOk());

        assertThat(cuerpo(pantalla)).contains("al menos 8 caracteres");
    }

    @Test
    void elRecorridoCompletoRegistroTiendaEInicio() throws Exception {
        MockHttpSession sesion = new MockHttpSession();
        registrar(sesion, "Rosa Quispe", "rosa@correo.pe");
        Long tiendaId = crearTienda(sesion, "rosa@correo.pe", "Moda Rosa");

        mvc.perform(get("/").session(sesion)).andExpect(redirectedUrl("/tiendas"));
        assertThat(cuerpo(mvc.perform(get("/tiendas").session(sesion)).andExpect(status().isOk())))
                .contains("Moda Rosa").contains("Trabajar en esta tienda");

        mvc.perform(post("/tiendas/{id}/elegir", tiendaId).session(sesion)).andExpect(redirectedUrl("/"));
        String inicio = cuerpo(mvc.perform(get("/").session(sesion)).andExpect(status().isOk()));
        assertThat(inicio).contains("Moda Rosa").contains("Productos activos").contains("Rosa Quispe");
    }

    @Test
    void nadiePuedeElegirLaTiendaDeOtroComerciante() throws Exception {
        MockHttpSession rosa = new MockHttpSession();
        registrar(rosa, "Rosa Quispe", "rosa@correo.pe");
        Long tiendaDeRosa = crearTienda(rosa, "rosa@correo.pe", "Moda Rosa");
        MockHttpSession pedro = new MockHttpSession();
        registrar(pedro, "Pedro Ramos", "pedro@correo.pe");

        mvc.perform(post("/tiendas/{id}/elegir", tiendaDeRosa).session(pedro)).andExpect(status().isNotFound());
    }

    @Test
    void alSalirLaSesionYaNoSirve() throws Exception {
        MockHttpSession sesion = new MockHttpSession();
        registrar(sesion, "Rosa Quispe", "rosa@correo.pe");

        mvc.perform(post("/salir").session(sesion)).andExpect(redirectedUrl("/login"));

        mvc.perform(get("/tiendas").session(sesion)).andExpect(redirectedUrl("/login"));
    }

    @Test
    void ingresarConCredencialesCorrectasLlevaAlInicio() throws Exception {
        MockHttpSession registro = new MockHttpSession();
        registrar(registro, "Rosa Quispe", "rosa@correo.pe");

        MockHttpSession sesion = new MockHttpSession();
        mvc.perform(post("/login").session(sesion).param("correo", "Rosa@Correo.pe")
                        .param("contrasena", "clave-segura-1"))
                .andExpect(redirectedUrl("/"));

        mvc.perform(get("/tiendas").session(sesion)).andExpect(status().isOk());
    }
}
