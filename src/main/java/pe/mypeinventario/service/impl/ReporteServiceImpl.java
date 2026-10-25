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
import java.sql.Timestamp;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import pe.mypeinventario.composite.GrupoVentasCategoria;
import pe.mypeinventario.composite.NodoVentasCategoria;
import pe.mypeinventario.composite.VentasDeCategoria;
import pe.mypeinventario.domain.Categoria;
import pe.mypeinventario.domain.TipoMovimiento;
import pe.mypeinventario.reporte.VarianteSinRotacion;
import pe.mypeinventario.repository.CategoriaRepository;

/** Implementación de {@link ReporteService}: solo lectura. */
@Service
@Transactional(readOnly = true)
public class ReporteServiceImpl implements ReporteService {

    private static final int LIMITE_MAXIMO = 100;
    private static final int DIAS_MAXIMOS = 3650;
    /** Identificador de apoyo para agrupar las categorías que no tienen padre. */
    private static final Long ID_RAIZ = 0L;
    private final TiendaRepository tiendaRepository;
    private final CategoriaRepository categoriaRepository;
    private final ReporteVentasRepository reporteRepository;
    private final Clock reloj;

    public ReporteServiceImpl(TiendaRepository tiendaRepository,
                              CategoriaRepository categoriaRepository,
                              ReporteVentasRepository reporteRepository,
                              Clock reloj) {
        this.tiendaRepository = tiendaRepository;
        this.categoriaRepository = categoriaRepository;
        this.reporteRepository = reporteRepository;
        this.reloj = reloj;
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

    @Override
    public List<VarianteSinRotacion> obtenerSinRotacion(Long tiendaId, int dias) {
        if (tiendaId == null) {
            throw new OperacionInvalidaException("La tienda es obligatoria");
        }
        if (dias < 1 || dias > DIAS_MAXIMOS) {
            throw new OperacionInvalidaException("Los días deben estar entre 1 y " + DIAS_MAXIMOS);
        }
        if (!tiendaRepository.existsById(tiendaId)) {
            throw new RecursoNoEncontradoException("Tienda", tiendaId);
        }
        LocalDateTime limite = LocalDateTime.now(reloj).minusDays(dias);
        return reporteRepository.variantesConStockSinVentasDesde(
                        tiendaId, TipoMovimiento.entradas(), TipoMovimiento.VENTA, limite).stream()
                .map(this::aVarianteSinRotacion)
                .sorted(Comparator.comparing(VarianteSinRotacion::ultimaVenta,
                        Comparator.nullsFirst(Comparator.<LocalDateTime>naturalOrder())))
                .toList();
    }

    @Override
    public List<NodoVentasCategoria> obtenerVentasPorCategoria(Long tiendaId, LocalDate desde, LocalDate hasta) {
        validarPeriodo(tiendaId, desde, hasta);
        Map<Long, Object[]> ventas = reporteRepository
                .ventasPorCategoria(tiendaId, inicio(desde), finExclusivo(hasta)).stream()
                .collect(Collectors.toMap(fila -> (Long) fila[0], fila -> fila));

        Map<Long, List<Categoria>> hijosDe = new HashMap<>();
        for (Categoria categoria : categoriaRepository.findAll()) {
            Long padreId = categoria.getCategoriaPadre() == null ? ID_RAIZ : categoria.getCategoriaPadre().getId();
            hijosDe.computeIfAbsent(padreId, id -> new ArrayList<>()).add(categoria);
        }
        return hijosDe.getOrDefault(ID_RAIZ, List.of()).stream()
                .map(raiz -> construirNodo(raiz, hijosDe, ventas))
                .flatMap(Optional::stream)
                .toList();
    }

    /** Arma el árbol: una categoría con hijos con ventas es un grupo; sin hijos, una hoja. */
    private Optional<NodoVentasCategoria> construirNodo(Categoria categoria,
                                                        Map<Long, List<Categoria>> hijosDe,
                                                        Map<Long, Object[]> ventas) {
        VentasDeCategoria propias = ventasPropias(categoria, ventas.get(categoria.getId()));
        List<NodoVentasCategoria> hijos = hijosDe.getOrDefault(categoria.getId(), List.of()).stream()
                .map(hija -> construirNodo(hija, hijosDe, ventas))
                .flatMap(Optional::stream)
                .toList();
        if (hijos.isEmpty()) {
            return propias.unidades() > 0 ? Optional.<NodoVentasCategoria>of(propias) : Optional.empty();
        }
        GrupoVentasCategoria grupo = new GrupoVentasCategoria(categoria.getNombre(), propias);
        hijos.forEach(grupo::agregar);
        return Optional.<NodoVentasCategoria>of(grupo);
    }

    private VentasDeCategoria ventasPropias(Categoria categoria, Object[] fila) {
        if (fila == null) {
            return new VentasDeCategoria(categoria.getNombre(), 0, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        return new VentasDeCategoria(categoria.getNombre(), ((Number) fila[1]).longValue(),
                aMonto(fila[2]), aMonto(fila[3]));
    }

    private VarianteSinRotacion aVarianteSinRotacion(Object[] fila) {
        long stock = ((Number) fila[6]).longValue();
        BigDecimal costo = (BigDecimal) fila[5];
        BigDecimal capital = costo == null ? null : costo.multiply(BigDecimal.valueOf(stock));
        return new VarianteSinRotacion((Long) fila[0], (String) fila[1], (String) fila[2],
                (String) fila[3], (String) fila[4], stock, aFecha(fila[7]), capital);
    }

    private static BigDecimal aMonto(Object valor) {
        return valor == null ? BigDecimal.ZERO : new BigDecimal(valor.toString());
    }

    /** Acepta el tipo de fecha que devuelva la base de datos. */
    private static LocalDateTime aFecha(Object valor) {
        if (valor == null) {
            return null;
        }
        return valor instanceof Timestamp marca ? marca.toLocalDateTime() : (LocalDateTime) valor;
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
