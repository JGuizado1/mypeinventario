package pe.mypeinventario.fachada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.mypeinventario.consulta.MarcaResumen;
import pe.mypeinventario.consulta.ProductoResumen;
import pe.mypeinventario.consulta.VarianteResumen;
import pe.mypeinventario.domain.Marca;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.service.CatalogoService;
import pe.mypeinventario.service.ConsultaCatalogoService;
import pe.mypeinventario.service.TiendaService;
import pe.mypeinventario.service.dto.SolicitudProducto;
import pe.mypeinventario.service.dto.SolicitudVariante;

/** Prueba unitaria con mocks: lo importante es que nadie toque productos de otro comerciante. */
@ExtendWith(MockitoExtension.class)
class CatalogoFacadeTest {

    @Mock private CatalogoService catalogo;
    @Mock private ConsultaCatalogoService consulta;
    @Mock private TiendaService tiendaService;
    @InjectMocks private CatalogoFacade fachada;

    private static VarianteResumen variante(Long id) {
        return new VarianteResumen(id, "POL-M", null, "M", "Azul", null, new BigDecimal("29.90"), 5, 10, false);
    }

    private static ProductoResumen producto(Long id, Long tiendaId) {
        return new ProductoResumen(id, tiendaId, "Polo", null, "Polos", null, true, List.of(variante(100L)));
    }

    private static SolicitudVariante solicitudVariante() {
        return new SolicitudVariante("POL-L", null, "L", "Azul", null, new BigDecimal("29.90"), null);
    }

    @Test
    void unProductoPropioSeDevuelveYUnoAjenoSeNiegaComoSiNoExistiera() {
        when(consulta.obtenerProducto(10L)).thenReturn(producto(10L, 5L));
        doThrow(new RecursoNoEncontradoException("Tienda", 5L)).when(tiendaService).verificarPropiedad(2L, 5L);

        // Primero la llamada ajena, que usa el doThrow: con Mockito estricto, una llamada propia
        // antes de usarlo se contaría como un posible error de la prueba.
        assertThatThrownBy(() -> fachada.producto(2L, 10L))
                .isInstanceOf(RecursoNoEncontradoException.class).hasMessageContaining("Producto");
        assertThat(fachada.producto(1L, 10L).id()).isEqualTo(10L);
    }

    @Test
    void noSeTocaNingunProductoAjeno() {
        when(consulta.obtenerProducto(10L)).thenReturn(producto(10L, 5L));
        doThrow(new RecursoNoEncontradoException("Tienda", 5L)).when(tiendaService).verificarPropiedad(2L, 5L);

        assertThatThrownBy(() -> fachada.cambiarEstado(2L, 10L, false)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> fachada.agregarVariante(2L, 10L, solicitudVariante()))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> fachada.actualizarPrecios(2L, 10L, 100L, null, new BigDecimal("10.00")))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(catalogo);
    }

    @Test
    void sePuedenCambiarPreciosYEstadoDeUnProductoPropio() {
        when(consulta.obtenerProducto(10L)).thenReturn(producto(10L, 5L));

        fachada.actualizarPrecios(1L, 10L, 100L, null, new BigDecimal("35.00"));
        fachada.cambiarEstado(1L, 10L, false);

        verify(catalogo).actualizarPrecios(100L, null, new BigDecimal("35.00"));
        verify(catalogo).cambiarEstadoProducto(10L, false);
    }

    @Test
    void unaVarianteQueNoEsDelProductoSeRechaza() {
        when(consulta.obtenerProducto(10L)).thenReturn(producto(10L, 5L));

        assertThatThrownBy(() -> fachada.actualizarPrecios(1L, 10L, 999L, null, new BigDecimal("35.00")))
                .isInstanceOf(RecursoNoEncontradoException.class).hasMessageContaining("Variante");
        verifyNoInteractions(catalogo);
    }

    @Test
    void registrarUnProductoExigeQueLaTiendaSeaPropia() {
        SolicitudProducto solicitud = new SolicitudProducto(5L, "Polo", null, 1L, null, List.of(solicitudVariante()));
        doThrow(new RecursoNoEncontradoException("Tienda", 5L)).when(tiendaService).verificarPropiedad(2L, 5L);

        assertThatThrownBy(() -> fachada.registrarProducto(2L, solicitud)).isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(catalogo);

        fachada.registrarProducto(1L, solicitud);
        verify(catalogo).registrarProducto(solicitud);
    }

    @Test
    void listarProductosExigeQueLaTiendaSeaPropia() {
        doThrow(new RecursoNoEncontradoException("Tienda", 5L)).when(tiendaService).verificarPropiedad(2L, 5L);

        assertThatThrownBy(() -> fachada.productos(2L, 5L, true)).isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(consulta);
        verifyNoMoreInteractions(catalogo);
    }

    @Test
    void lasMarcasSeEntreganComoResumenes() {
        Marca runner = new Marca(3L, "Runner");
        when(consulta.listarMarcas()).thenReturn(List.of(runner));
        when(catalogo.crearMarca("Runner")).thenReturn(runner);

        assertThat(fachada.marcas()).containsExactly(new MarcaResumen(3L, "Runner"));
        assertThat(fachada.crearMarca("Runner")).isEqualTo(new MarcaResumen(3L, "Runner"));
    }
}
