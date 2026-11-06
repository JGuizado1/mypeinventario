package pe.mypeinventario.service.impl;

import java.util.List;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import pe.mypeinventario.config.ParametrosStock;
import pe.mypeinventario.domain.AlertaReabastecimiento;
import pe.mypeinventario.domain.EstadoAlerta;
import pe.mypeinventario.domain.MovimientoInventario;
import pe.mypeinventario.domain.ProductoItem;
import pe.mypeinventario.domain.VarianteSKU;
import pe.mypeinventario.exception.ConflictoConcurrenciaException;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.observer.EventoAlerta;
import pe.mypeinventario.observer.SujetoAlertas;
import pe.mypeinventario.repository.AlertaReabastecimientoRepository;
import pe.mypeinventario.repository.ProductoItemRepository;
import pe.mypeinventario.repository.VarianteSKURepository;
import pe.mypeinventario.service.InventarioService;

/** Implementación de {@link InventarioService}. */
@Service
public class InventarioServiceImpl implements InventarioService {

    private static final List<EstadoAlerta> ESTADOS_ABIERTOS =
            List.of(EstadoAlerta.PENDIENTE, EstadoAlerta.VISTA);

    private final VarianteSKURepository varianteRepository;
    private final AlertaReabastecimientoRepository alertaRepository;
    private final ProductoItemRepository productoRepository;
    private final ParametrosStock parametros;
    private final SujetoAlertas sujetoAlertas;

    public InventarioServiceImpl(VarianteSKURepository varianteRepository,
                                 AlertaReabastecimientoRepository alertaRepository,
                                 ProductoItemRepository productoRepository,
                                 ParametrosStock parametros,
                                 SujetoAlertas sujetoAlertas) {
        this.varianteRepository = varianteRepository;
        this.alertaRepository = alertaRepository;
        this.productoRepository = productoRepository;
        this.parametros = parametros;
        this.sujetoAlertas = sujetoAlertas;
    }

    @Override
    @Transactional
    public MovimientoInventario registrarMovimiento(Long varianteId, MovimientoInventario movimiento) {
        if (movimiento == null) {
            throw new OperacionInvalidaException("El movimiento es obligatorio");
        }
        VarianteSKU variante = varianteRepository.findById(varianteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Variante", varianteId));
        try {
            variante.registrarMovimiento(movimiento);
            // flush (y no save): persiste esta misma instancia del movimiento, con su identificador.
            varianteRepository.flush();
            actualizarAlertas(variante);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ConflictoConcurrenciaException("La variante " + variante.getSku()
                    + " fue modificada por otra operación; vuelva a intentarlo", e);
        }
        return movimiento;
    }

    @Override
    @Transactional
    public AlertaReabastecimiento marcarAlertaVista(Long alertaId) {
        AlertaReabastecimiento alerta = obtenerAlerta(alertaId);
        try {
            alerta.marcarVista();
        } catch (IllegalStateException e) {
            throw new OperacionInvalidaException(e.getMessage(), e);
        }
        return alerta;
    }

    @Override
    @Transactional
    public AlertaReabastecimiento marcarAlertaAtendida(Long alertaId) {
        AlertaReabastecimiento alerta = obtenerAlerta(alertaId);
        try {
            alerta.marcarAtendida();
        } catch (IllegalStateException e) {
            throw new OperacionInvalidaException(e.getMessage(), e);
        }
        return alerta;
    }

    /** Crea una alerta si hace falta reabastecer, o cierra las abiertas si el stock se recuperó. */
    private void actualizarAlertas(VarianteSKU variante) {
        if (variante.requiereReabastecimiento(parametros)) {
            boolean hayAlertaAbierta = alertaRepository.existeAlertaEnEstados(variante.getId(), ESTADOS_ABIERTOS);
            if (!hayAlertaAbierta) {
                AlertaReabastecimiento alerta = new AlertaReabastecimiento(
                        null, variante.calcularStock(), variante.resolverStockMinimo(parametros));
                variante.agregarAlerta(alerta);
                varianteRepository.flush();
                notificarTrasConfirmar(construirEvento(variante, alerta));
            }
        } else {
            for (AlertaReabastecimiento alerta : variante.getAlertas()) {
                if (alerta.getEstado() != EstadoAlerta.ATENDIDA) {
                    alerta.marcarAtendida();
                }
            }
        }
    }

    /**
     * Avisa a los observadores solo cuando la transacción se confirma. Si se revierte, no se
     * notifica: así nadie recibe un aviso de una alerta que nunca llegó a guardarse.
     */
    private void notificarTrasConfirmar(EventoAlerta evento) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sujetoAlertas.notificar(evento);
                }
            });
        } else {
            sujetoAlertas.notificar(evento);
        }
    }

    private EventoAlerta construirEvento(VarianteSKU variante, AlertaReabastecimiento alerta) {
        ProductoItem producto = productoRepository.findByVarianteId(variante.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto de la variante", variante.getId()));
        return new EventoAlerta(alerta.getId(), producto.getTienda().getId(), variante.getId(),
                variante.getSku(), producto.getNombre(), variante.getTalla(), variante.getColor(),
                alerta.getStockActual(), alerta.getStockMinimo(), alerta.getMensaje(),
                alerta.getFechaGeneracion());
    }

    private AlertaReabastecimiento obtenerAlerta(Long alertaId) {
        return alertaRepository.findById(alertaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Alerta", alertaId));
    }
}