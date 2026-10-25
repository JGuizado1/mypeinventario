package pe.mypeinventario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.mypeinventario.domain.Categoria;
import pe.mypeinventario.domain.MedioPago;
import pe.mypeinventario.domain.MovimientoInventario;
import pe.mypeinventario.domain.ProductoItem;
import pe.mypeinventario.domain.Tienda;
import pe.mypeinventario.domain.TipoMovimiento;
import pe.mypeinventario.domain.VarianteSKU;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.reporte.ProductoVendido;
import pe.mypeinventario.reporte.ResumenVentas;
import pe.mypeinventario.repository.CategoriaRepository;
import pe.mypeinventario.repository.ProductoItemRepository;
import pe.mypeinventario.repository.TiendaRepository;
import pe.mypeinventario.repository.VarianteSKURepository;
import pe.mypeinventario.service.dto.LineaVenta;
import pe.mypeinventario.service.dto.SolicitudVenta;

/**
 * Valida los reportes de ventas: más vendidos, resumen del periodo y costo congelado.
 * <p>
 * Datos: variante A (venta 118.00, costo 80.00), B (59.00, 40.00) y C (30.00, sin costo).
 * Venta 1: 3 de A y 1 de B, sin descuento. Venta 2: 2 de A con 10 % de descuento.
 */
@SpringBootTest
@Transactional
class ReporteServiceTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired private ReporteService reporteService;
    @Autowired private VentaService ventaService;
    @Autowired private InventarioService inventarioService;
    @Autowired private TiendaRepository tiendaRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private ProductoItemRepository productoRepository;
    @Autowired private VarianteSKURepository varianteRepository;

    private final LocalDate desde = LocalDate.now().minusDays(1);
    private final LocalDate hasta = LocalDate.now().plusDays(1);

    private Long tiendaId;
    private Long varianteAId;
    private Long varianteBId;
    private Long varianteCId;

    @BeforeEach
    void prepararDatos() {
        Tienda tienda = tiendaRepository.save(new Tienda(null, "Tienda Centro", "Av. Lima 123", "Gamarra"));
        Categoria categoria = categoriaRepository.save(new Categoria(null, "Polos", "Polos de algodón", null));
        ProductoItem producto = new ProductoItem(null, "Polo básico", "Algodón", tienda, null);
        categoria.agregarProducto(producto);
        producto.agregarVariante(new VarianteSKU(null, "POL-M-AZU", null, "M", "Azul",
                new BigDecimal("80.00"), new BigDecimal("118.00"), 2));
        producto.agregarVariante(new VarianteSKU(null, "POL-L-ROJ", null, "L", "Rojo",
                new BigDecimal("40.00"), new BigDecimal("59.00"), 2));
        producto.agregarVariante(new VarianteSKU(null, "POL-S-VER", null, "S", "Verde",
                null, new BigDecimal("30.00"), 2));
        productoRepository.save(producto);
        tiendaId = tienda.getId();
        varianteAId = producto.getVariantes().get(0).getId();
        varianteBId = producto.getVariantes().get(1).getId();
        varianteCId = producto.getVariantes().get(2).getId();
        em.flush();
        em.clear();

        for (Long varianteId : List.of(varianteAId, varianteBId, varianteCId)) {
            inventarioService.registrarMovimiento(varianteId, new MovimientoInventario(
                    null, TipoMovimiento.COMPRA, 20, LocalDateTime.now(), "Stock inicial", null, null));
        }
    }

    private void registrarVentasDeEjemplo() {
        ventaService.registrarVenta(new SolicitudVenta(tiendaId, null, MedioPago.EFECTIVO,
                List.of(new LineaVenta(varianteAId, 3), new LineaVenta(varianteBId, 1)), null, null));
        ventaService.registrarVenta(new SolicitudVenta(tiendaId, null, MedioPago.YAPE,
                List.of(new LineaVenta(varianteAId, 2)), new BigDecimal("10"), null));
    }

    @Test
    void losMasVendidosSeOrdenanPorUnidades() {
        registrarVentasDeEjemplo();

        List<ProductoVendido> masVendidos = reporteService.obtenerMasVendidos(tiendaId, desde, hasta, 10);

        assertThat(masVendidos).hasSize(2);
        assertThat(masVendidos.get(0).sku()).isEqualTo("POL-M-AZU");
        assertThat(masVendidos.get(0).producto()).isEqualTo("Polo básico");
        assertThat(masVendidos.get(0).unidades()).isEqualTo(5L);
        assertThat(masVendidos.get(0).ingresos()).isEqualByComparingTo("590.00");
        assertThat(masVendidos.get(1).sku()).isEqualTo("POL-L-ROJ");
        assertThat(masVendidos.get(1).unidades()).isEqualTo(1L);
        assertThat(masVendidos.get(1).ingresos()).isEqualByComparingTo("59.00");
    }

    @Test
    void elLimiteRecortaLaLista() {
        registrarVentasDeEjemplo();

        assertThat(reporteService.obtenerMasVendidos(tiendaId, desde, hasta, 1))
                .extracting(ProductoVendido::sku).containsExactly("POL-M-AZU");
    }

    @Test
    void unPeriodoSinVentasDevuelveListaVaciaYCeros() {
        LocalDate hace = LocalDate.now().minusYears(5);

        assertThat(reporteService.obtenerMasVendidos(tiendaId, hace, hace.plusDays(10), 10)).isEmpty();

        ResumenVentas resumen = reporteService.obtenerResumen(tiendaId, hace, hace.plusDays(10));
        assertThat(resumen.cantidadVentas()).isZero();
        assertThat(resumen.totalCobrado()).isEqualByComparingTo("0");
        assertThat(resumen.costo()).isEqualByComparingTo("0");
        assertThat(resumen.utilidadBruta()).isEqualByComparingTo("0");
    }

    @Test
    void elResumenSumaVentasDescuentosIgvCostoYUtilidad() {
        registrarVentasDeEjemplo();

        ResumenVentas resumen = reporteService.obtenerResumen(tiendaId, desde, hasta);

        assertThat(resumen.cantidadVentas()).isEqualTo(2);
        assertThat(resumen.ventasBrutas()).isEqualByComparingTo("649.00");
        assertThat(resumen.descuentos()).isEqualByComparingTo("23.60");
        assertThat(resumen.totalCobrado()).isEqualByComparingTo("625.40");
        assertThat(resumen.igv()).isEqualByComparingTo("95.40");
        assertThat(resumen.costo()).isEqualByComparingTo("440.00");
        assertThat(resumen.utilidadBruta()).isEqualByComparingTo("185.40");
        assertThat(resumen.lineasSinCosto()).isZero();
    }

    @Test
    void lasLineasSinCostoSeCuentanYNoSeSumanAlCosto() {
        ventaService.registrarVenta(new SolicitudVenta(tiendaId, null, MedioPago.EFECTIVO,
                List.of(new LineaVenta(varianteCId, 1)), null, null));

        ResumenVentas resumen = reporteService.obtenerResumen(tiendaId, desde, hasta);

        assertThat(resumen.lineasSinCosto()).isEqualTo(1);
        assertThat(resumen.costo()).isEqualByComparingTo("0");
        assertThat(resumen.totalCobrado()).isEqualByComparingTo("30.00");
    }

    @Test
    void elCostoDeUnaVentaPasadaNoCambiaSiLaVarianteCambiaDeCosto() {
        registrarVentasDeEjemplo();
        VarianteSKU varianteA = varianteRepository.findById(varianteAId).orElseThrow();
        varianteA.setPrecioCompra(new BigDecimal("100.00"));
        em.flush();
        em.clear();

        assertThat(reporteService.obtenerResumen(tiendaId, desde, hasta).costo())
                .isEqualByComparingTo("440.00");
    }

    @Test
    void losDatosInvalidosSeRechazan() {
        assertThatThrownBy(() -> reporteService.obtenerResumen(999_999L, desde, hasta))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> reporteService.obtenerResumen(tiendaId, hasta, desde))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> reporteService.obtenerResumen(tiendaId, null, hasta))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> reporteService.obtenerMasVendidos(tiendaId, desde, hasta, 0))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> reporteService.obtenerMasVendidos(tiendaId, desde, hasta, 101))
                .isInstanceOf(OperacionInvalidaException.class);
    }
}
