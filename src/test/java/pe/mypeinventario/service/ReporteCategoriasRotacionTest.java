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
import pe.mypeinventario.composite.NodoVentasCategoria;
import pe.mypeinventario.domain.Categoria;
import pe.mypeinventario.domain.MedioPago;
import pe.mypeinventario.domain.MovimientoInventario;
import pe.mypeinventario.domain.ProductoItem;
import pe.mypeinventario.domain.Tienda;
import pe.mypeinventario.domain.TipoMovimiento;
import pe.mypeinventario.domain.VarianteSKU;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.reporte.VarianteSinRotacion;
import pe.mypeinventario.repository.CategoriaRepository;
import pe.mypeinventario.repository.ProductoItemRepository;
import pe.mypeinventario.repository.TiendaRepository;
import pe.mypeinventario.service.dto.LineaVenta;
import pe.mypeinventario.service.dto.SolicitudVenta;

/**
 * Valida el reporte de productos parados y la utilidad por categoría (Composite).
 * <p>
 * Árbol de categorías: Calzado (Zapatillas, Sandalias), Ropa y Accesorios. Hay un
 * producto directamente en Calzado, y Accesorios no tiene ventas.
 */
@SpringBootTest
@Transactional
class ReporteCategoriasRotacionTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired private ReporteService reporteService;
    @Autowired private VentaService ventaService;
    @Autowired private InventarioService inventarioService;
    @Autowired private TiendaRepository tiendaRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private ProductoItemRepository productoRepository;

    private final LocalDate desde = LocalDate.now().minusDays(1);
    private final LocalDate hasta = LocalDate.now().plusDays(1);

    private Long tiendaId;
    private Long zapatillaId;
    private Long sandaliaId;
    private Long genericoId;
    private Long poloId;
    private Long cinturonId;
    private Long bufandaId;
    private Long llaveroId;

    @BeforeEach
    void prepararDatos() {
        Tienda tienda = tiendaRepository.save(new Tienda(null, "Tienda Centro", "Av. Lima 123", "Gamarra"));
        tiendaId = tienda.getId();
        Categoria calzado = categoriaRepository.save(new Categoria(null, "Calzado", "Todo calzado", null));
        Categoria zapatillas = categoriaRepository.save(new Categoria(null, "Zapatillas", "Deportivas", calzado));
        Categoria sandalias = categoriaRepository.save(new Categoria(null, "Sandalias", "De verano", calzado));
        Categoria ropa = categoriaRepository.save(new Categoria(null, "Ropa", "Prendas", null));
        Categoria accesorios = categoriaRepository.save(new Categoria(null, "Accesorios", "Complementos", null));

        zapatillaId = crearProducto(tienda, zapatillas, "Zapatilla Runner", "ZAP-42", "200.00", "120.00");
        sandaliaId = crearProducto(tienda, sandalias, "Sandalia Verano", "SAN-38", "100.00", "60.00");
        genericoId = crearProducto(tienda, calzado, "Calzado genérico", "CAL-GEN", "50.00", "30.00");
        poloId = crearProducto(tienda, ropa, "Polo básico", "POL-M", "118.00", "80.00");
        cinturonId = crearProducto(tienda, accesorios, "Cinturón", "CIN-01", "40.00", "10.00");
        bufandaId = crearProducto(tienda, accesorios, "Bufanda", "BUF-01", "60.00", "25.00");
        llaveroId = crearProducto(tienda, accesorios, "Llavero", "LLA-01", "10.00", "2.00");
        crearProducto(tienda, accesorios, "Gorra", "GOR-01", "30.00", "10.00"); // sin movimientos

        productoRepository.findByVarianteId(llaveroId).orElseThrow().setActivo(false);
        em.flush();
        em.clear();

        stockInicial(zapatillaId, 20);
        stockInicial(sandaliaId, 20);
        stockInicial(genericoId, 20);
        stockInicial(poloId, 20);
        stockInicial(cinturonId, 5);
        stockInicial(bufandaId, 5);
        stockInicial(llaveroId, 5);
    }

    private Long crearProducto(Tienda tienda, Categoria categoria, String nombre, String sku,
                               String precioVenta, String precioCompra) {
        ProductoItem producto = new ProductoItem(null, nombre, "", tienda, null);
        categoria.agregarProducto(producto);
        producto.agregarVariante(new VarianteSKU(null, sku, null, "U", "Negro",
                new BigDecimal(precioCompra), new BigDecimal(precioVenta), 2));
        productoRepository.save(producto);
        return producto.getVariantes().get(0).getId();
    }

    private void stockInicial(Long varianteId, int cantidad) {
        inventarioService.registrarMovimiento(varianteId, new MovimientoInventario(
                null, TipoMovimiento.COMPRA, cantidad, LocalDateTime.now(), "Stock inicial", null, null));
    }

    private void registrarVentaDeCalzadoYRopa() {
        ventaService.registrarVenta(new SolicitudVenta(tiendaId, null, MedioPago.EFECTIVO, List.of(
                new LineaVenta(zapatillaId, 2), new LineaVenta(sandaliaId, 3),
                new LineaVenta(genericoId, 1), new LineaVenta(poloId, 1)), null, null));
    }

    private NodoVentasCategoria buscar(List<NodoVentasCategoria> nodos, String nombre) {
        return nodos.stream().filter(n -> n.nombre().equals(nombre)).findFirst().orElseThrow();
    }

    // ------------------------------------------------- utilidad por categoría

    @Test
    void laUtilidadDeUnaCategoriaSumaLaDeSusSubcategorias() {
        registrarVentaDeCalzadoYRopa();

        List<NodoVentasCategoria> raices = reporteService.obtenerVentasPorCategoria(tiendaId, desde, hasta);

        assertThat(raices).extracting(NodoVentasCategoria::nombre).containsExactlyInAnyOrder("Calzado", "Ropa");

        NodoVentasCategoria calzado = buscar(raices, "Calzado");
        assertThat(calzado.unidades()).isEqualTo(6);
        assertThat(calzado.ingresos()).isEqualByComparingTo("750.00");
        assertThat(calzado.costo()).isEqualByComparingTo("450.00");
        assertThat(calzado.utilidad()).isEqualByComparingTo("300.00");
        assertThat(calzado.subcategorias()).extracting(NodoVentasCategoria::nombre)
                .containsExactlyInAnyOrder("Zapatillas", "Sandalias");

        NodoVentasCategoria zapatillas = buscar(calzado.subcategorias(), "Zapatillas");
        assertThat(zapatillas.unidades()).isEqualTo(2);
        assertThat(zapatillas.utilidad()).isEqualByComparingTo("160.00");
    }

    @Test
    void unaCategoriaSimpleSeTrataIgualQueUnGrupo() {
        registrarVentaDeCalzadoYRopa();

        List<NodoVentasCategoria> raices = reporteService.obtenerVentasPorCategoria(tiendaId, desde, hasta);

        NodoVentasCategoria ropa = buscar(raices, "Ropa");
        assertThat(ropa.subcategorias()).isEmpty();
        assertThat(ropa.unidades()).isEqualTo(1);
        assertThat(ropa.utilidad()).isEqualByComparingTo("38.00");

        // Mismo trato para un grupo (Calzado) y una categoría simple (Ropa).
        BigDecimal utilidadTotal = raices.stream()
                .map(NodoVentasCategoria::utilidad)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(utilidadTotal).isEqualByComparingTo("338.00");
    }

    @Test
    void lasCategoriasSinVentasNoAparecen() {
        registrarVentaDeCalzadoYRopa();

        assertThat(reporteService.obtenerVentasPorCategoria(tiendaId, desde, hasta))
                .extracting(NodoVentasCategoria::nombre).doesNotContain("Accesorios");

        LocalDate hace = LocalDate.now().minusYears(5);
        assertThat(reporteService.obtenerVentasPorCategoria(tiendaId, hace, hace.plusDays(10))).isEmpty();
    }

    // ------------------------------------------------------ productos parados

    @Test
    void losProductosParadosIncluyenLosNuncaVendidosYLosDeVentaAntigua() {
        registrarVentaDeCalzadoYRopa();
        inventarioService.registrarMovimiento(cinturonId, new MovimientoInventario(
                null, TipoMovimiento.VENTA, 1, LocalDateTime.now().minusDays(60), "Venta antigua", null, null));

        List<VarianteSinRotacion> parados = reporteService.obtenerSinRotacion(tiendaId, 30);

        // Nunca vendido primero; luego el de la venta más antigua. No aparecen: lo vendido
        // hace poco, el producto inactivo ni la variante sin stock.
        assertThat(parados).extracting(VarianteSinRotacion::sku).containsExactly("BUF-01", "CIN-01");

        VarianteSinRotacion bufanda = parados.get(0);
        assertThat(bufanda.stock()).isEqualTo(5);
        assertThat(bufanda.ultimaVenta()).isNull();
        assertThat(bufanda.capitalInmovilizado()).isEqualByComparingTo("125.00");

        VarianteSinRotacion cinturon = parados.get(1);
        assertThat(cinturon.stock()).isEqualTo(4);
        assertThat(cinturon.ultimaVenta())
                .isBetween(LocalDateTime.now().minusDays(61), LocalDateTime.now().minusDays(59));
        assertThat(cinturon.capitalInmovilizado()).isEqualByComparingTo("40.00");
    }

    @Test
    void conUnPlazoMasLargoUnaVentaAntiguaDejaDeSerParada() {
        inventarioService.registrarMovimiento(cinturonId, new MovimientoInventario(
                null, TipoMovimiento.VENTA, 1, LocalDateTime.now().minusDays(60), "Venta antigua", null, null));

        List<VarianteSinRotacion> parados = reporteService.obtenerSinRotacion(tiendaId, 90);

        assertThat(parados).extracting(VarianteSinRotacion::sku).doesNotContain("CIN-01");
    }

    @Test
    void losDatosInvalidosSeRechazan() {
        assertThatThrownBy(() -> reporteService.obtenerSinRotacion(tiendaId, 0))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> reporteService.obtenerSinRotacion(null, 30))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> reporteService.obtenerSinRotacion(999_999L, 30))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> reporteService.obtenerVentasPorCategoria(999_999L, desde, hasta))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
