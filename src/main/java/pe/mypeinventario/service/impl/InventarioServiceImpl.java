package pe.mypeinventario.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.mypeinventario.config.ParametrosStock;
import pe.mypeinventario.domain.AlertaReabastecimiento;
import pe.mypeinventario.domain.EstadoAlerta;
import pe.mypeinventario.domain.MovimientoInventario;
import pe.mypeinventario.domain.VarianteSKU;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.repository.AlertaReabastecimientoRepository;
import pe.mypeinventario.repository.VarianteSKURepository;
import pe.mypeinventario.service.InventarioService;

/** Implementación de {@link InventarioService}. */
@Service
public class InventarioServiceImpl implements InventarioService {

    private static final List<EstadoAlerta> ESTADOS_ABIERTOS =
            List.of(EstadoAlerta.PENDIENTE, EstadoAlerta.VISTA);

    private final VarianteSKURepository varianteRepository;
    private final AlertaReabastecimientoRepository alertaRepository;
    private final ParametrosStock parametros;

    public InventarioServiceImpl(VarianteSKURepository varianteRepository,
                                 AlertaReabastecimientoRepository alertaRepository,
                                 ParametrosStock parametros) {
        this.varianteRepository = varianteRepository;
        this.alertaRepository = alertaRepository;
        this.parametros = parametros;
    }

    @Override
    @Transactional
    public MovimientoInventario registrarMovimiento(Long varianteId, MovimientoInventario movimiento) {
        if (movimiento == null) {
            throw new OperacionInvalidaException("El movimiento es obligatorio");
        }
        VarianteSKU variante = varianteRepository.findById(varianteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Variante", varianteId));

        variante.registrarMovimiento(movimiento);
        // flush (y no save): la variante ya está gestionada, y así se persiste esta misma
        // instancia del movimiento, que queda con su identificador.
        varianteRepository.flush();
        actualizarAlertas(variante);
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
                variante.agregarAlerta(new AlertaReabastecimiento(
                        null, variante.calcularStock(), variante.resolverStockMinimo(parametros)));
                varianteRepository.flush();
            }
        } else {
            for (AlertaReabastecimiento alerta : variante.getAlertas()) {
                if (alerta.getEstado() != EstadoAlerta.ATENDIDA) {
                    alerta.marcarAtendida();
                }
            }
        }
    }

    private AlertaReabastecimiento obtenerAlerta(Long alertaId) {
        return alertaRepository.findById(alertaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Alerta", alertaId));
    }
}
