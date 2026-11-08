package pe.mypeinventario.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import pe.mypeinventario.domain.EstadoAlerta;
import pe.mypeinventario.observer.BandejaNotificaciones;
import pe.mypeinventario.observer.EventoAlerta;
import pe.mypeinventario.service.ConsultaCatalogoService;
import pe.mypeinventario.service.ConsultaInventarioService;
import pe.mypeinventario.service.CuentaService;
import pe.mypeinventario.service.ProveedorService;
import pe.mypeinventario.service.TiendaService;

/** Recorre las pantallas de alertas y reportes con la aplicación real, sin navegador. */
@SpringBootTest
@Transactional
class WebAlertasReportesTest {

    @Autowired private WebApplicationContext contexto;
    @Autowired private CuentaService cuentaService;
    @Autowired private TiendaService tiendaService;
    @Autowired private ConsultaCatalogoService consulta;
    @Autowired private ProveedorService proveedorService;
    @Autowired private ConsultaInventarioService consultaInventario;
    @Autowired private BandejaNotificaciones bandeja;

    private MockMvc mvc;
    private PasosWeb pasos;

    @BeforeEach
    void prepararMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        pasos = new PasosWeb(mvc, cuentaService, tiendaService, consulta, proveedorService);
    }

    private Long alertaPendiente(MockHttpSession sesion) {
        return consultaInventario.listarResumenAlertas(pasos.tiendaDe(sesion), List.of(EstadoAlerta.PENDIENTE))
                .get(0).id();
    }

    @Test
    void sinTiendaElegidaAlertasYReportesPidenElegirUna() throws Exception {
        MockHttpSession sesion = new MockHttpSession();
        mvc.perform(post("/registro").session(sesion).param("nombre", "Rosa").param("correo", "rosa@correo.pe")
                .param("contrasena", "clave-segura-1"));

        mvc.perform(get("/alertas").session(sesion)).andExpect(redirectedUrl("/tiendas"));
        mvc.perform(get("/reportes").session(sesion)).andExpect(redirectedUrl("/tiendas"));
        mvc.perform(get("/alertas")).andExpect(redirectedUrl("/login"));
    }

    @Test
    void sinAlertasLaPantallaLoDice() throws Exception {
        MockHttpSession sesion = pasos.comercianteConTienda("rosa@correo.pe");

        assertThat(pasos.pagina(sesion, "/alertas")).contains("No hay alertas por atender")
                .contains("Todavía no hay avisos recientes");
        assertThat(pasos.pagina(sesion, "/alertas?atendidas=true")).contains("Aún no atendiste ninguna alerta");
    }

    @Test
    void unaVentaConPocoStockDejaUnaAlertaQueSePuedeVerYAtender() throws Exception {
        MockHttpSession sesion = pasos.comercianteConTienda("rosa@correo.pe");
        pasos.productoConPocoStock(sesion);

        String pendiente = pasos.pagina(sesion, "/alertas");
        assertThat(pendiente).contains("Polo básico").contains("POL-M-AZU").contains("Pendiente")
                .contains("Marcar vista").contains("Marcar atendida");
        Long alertaId = alertaPendiente(sesion);

        mvc.perform(post("/alertas/{id}/vista", alertaId).session(sesion)).andExpect(redirectedUrl("/alertas"));
        String vista = pasos.pagina(sesion, "/alertas");
        assertThat(vista).contains("Alerta marcada como vista.").contains("Marcar atendida")
                .doesNotContain("Marcar vista");

        mvc.perform(post("/alertas/{id}/vista", alertaId).session(sesion)).andExpect(redirectedUrl("/alertas"));
        assertThat(pasos.pagina(sesion, "/alertas")).contains("Solo una alerta pendiente puede marcarse como vista");

        mvc.perform(post("/alertas/{id}/atendida", alertaId).session(sesion)).andExpect(redirectedUrl("/alertas"));
        assertThat(pasos.pagina(sesion, "/alertas")).contains("Alerta marcada como atendida.")
                .contains("No hay alertas por atender");
        assertThat(pasos.pagina(sesion, "/alertas?atendidas=true")).contains("POL-M-AZU").contains("Atendida");

        mvc.perform(post("/alertas/{id}/atendida", alertaId).session(sesion)).andExpect(redirectedUrl("/alertas"));
        assertThat(pasos.pagina(sesion, "/alertas")).contains("La alerta ya está atendida");
    }

    /**
     * Los observadores se avisan al confirmar la transacción, y estas pruebas la revierten;
     * por eso aquí se simula el aviso que el sistema entrega al confirmar.
     */
    @Test
    void losAvisosRecientesMuestranLoQueRecibioLaBandeja() throws Exception {
        MockHttpSession sesion = pasos.comercianteConTienda("rosa@correo.pe");
        Long tiendaId = pasos.tiendaDe(sesion);
        bandeja.alertaGenerada(new EventoAlerta(1L, tiendaId, 100L, "POL-M-AZU", "Polo básico", "M", "Azul",
                2, 5, "Stock bajo", LocalDateTime.now()));
        bandeja.alertaGenerada(new EventoAlerta(2L, tiendaId, 101L, "POL-L-AZU", "Polo básico", "L", "Azul",
                1, 5, "Stock bajo", LocalDateTime.now()));

        String pagina = pasos.pagina(sesion, "/alertas");

        assertThat(pagina).contains("Stock bajo: POL-M-AZU").contains("quedan 2 unidades")
                .contains("quedan 1 unidad;");
        // El más nuevo aparece primero.
        assertThat(pagina.indexOf("POL-L-AZU")).isLessThan(pagina.indexOf("POL-M-AZU"));
    }

    @Test
    void nadiePuedeCambiarUnaAlertaDeOtroComerciante() throws Exception {
        MockHttpSession rosa = pasos.comercianteConTienda("rosa@correo.pe");
        pasos.productoConPocoStock(rosa);
        Long alertaId = alertaPendiente(rosa);
        MockHttpSession pedro = pasos.comercianteConTienda("pedro@correo.pe");

        mvc.perform(post("/alertas/{id}/vista", alertaId).session(pedro)).andExpect(status().isNotFound());
        mvc.perform(post("/alertas/{id}/atendida", alertaId).session(pedro)).andExpect(status().isNotFound());
        assertThat(pasos.pagina(pedro, "/alertas")).doesNotContain("POL-M-AZU");
        assertThat(alertaPendiente(rosa)).isEqualTo(alertaId);
    }

    @Test
    void elReporteDelPeriodoMuestraVentasCostoUtilidadYCategorias() throws Exception {
        MockHttpSession sesion = pasos.comercianteConTienda("rosa@correo.pe");
        pasos.productoConPocoStock(sesion);

        // 8 unidades a 29.90 = 239.20; costo 8 x 12.00 = 96.00; utilidad 143.20.
        String reporte = pasos.pagina(sesion, "/reportes");
        assertThat(reporte).contains("S/ 239.20").contains("S/ 96.00").contains("S/ 143.20")
                .contains("Más vendidos").contains("Polo básico").contains("POL-M-AZU")
                .contains("Ventas por categoría").contains("Polos")
                .contains("Ninguna variante está parada");
    }

    @Test
    void unPeriodoSinVentasLoDiceEnCadaSeccion() throws Exception {
        MockHttpSession sesion = pasos.comercianteConTienda("rosa@correo.pe");
        pasos.productoConPocoStock(sesion);

        String reporte = pasos.pagina(sesion, "/reportes?desde=2020-01-01&hasta=2020-01-31");

        assertThat(reporte).contains("No hubo ventas en este periodo").doesNotContain("S/ 239.20");
    }

    @Test
    void unProductoConStockQueNuncaSeVendioApareceSinRotacion() throws Exception {
        MockHttpSession sesion = pasos.comercianteConTienda("rosa@correo.pe");
        Long varianteId = pasos.crearVariante(sesion);
        pasos.comprar(sesion, pasos.crearProveedor(sesion), varianteId, 10);

        String reporte = pasos.pagina(sesion, "/reportes?dias=1");

        assertThat(reporte).contains("Sin rotación").contains("POL-M-AZU").contains("Nunca").contains("S/ 120.00");
    }

    @Test
    void unReporteConDatosInvalidosMuestraElMotivoSinPerderLoEscrito() throws Exception {
        MockHttpSession sesion = pasos.comercianteConTienda("rosa@correo.pe");

        assertThat(pasos.pagina(sesion, "/reportes?desde=2026-12-01&hasta=2026-11-01"))
                .contains("La fecha final no puede ser anterior a la inicial").contains("2026-12-01");
        assertThat(pasos.pagina(sesion, "/reportes?desde=abc")).contains("La fecha inicial no es válida");
        assertThat(pasos.pagina(sesion, "/reportes?dias=0")).contains("Los días deben estar entre 1 y 3650");
        assertThat(pasos.pagina(sesion, "/reportes?limite=abc"))
                .contains("El límite de más vendidos debe ser un número entero");
    }
}
