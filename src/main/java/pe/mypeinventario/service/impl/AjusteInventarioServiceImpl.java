package pe.mypeinventario.service.impl;

import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.mypeinventario.domain.MovimientoInventario;
import pe.mypeinventario.domain.TipoMovimiento;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.factory.CreadorMovimiento;
import pe.mypeinventario.factory.OrigenMovimiento;
import pe.mypeinventario.service.AjusteInventarioService;
import pe.mypeinventario.service.InventarioService;

/** Implementación de {@link AjusteInventarioService}. */
@Service
public class AjusteInventarioServiceImpl implements AjusteInventarioService {

    private final InventarioService inventarioService;
    private final Map<TipoMovimiento, CreadorMovimiento> creadores;

    public AjusteInventarioServiceImpl(InventarioService inventarioService,
                                       @Qualifier("creadorAjustePositivo") CreadorMovimiento creadorAjustePositivo,
                                       @Qualifier("creadorAjusteNegativo") CreadorMovimiento creadorAjusteNegativo) {
        this.inventarioService = inventarioService;
        this.creadores = Map.of(
                TipoMovimiento.AJUSTE_POSITIVO, creadorAjustePositivo,
                TipoMovimiento.AJUSTE_NEGATIVO, creadorAjusteNegativo);
    }

    @Override
    @Transactional
    public MovimientoInventario registrarAjuste(Long varianteId, TipoMovimiento tipo, int cantidad, String motivo) {
        CreadorMovimiento creador = tipo == null ? null : creadores.get(tipo);
        if (creador == null) {
            throw new OperacionInvalidaException("El tipo de ajuste debe ser AJUSTE_POSITIVO o AJUSTE_NEGATIVO");
        }
        MovimientoInventario movimiento = creador.crear(OrigenMovimiento.ninguno(), cantidad, motivo);
        return inventarioService.registrarMovimiento(varianteId, movimiento);
    }
}
