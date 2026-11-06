package pe.mypeinventario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.mypeinventario.consulta.TiendaResumen;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.service.dto.SolicitudRegistro;
import pe.mypeinventario.service.dto.SolicitudTienda;

@SpringBootTest
@Transactional
class TiendaServiceTest {

    @Autowired private CuentaService cuentaService;
    @Autowired private TiendaService tiendaService;

    private Long rosaId;
    private Long pedroId;

    @BeforeEach
    void prepararDatos() {
        rosaId = cuentaService.registrarComerciante(
                new SolicitudRegistro("Rosa Quispe", "rosa@correo.pe", null, "clave-segura-1")).id();
        pedroId = cuentaService.registrarComerciante(
                new SolicitudRegistro("Pedro Ramos", "pedro@correo.pe", null, "clave-segura-2")).id();
    }

    @Test
    void creaUnaTiendaYLaListaEntreLasDelComerciante() {
        TiendaResumen tienda = tiendaService.crearTienda(
                new SolicitudTienda(rosaId, "  Moda Rosa ", "Jr. Gamarra 100", "Gamarra"));

        assertThat(tienda.id()).isNotNull();
        assertThat(tienda.nombre()).isEqualTo("Moda Rosa");
        assertThat(tiendaService.listarTiendas(rosaId)).extracting(TiendaResumen::nombre)
                .containsExactly("Moda Rosa");
    }

    @Test
    void cadaComercianteSoloVeSusTiendas() {
        tiendaService.crearTienda(new SolicitudTienda(rosaId, "Moda Rosa", null, null));
        tiendaService.crearTienda(new SolicitudTienda(pedroId, "Calzado Pedro", null, null));
        tiendaService.crearTienda(new SolicitudTienda(rosaId, "Accesorios Rosa", null, null));

        assertThat(tiendaService.listarTiendas(rosaId)).extracting(TiendaResumen::nombre)
                .containsExactly("Accesorios Rosa", "Moda Rosa");
        assertThat(tiendaService.listarTiendas(pedroId)).extracting(TiendaResumen::nombre)
                .containsExactly("Calzado Pedro");
    }

    @Test
    void nadiePuedeUsarLaTiendaDeOtro() {
        Long tiendaDeRosa = tiendaService.crearTienda(new SolicitudTienda(rosaId, "Moda Rosa", null, null)).id();

        tiendaService.verificarPropiedad(rosaId, tiendaDeRosa);

        assertThatThrownBy(() -> tiendaService.verificarPropiedad(pedroId, tiendaDeRosa))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> tiendaService.verificarPropiedad(rosaId, 999_999L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void lasSolicitudesInvalidasSeRechazan() {
        assertThatThrownBy(() -> tiendaService.crearTienda(new SolicitudTienda(999_999L, "Moda", null, null)))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> tiendaService.listarTiendas(999_999L))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> new SolicitudTienda(rosaId, " ", null, null))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> new SolicitudTienda(null, "Moda", null, null))
                .isInstanceOf(OperacionInvalidaException.class);
    }
}
