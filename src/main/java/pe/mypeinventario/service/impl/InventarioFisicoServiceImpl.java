package pe.mypeinventario.service.impl;

import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.mypeinventario.domain.DetalleTomaInventario;
import pe.mypeinventario.domain.EstadoToma;
import pe.mypeinventario.domain.Tienda;
import pe.mypeinventario.domain.TipoMovimiento;
import pe.mypeinventario.domain.TomaInventario;
import pe.mypeinventario.domain.VarianteSKU;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.repository.TiendaRepository;
import pe.mypeinventario.repository.TomaInventarioRepository;
import pe.mypeinventario.repository.VarianteSKURepository;
import pe.mypeinventario.service.AjusteInventarioService;
import pe.mypeinventario.service.InventarioFisicoService;

/** Implementación de {@link InventarioFisicoService}. */
@Service
public class InventarioFisicoServiceImpl implements InventarioFisicoService {

    private final TiendaRepository tiendaRepository;
    private final TomaInventarioRepository tomaRepository;
    private final VarianteSKURepository varianteRepository;
    private final VariantesDeTienda variantesDeTienda;
    private final AjusteInventarioService ajusteService;
    private final Clock reloj;

    public InventarioFisicoServiceImpl(TiendaRepository tiendaRepository,
                                       TomaInventarioRepository tomaRepository,
                                       VarianteSKURepository varianteRepository,
                                       VariantesDeTienda variantesDeTienda,
                                       AjusteInventarioService ajusteService,
                                       Clock reloj) {
        this.tiendaRepository = tiendaRepository;
        this.tomaRepository = tomaRepository;
        this.varianteRepository = varianteRepository;
        this.variantesDeTienda = variantesDeTienda;
        this.ajusteService = ajusteService;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public TomaInventario iniciarToma(Long tiendaId) {
        if (tiendaId == null) {
            throw new OperacionInvalidaException("La tienda es obligatoria");
        }
        Tienda tienda = tiendaRepository.findById(tiendaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tienda", tiendaId));
        if (tomaRepository.existsByTiendaIdAndEstado(tiendaId, EstadoToma.EN_CURSO)) {
            throw new OperacionInvalidaException("Ya hay una toma de inventario en curso en la tienda " + tiendaId);
        }
        return tomaRepository.save(new TomaInventario(null, LocalDateTime.now(reloj), tienda));
    }

    @Override
    @Transactional
    public TomaInventario registrarConteo(Long tomaId, Long varianteId, int stockContado) {
        if (stockContado < 0) {
            throw new OperacionInvalidaException("El stock contado no puede ser negativo");
        }
        TomaInventario toma = obtenerEntidad(tomaId);
        if (varianteId == null) {
            throw new OperacionInvalidaException("La variante es obligatoria");
        }
        // Se puede contar un producto inactivo: el stock físico existe aunque ya no se venda.
        VarianteSKU variante = variantesDeTienda.obtener(varianteId, toma.getTienda().getId(), false);
        int stockSistema = varianteRepository.calcularStock(varianteId);
        try {
            toma.agregarConteo(variante, stockSistema, stockContado);
        } catch (IllegalStateException e) {
            throw new OperacionInvalidaException(e.getMessage(), e);
        }
        return toma;
    }

    @Override
    @Transactional
    public TomaInventario cerrarToma(Long tomaId) {
        TomaInventario toma = obtenerEntidad(tomaId);
        // Se valida antes de ajustar, para no aplicar dos veces los ajustes de una toma cerrada.
        if (toma.estaCerrada()) {
            throw new OperacionInvalidaException("La toma de inventario ya está cerrada");
        }
        if (toma.getDetalles().isEmpty()) {
            throw new OperacionInvalidaException("No se puede cerrar una toma sin conteos");
        }
        for (DetalleTomaInventario detalle : toma.getDetalles()) {
            aplicarDiferencia(toma, detalle);
        }
        try {
            toma.cerrar();
        } catch (IllegalStateException e) {
            throw new OperacionInvalidaException(e.getMessage(), e);
        }
        return toma;
    }

    @Override
    @Transactional(readOnly = true)
    public TomaInventario obtenerToma(Long tomaId) {
        return obtenerEntidad(tomaId);
    }

    /** Cada diferencia se aplica como un ajuste positivo o negativo, con la toma como motivo. */
    private void aplicarDiferencia(TomaInventario toma, DetalleTomaInventario detalle) {
        int diferencia = detalle.calcularDiferencia();
        if (diferencia == 0) {
            return;
        }
        TipoMovimiento tipo = diferencia > 0 ? TipoMovimiento.AJUSTE_POSITIVO : TipoMovimiento.AJUSTE_NEGATIVO;
        String motivo = "Toma de inventario #" + toma.getId() + ": contado " + detalle.getStockContado()
                + ", sistema " + detalle.getStockSistema();
        ajusteService.registrarAjuste(detalle.getVariante().getId(), tipo, Math.abs(diferencia), motivo);
    }

    private TomaInventario obtenerEntidad(Long tomaId) {
        if (tomaId == null) {
            throw new OperacionInvalidaException("La toma es obligatoria");
        }
        return tomaRepository.findById(tomaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Toma de inventario", tomaId));
    }
}