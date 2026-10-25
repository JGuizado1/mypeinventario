package pe.mypeinventario.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.reporte.ProductoVendido;
import pe.mypeinventario.reporte.ResumenVentas;
import pe.mypeinventario.reporte.TotalesVentas;
import pe.mypeinventario.repository.ReporteVentasRepository;
import pe.mypeinventario.repository.TiendaRepository;
import pe.mypeinventario.service.ReporteService;

/** Implementación de {@link ReporteService}: solo lectura. */
@Service
@Transactional(readOnly = true)
public class ReporteServiceImpl implements ReporteService {

    private static final int LIMITE_MAXIMO = 100;

    private final TiendaRepository tiendaRepository;
    private final ReporteVentasRepository reporteRepository;

    public ReporteServiceImpl(TiendaRepository tiendaRepository, ReporteVentasRepository reporteRepository) {
        this.tiendaRepository = tiendaRepository;
        this.reporteRepository = reporteRepository;
    }

    @Override
    public List<ProductoVendido> obtenerMasVendidos(Long tiendaId, LocalDate desde, LocalDate hasta, int limite) {
        validarPeriodo(tiendaId, desde, hasta);
        if (limite < 1 || limite > LIMITE_MAXIMO) {
            throw new OperacionInvalidaException("El límite debe estar entre 1 y " + LIMITE_MAXIMO);
        }
        return reporteRepository.masVendidos(tiendaId, inicio(desde), finExclusivo(hasta),
                PageRequest.of(0, limite));
    }

    @Override
    public ResumenVentas obtenerResumen(Long tiendaId, LocalDate desde, LocalDate hasta) {
        validarPeriodo(tiendaId, desde, hasta);
        LocalDateTime inicio = inicio(desde);
        LocalDateTime fin = finExclusivo(hasta);

        TotalesVentas totales = reporteRepository.totalesDelPeriodo(tiendaId, inicio, fin);
        BigDecimal costo = reporteRepository.costoDelPeriodo(tiendaId, inicio, fin);
        costo = costo == null ? BigDecimal.ZERO : costo;
        long lineasSinCosto = reporteRepository.contarLineasSinCosto(tiendaId, inicio, fin);

        return new ResumenVentas(totales.cantidadVentas(), totales.subtotal(), totales.descuento(),
                totales.total(), totales.igv(), costo, totales.total().subtract(costo), lineasSinCosto);
    }

    private void validarPeriodo(Long tiendaId, LocalDate desde, LocalDate hasta) {
        if (tiendaId == null || desde == null || hasta == null) {
            throw new OperacionInvalidaException("La tienda y el periodo son obligatorios");
        }
        if (hasta.isBefore(desde)) {
            throw new OperacionInvalidaException("La fecha final no puede ser anterior a la inicial");
        }
        if (!tiendaRepository.existsById(tiendaId)) {
            throw new RecursoNoEncontradoException("Tienda", tiendaId);
        }
    }

    private static LocalDateTime inicio(LocalDate desde) {
        return desde.atStartOfDay();
    }

    /** El último día del periodo se incluye completo: el intervalo termina al inicio del día siguiente. */
    private static LocalDateTime finExclusivo(LocalDate hasta) {
        return hasta.plusDays(1).atStartOfDay();
    }
}
