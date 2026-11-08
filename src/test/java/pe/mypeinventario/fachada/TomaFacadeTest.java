package pe.mypeinventario.fachada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.mypeinventario.consulta.ConteoResumen;
import pe.mypeinventario.consulta.TomaResumen;
import pe.mypeinventario.domain.EstadoToma;
import pe.mypeinventario.domain.TomaInventario;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.service.ConsultaCatalogoService;
import pe.mypeinventario.service.ConsultaTomaService;
import pe.mypeinventario.service.InventarioFisicoService;
import pe.mypeinventario.service.TiendaService;

/** Prueba unitaria con mocks: nadie cuenta ni cierra una toma de inventario de otro comerciante. */
@ExtendWith(MockitoExtension.class)
class TomaFacadeTest {

    @Mock private InventarioFisicoService fisico;
    @Mock private ConsultaTomaService consulta;
    @Mock private ConsultaCatalogoService catalogo;
    @Mock private TiendaService tiendaService;
    @InjectMocks private TomaFacade fachada;

    private static TomaResumen toma(EstadoToma estado) {
        ConteoResumen conteo = new ConteoResumen(100L, "POL-M-AZU", "Polo básico", "M", "Azul", 10, 8);
        return new TomaResumen(40L, 5L, LocalDateTime.of(2026, 11, 6, 19, 30), estado, List.of(conteo));
    }

    @Test
    void unaTomaPropiaSeDevuelve() {
        when(consulta.obtenerToma(40L)).thenReturn(toma(EstadoToma.EN_CURSO));

        assertThat(fachada.toma(1L, 40L).id()).isEqualTo(40L);
    }

    @Test
    void unaTomaAjenaSeNiegaComoSiNoExistiera() {
        when(consulta.obtenerToma(40L)).thenReturn(toma(EstadoToma.EN_CURSO));
        doThrow(new RecursoNoEncontradoException("Tienda", 5L)).when(tiendaService).verificarPropiedad(2L, 5L);

        assertThatThrownBy(() -> fachada.toma(2L, 40L))
                .isInstanceOf(RecursoNoEncontradoException.class).hasMessageContaining("Toma de inventario");
        assertThatThrownBy(() -> fachada.cerrar(2L, 40L)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> fachada.registrarConteos(2L, 40L, Map.of(100L, 8)))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(fisico);
    }

    @Test
    void noSeIniciaUnaTomaEnUnaTiendaAjena() {
        doThrow(new RecursoNoEncontradoException("Tienda", 5L)).when(tiendaService).verificarPropiedad(2L, 5L);

        assertThatThrownBy(() -> fachada.iniciar(2L, 5L)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> fachada.tomas(2L, 5L)).isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(fisico, consulta);
    }

    @Test
    void alIniciarSeDevuelveElResumenDeLaTomaNueva() {
        TomaInventario nueva = mock(TomaInventario.class);
        when(nueva.getId()).thenReturn(40L);
        when(fisico.iniciarToma(5L)).thenReturn(nueva);
        when(consulta.obtenerToma(40L)).thenReturn(toma(EstadoToma.EN_CURSO));

        assertThat(fachada.iniciar(1L, 5L).enCurso()).isTrue();
    }

    @Test
    void cadaConteoSeAnotaEnLaToma() {
        when(consulta.obtenerToma(40L)).thenReturn(toma(EstadoToma.EN_CURSO));
        Map<Long, Integer> conteos = new LinkedHashMap<>();
        conteos.put(100L, 8);
        conteos.put(101L, 0);

        int anotados = fachada.registrarConteos(1L, 40L, conteos);

        assertThat(anotados).isEqualTo(2);
        verify(fisico).registrarConteo(40L, 100L, 8);
        verify(fisico).registrarConteo(40L, 101L, 0);
    }

    @Test
    void cerrarPrimeroCompruebaLaPropiedadYLuegoCierra() {
        when(consulta.obtenerToma(40L)).thenReturn(toma(EstadoToma.CERRADA));

        TomaResumen cerrada = fachada.cerrar(1L, 40L);

        assertThat(cerrada.enCurso()).isFalse();
        assertThat(cerrada.conDiferencia()).isEqualTo(1);
        InOrder orden = inOrder(tiendaService, fisico);
        orden.verify(tiendaService).verificarPropiedad(1L, 5L);
        orden.verify(fisico).cerrarToma(40L);
    }

    @Test
    void paraContarSeOfrecenTambienLosProductosInactivos() {
        when(catalogo.listarProductos(5L, false)).thenReturn(List.of());

        assertThat(fachada.productosParaContar(1L, 5L)).isEmpty();
    }
}
