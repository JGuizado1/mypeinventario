package pe.mypeinventario.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import pe.mypeinventario.consulta.ProductoResumen;
import pe.mypeinventario.consulta.TiendaResumen;
import pe.mypeinventario.consulta.UsuarioSesion;
import pe.mypeinventario.consulta.VarianteResumen;
import pe.mypeinventario.domain.EstadoAlerta;
import pe.mypeinventario.service.ConsultaCatalogoService;
import pe.mypeinventario.service.ConsultaInventarioService;
import pe.mypeinventario.service.CuentaService;
import pe.mypeinventario.service.TiendaService;

/**
 * Con el perfil demo se cargan los datos de ejemplo. Usa su propia base de datos en memoria
 * para no mezclar estos datos con los de las demás pruebas.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:demo")
@ActiveProfiles("demo")
class CargadorDatosDemoTest {

    @Autowired private CargadorDatosDemo cargador;
    @Autowired private CuentaService cuentaService;
    @Autowired private TiendaService tiendaService;
    @Autowired private ConsultaCatalogoService catalogo;
    @Autowired private ConsultaInventarioService inventario;

    @Test
    void cargaUnComercianteConDosTiendasYProductos() {
        UsuarioSesion sesion = cuentaService.autenticar(
                CargadorDatosDemo.CORREO_DEMO, CargadorDatosDemo.CONTRASENA_DEMO);

        List<TiendaResumen> tiendas = tiendaService.listarTiendas(sesion.id());
        assertThat(tiendas).extracting(TiendaResumen::nombre).containsExactly("Calzado Rosa", "Moda Rosa");

        TiendaResumen moda = tiendas.get(1);
        assertThat(catalogo.listarProductos(moda.id(), true)).extracting(ProductoResumen::nombre)
                .containsExactly("Casaca de invierno", "Jean clásico", "Polo básico");
    }

    @Test
    void lasVentasDejanStockCoherenteYAlertas() {
        UsuarioSesion sesion = cuentaService.autenticar(
                CargadorDatosDemo.CORREO_DEMO, CargadorDatosDemo.CONTRASENA_DEMO);
        TiendaResumen moda = tiendaService.listarTiendas(sesion.id()).get(1);
        TiendaResumen calzado = tiendaService.listarTiendas(sesion.id()).get(0);

        VarianteResumen poloNegro = catalogo.listarProductos(moda.id(), true).stream()
                .filter(p -> p.nombre().equals("Polo básico")).findFirst().orElseThrow()
                .variantes().stream().filter(v -> v.sku().equals("POL-M-NEG")).findFirst().orElseThrow();
        assertThat(poloNegro.stock()).isEqualTo(2);
        assertThat(poloNegro.stockBajo()).isTrue();

        assertThat(inventario.listarAlertas(moda.id(), EstadoAlerta.PENDIENTE)).isNotEmpty();
        assertThat(inventario.listarAlertas(calzado.id(), EstadoAlerta.PENDIENTE)).isNotEmpty();
    }

    @Test
    void cargarDosVecesNoDuplicaNada() {
        cargador.run(null);

        UsuarioSesion sesion = cuentaService.autenticar(
                CargadorDatosDemo.CORREO_DEMO, CargadorDatosDemo.CONTRASENA_DEMO);
        assertThat(tiendaService.listarTiendas(sesion.id())).hasSize(2);
    }
}
