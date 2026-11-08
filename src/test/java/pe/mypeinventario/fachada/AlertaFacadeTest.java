package pe.mypeinventario.fachada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.mypeinventario.consulta.AlertaResumen;
import pe.mypeinventario.domain.EstadoAlerta;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.observer.BandejaNotificaciones;
import pe.mypeinventario.service.ConsultaInventarioService;
import pe.mypeinventario.service.InventarioService;
import pe.mypeinventario.service.TiendaService;

/** Prueba unitaria con mocks: nadie ve ni cambia las alertas de otro comerciante. */
@ExtendWith(MockitoExtension.class)
class AlertaFacadeTest {

    @Mock private ConsultaInventarioService consulta;
    @Mock private InventarioService inventarioService;
    @Mock private TiendaService tiendaService;
    @Mock private BandejaNotificaciones bandeja;
    @InjectMocks private AlertaFacade fachada;

    private static AlertaResumen alerta(Long id, Long tiendaId, EstadoAlerta estado) {
        return new AlertaResumen(id, tiendaId, 100L, "POL-M-NEG", "Polo básico", "M", "Negro",
                LocalDateTime.of(2026, 11, 6, 19, 30), 2, 5, estado, "Stock actual (2) igual o inferior al mínimo (5)");
    }

    @Test
    void sinPedirAtendidasSeListanLasPendientesYLasVistas() {
        AlertaResumen alerta = alerta(20L, 5L, EstadoAlerta.PENDIENTE);
        when(consulta.listarResumenAlertas(5L, List.of(EstadoAlerta.PENDIENTE, EstadoAlerta.VISTA)))
                .thenReturn(List.of(alerta));

        assertThat(fachada.alertas(1L, 5L, false)).containsExactly(alerta);
    }

    @Test
    void pidiendoAtendidasSoloSeListanLasAtendidas() {
        AlertaResumen alerta = alerta(21L, 5L, EstadoAlerta.ATENDIDA);
        when(consulta.listarResumenAlertas(5L, List.of(EstadoAlerta.ATENDIDA))).thenReturn(List.of(alerta));

        assertThat(fachada.alertas(1L, 5L, true)).containsExactly(alerta);
    }

    @Test
    void lasAlertasDeUnaTiendaAjenaSeNiegan() {
        doThrow(new RecursoNoEncontradoException("Tienda", 5L)).when(tiendaService).verificarPropiedad(2L, 5L);

        assertThatThrownBy(() -> fachada.alertas(2L, 5L, false)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> fachada.avisosRecientes(2L, 5L)).isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(consulta, bandeja);
    }

    @Test
    void unaAlertaPropiaSePuedeMarcarVista() {
        when(consulta.obtenerResumenAlerta(20L)).thenReturn(alerta(20L, 5L, EstadoAlerta.PENDIENTE));

        fachada.marcarVista(1L, 20L);

        verify(inventarioService).marcarAlertaVista(20L);
    }

    @Test
    void unaAlertaPropiaSePuedeMarcarAtendida() {
        when(consulta.obtenerResumenAlerta(20L)).thenReturn(alerta(20L, 5L, EstadoAlerta.VISTA));

        fachada.marcarAtendida(1L, 20L);

        verify(inventarioService).marcarAlertaAtendida(20L);
    }

    @Test
    void unaAlertaAjenaSeNiegaComoSiNoExistiera() {
        when(consulta.obtenerResumenAlerta(20L)).thenReturn(alerta(20L, 5L, EstadoAlerta.PENDIENTE));
        doThrow(new RecursoNoEncontradoException("Tienda", 5L)).when(tiendaService).verificarPropiedad(2L, 5L);

        assertThatThrownBy(() -> fachada.marcarAtendida(2L, 20L))
                .isInstanceOf(RecursoNoEncontradoException.class).hasMessageContaining("Alerta");
        verifyNoInteractions(inventarioService);
    }

    @Test
    void losAvisosSalenDelMasNuevoAlMasAntiguo() {
        when(bandeja.mensajesDe(5L)).thenReturn(List.of("primero", "segundo", "tercero"));

        assertThat(fachada.avisosRecientes(1L, 5L)).containsExactly("tercero", "segundo", "primero");
    }

    @Test
    void soloSeMuestranLosDiezAvisosMasRecientes() {
        when(bandeja.mensajesDe(5L)).thenReturn(IntStream.rangeClosed(1, 12).mapToObj(n -> "aviso " + n).toList());

        List<String> avisos = fachada.avisosRecientes(1L, 5L);

        assertThat(avisos).hasSize(10).startsWith("aviso 12").endsWith("aviso 3");
    }
}
