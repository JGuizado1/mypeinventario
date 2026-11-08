package pe.mypeinventario.fachada;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import pe.mypeinventario.consulta.AlertaResumen;
import pe.mypeinventario.domain.EstadoAlerta;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.observer.BandejaNotificaciones;
import pe.mypeinventario.service.ConsultaInventarioService;
import pe.mypeinventario.service.InventarioService;
import pe.mypeinventario.service.TiendaService;

/**
 * Patrón Facade de las alertas de reabastecimiento: verlas, marcarlas y leer los avisos
 * recientes que dejó la bandeja de notificaciones (un observador de las alertas).
 * Antes de tocar una alerta comprueba que sea de una tienda del comerciante; una alerta
 * ajena se responde como "no existe".
 */
@Service
public class AlertaFacade {

    private static final int AVISOS_MAXIMOS = 10;
    private static final List<EstadoAlerta> POR_ATENDER = List.of(EstadoAlerta.PENDIENTE, EstadoAlerta.VISTA);
    private static final List<EstadoAlerta> CERRADAS = List.of(EstadoAlerta.ATENDIDA);

    private final ConsultaInventarioService consulta;
    private final InventarioService inventarioService;
    private final TiendaService tiendaService;
    private final BandejaNotificaciones bandeja;

    public AlertaFacade(ConsultaInventarioService consulta, InventarioService inventarioService,
                        TiendaService tiendaService, BandejaNotificaciones bandeja) {
        this.consulta = consulta;
        this.inventarioService = inventarioService;
        this.tiendaService = tiendaService;
        this.bandeja = bandeja;
    }

    /**
     * Lista las alertas de una tienda del comerciante.
     *
     * @param atendidas {@code true} para las ya atendidas; {@code false} para las pendientes y vistas
     */
    public List<AlertaResumen> alertas(Long comercianteId, Long tiendaId, boolean atendidas) {
        tiendaService.verificarPropiedad(comercianteId, tiendaId);
        return consulta.listarResumenAlertas(tiendaId, atendidas ? CERRADAS : POR_ATENDER);
    }

    /** @return los avisos más recientes de la tienda, del más nuevo al más antiguo */
    public List<String> avisosRecientes(Long comercianteId, Long tiendaId) {
        tiendaService.verificarPropiedad(comercianteId, tiendaId);
        List<String> avisos = new ArrayList<>(bandeja.mensajesDe(tiendaId));
        Collections.reverse(avisos);
        return avisos.size() > AVISOS_MAXIMOS ? List.copyOf(avisos.subList(0, AVISOS_MAXIMOS)) : avisos;
    }

    /**
     * Marca como vista una alerta del comerciante.
     *
     * @throws RecursoNoEncontradoException si no existe o es de otro comerciante
     */
    public void marcarVista(Long comercianteId, Long alertaId) {
        alerta(comercianteId, alertaId);
        inventarioService.marcarAlertaVista(alertaId);
    }

    /**
     * Marca como atendida una alerta del comerciante.
     *
     * @throws RecursoNoEncontradoException si no existe o es de otro comerciante
     */
    public void marcarAtendida(Long comercianteId, Long alertaId) {
        alerta(comercianteId, alertaId);
        inventarioService.marcarAlertaAtendida(alertaId);
    }

    private AlertaResumen alerta(Long comercianteId, Long alertaId) {
        AlertaResumen alerta = consulta.obtenerResumenAlerta(alertaId);
        try {
            tiendaService.verificarPropiedad(comercianteId, alerta.tiendaId());
        } catch (RecursoNoEncontradoException e) {
            throw new RecursoNoEncontradoException("Alerta", alertaId);
        }
        return alerta;
    }
}
