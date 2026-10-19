package pe.mypeinventario.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.mypeinventario.domain.AlertaReabastecimiento;
import pe.mypeinventario.domain.EstadoAlerta;
import pe.mypeinventario.domain.MovimientoInventario;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.repository.AlertaReabastecimientoRepository;
import pe.mypeinventario.repository.MovimientoInventarioRepository;
import pe.mypeinventario.repository.TiendaRepository;
import pe.mypeinventario.repository.VarianteSKURepository;
import pe.mypeinventario.service.ConsultaInventarioService;

/** Implementación de {@link ConsultaInventarioService}: solo lectura. */
@Service
@Transactional(readOnly = true)
public class ConsultaInventarioServiceImpl implements ConsultaInventarioService {

    private final VarianteSKURepository varianteRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final AlertaReabastecimientoRepository alertaRepository;
    private final TiendaRepository tiendaRepository;

    public ConsultaInventarioServiceImpl(VarianteSKURepository varianteRepository,
                                         MovimientoInventarioRepository movimientoRepository,
                                         AlertaReabastecimientoRepository alertaRepository,
                                         TiendaRepository tiendaRepository) {
        this.varianteRepository = varianteRepository;
        this.movimientoRepository = movimientoRepository;
        this.alertaRepository = alertaRepository;
        this.tiendaRepository = tiendaRepository;
    }

    @Override
    public int consultarStock(Long varianteId) {
        exigirVariante(varianteId);
        return varianteRepository.calcularStock(varianteId);
    }

    @Override
    public List<MovimientoInventario> obtenerHistorial(Long varianteId) {
        exigirVariante(varianteId);
        return movimientoRepository.listarPorVariante(varianteId);
    }

    @Override
    public List<AlertaReabastecimiento> listarAlertas(Long tiendaId, EstadoAlerta estado) {
        if (!tiendaRepository.existsById(tiendaId)) {
            throw new RecursoNoEncontradoException("Tienda", tiendaId);
        }
        return alertaRepository.listarPorTiendaYEstado(tiendaId, estado);
    }

    private void exigirVariante(Long varianteId) {
        if (!varianteRepository.existsById(varianteId)) {
            throw new RecursoNoEncontradoException("Variante", varianteId);
        }
    }
}