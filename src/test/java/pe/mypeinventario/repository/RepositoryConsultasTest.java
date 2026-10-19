package pe.mypeinventario.repository;

import static org.assertj.core.api.Assertions.assertThat;

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
import pe.mypeinventario.domain.AlertaReabastecimiento;
import pe.mypeinventario.domain.Categoria;
import pe.mypeinventario.domain.Cliente;
import pe.mypeinventario.domain.Comerciante;
import pe.mypeinventario.domain.Compra;
import pe.mypeinventario.domain.DetalleCompra;
import pe.mypeinventario.domain.DetalleVenta;
import pe.mypeinventario.domain.EstadoAlerta;
import pe.mypeinventario.domain.Marca;
import pe.mypeinventario.domain.MedioPago;
import pe.mypeinventario.domain.MovimientoInventario;
import pe.mypeinventario.domain.PlanSuscripcion;
import pe.mypeinventario.domain.ProductoItem;
import pe.mypeinventario.domain.ProveedorMayorista;
import pe.mypeinventario.domain.Suscripcion;
import pe.mypeinventario.domain.Tienda;
import pe.mypeinventario.domain.TipoMovimiento;
import pe.mypeinventario.domain.VarianteSKU;
import pe.mypeinventario.domain.VentaPunto;

/**
 * Valida contra H2 que las consultas de los repositories se ejecutan y devuelven
 * los datos esperados. Cada prueba corre en una transacción que se revierte al
 * terminar, así que no deja datos entre pruebas.
 */
@SpringBootTest
@Transactional
class RepositoryConsultasTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired private TiendaRepository tiendaRepository;
    @Autowired private ComercianteRepository comercianteRepository;
    @Autowired private ProveedorMayoristaRepository proveedorRepository;
    @Autowired private PlanSuscripcionRepository planRepository;
    @Autowired private SuscripcionRepository suscripcionRepository;
    @Autowired private MarcaRepository marcaRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private ProductoItemRepository productoRepository;
    @Autowired private VarianteSKURepository varianteRepository;
    @Autowired private MovimientoInventarioRepository movimientoRepository;
    @Autowired private AlertaReabastecimientoRepository alertaRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private CompraRepository compraRepository;
    @Autowired private VentaPuntoRepository ventaRepository;

    private Long tienda1Id;
    private Long tienda2Id;
    private Long comercianteId;
    private Long proveedorId;
    private Long categoriaRaizId;
    private Long categoriaSubId;
    private Long producto1Id;
    private Long varianteAId;
    private Long varianteBId;
    private Long varianteTienda2Id;

    /**
     * Datos de prueba. La variante A (tienda 1) queda con stock 6:
     * +10 compra, -3 venta, +1 devolución, -2 ajuste negativo.
     */
    @BeforeEach
    void prepararDatos() {
        Tienda tienda1 = tiendaRepository.save(new Tienda(null, "Tienda Centro", "Av. Lima 123", "Gamarra"));
        Tienda tienda2 = tiendaRepository.save(new Tienda(null, "Tienda Norte", "Av. Norte 456", "Mesa Redonda"));
        tienda1Id = tienda1.getId();
        tienda2Id = tienda2.getId();

        PlanSuscripcion plan = planRepository.save(new PlanSuscripcion(null, "Basico", new BigDecimal("20.00"), 100));
        Comerciante comerciante = new Comerciante(null, "Ana Quispe", "ana@correo.com", "999111222",
                "hash", LocalDate.of(2026, 10, 1));
        comerciante.agregarTienda(tienda1);
        comerciante.agregarSuscripcion(new Suscripcion(null, plan, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)));
        comercianteId = comercianteRepository.save(comerciante).getId();

        proveedorId = proveedorRepository.save(new ProveedorMayorista(null, "Mayorista Sur", "sur@correo.com",
                "988777666", "hash", "Distribuidora Sur SAC", "20123456789")).getId();

        Marca marca = marcaRepository.save(new Marca(null, "Runner"));
        Categoria raiz = categoriaRepository.save(new Categoria(null, "Calzado", "Todo calzado", null));
        Categoria sub = categoriaRepository.save(new Categoria(null, "Zapatillas", "Deportivas", raiz));
        categoriaRaizId = raiz.getId();
        categoriaSubId = sub.getId();

        ProductoItem producto1 = new ProductoItem(null, "Zapatilla Runner", "Running", tienda1, marca);
        sub.agregarProducto(producto1);
        producto1.agregarVariante(new VarianteSKU(null, "RUN-42-NEG", "7751234567890", "42", "Negro",
                new BigDecimal("80.00"), new BigDecimal("129.90"), null));
        producto1.agregarVariante(new VarianteSKU(null, "RUN-40-BLA", null, "40", "Blanco",
                new BigDecimal("80.00"), new BigDecimal("129.90"), 3));
        productoRepository.save(producto1);
        producto1Id = producto1.getId();
        VarianteSKU varianteA = producto1.getVariantes().get(0);
        varianteAId = varianteA.getId();
        varianteBId = producto1.getVariantes().get(1).getId();

        // Mismo SKU en otra tienda: debe poder repetirse entre tiendas.
        ProductoItem producto2 = new ProductoItem(null, "Zapatilla Runner", "Running", tienda2, null);
        sub.agregarProducto(producto2);
        producto2.agregarVariante(new VarianteSKU(null, "RUN-42-NEG", null, "42", "Negro",
                new BigDecimal("80.00"), new BigDecimal("129.90"), null));
        productoRepository.save(producto2);
        varianteTienda2Id = producto2.getVariantes().get(0).getId();

        varianteA.registrarMovimiento(movimiento(TipoMovimiento.COMPRA, 10, 9));
        varianteA.registrarMovimiento(movimiento(TipoMovimiento.VENTA, 3, 10));
        varianteA.registrarMovimiento(movimiento(TipoMovimiento.DEVOLUCION_CLIENTE, 1, 11));
        varianteA.registrarMovimiento(movimiento(TipoMovimiento.AJUSTE_NEGATIVO, 2, 12));

        em.flush();
        em.clear();
    }

    private MovimientoInventario movimiento(TipoMovimiento tipo, int cantidad, int hora) {
        return new MovimientoInventario(null, tipo, cantidad, LocalDateTime.of(2026, 10, 2, hora, 0),
                "Prueba", null, null);
    }

    private VarianteSKU variante(Long id) {
        return varianteRepository.findById(id).orElseThrow();
    }

    @Test
    void stockCalculadoEnBaseCoincideConElCalculoDelDominio() {
        int enBase = varianteRepository.calcularStock(varianteAId);

        assertThat(enBase).isEqualTo(6);
        assertThat(variante(varianteAId).calcularStock()).isEqualTo(6);
    }

    @Test
    void varianteSinMovimientosTieneStockCero() {
        assertThat(varianteRepository.calcularStock(varianteBId)).isZero();
    }

    @Test
    void skuYCodigoDeBarrasSeBuscanDentroDeCadaTienda() {
        assertThat(varianteRepository.buscarPorSkuEnTienda(tienda1Id, "RUN-42-NEG"))
                .get().extracting(VarianteSKU::getId).isEqualTo(varianteAId);
        assertThat(varianteRepository.buscarPorSkuEnTienda(tienda2Id, "RUN-42-NEG"))
                .get().extracting(VarianteSKU::getId).isEqualTo(varianteTienda2Id);
        assertThat(varianteRepository.buscarPorSkuEnTienda(tienda1Id, "NO-EXISTE")).isEmpty();
        assertThat(varianteRepository.buscarPorCodigoBarrasEnTienda(tienda1Id, "7751234567890"))
                .get().extracting(VarianteSKU::getId).isEqualTo(varianteAId);
    }

    @Test
    void variantesYProductosSeListanPorTienda() {
        assertThat(varianteRepository.listarPorTienda(tienda1Id)).hasSize(2);
        assertThat(varianteRepository.listarPorTienda(tienda2Id)).hasSize(1);
        assertThat(productoRepository.findByTiendaId(tienda1Id)).hasSize(1);
        assertThat(productoRepository.findByTiendaIdAndActivoTrue(tienda1Id)).hasSize(1);
        assertThat(productoRepository.findByCategoriaId(categoriaSubId)).hasSize(2);
    }

    @Test
    void productoSeObtienePorUnaDeSusVariantes() {
        ProductoItem producto = productoRepository.findByVarianteId(varianteAId).orElseThrow();

        assertThat(producto.getId()).isEqualTo(producto1Id);
        assertThat(producto.getTienda().getId()).isEqualTo(tienda1Id);
    }

    @Test
    void categoriasRaizEHijas() {
        assertThat(categoriaRepository.findByCategoriaPadreIsNull())
                .extracting(Categoria::getId).containsExactly(categoriaRaizId);
        assertThat(categoriaRepository.findByCategoriaPadreId(categoriaRaizId))
                .extracting(Categoria::getNombre).containsExactly("Zapatillas");
    }

    @Test
    void tiendasYSuscripcionesSeListanPorComerciante() {
        assertThat(tiendaRepository.listarPorComerciante(comercianteId))
                .extracting(Tienda::getNombre).containsExactly("Tienda Centro");
        assertThat(suscripcionRepository.listarPorComerciante(comercianteId)).hasSize(1);
    }

    @Test
    void historialSeOrdenaDelMasRecienteAlMasAntiguo() {
        List<MovimientoInventario> historial = movimientoRepository.listarPorVariante(varianteAId);

        assertThat(historial).hasSize(4);
        assertThat(historial.get(0).getTipo()).isEqualTo(TipoMovimiento.AJUSTE_NEGATIVO);
        assertThat(historial.get(3).getTipo()).isEqualTo(TipoMovimiento.COMPRA);
    }

    @Test
    void alertasSeConsultanPorVarianteTiendaYEstado() {
        VarianteSKU variante = variante(varianteAId);
        variante.agregarAlerta(new AlertaReabastecimiento(null, 4, 5));
        varianteRepository.saveAndFlush(variante);
        em.clear();

        assertThat(alertaRepository.listarPorVariante(varianteAId)).hasSize(1);
        assertThat(alertaRepository.listarPorTiendaYEstado(tienda1Id, EstadoAlerta.PENDIENTE)).hasSize(1);
        assertThat(alertaRepository.listarPorTiendaYEstado(tienda1Id, EstadoAlerta.ATENDIDA)).isEmpty();
        assertThat(alertaRepository.listarPorTiendaYEstado(tienda2Id, EstadoAlerta.PENDIENTE)).isEmpty();
        assertThat(alertaRepository.existeAlertaEnEstados(varianteAId,
                List.of(EstadoAlerta.PENDIENTE, EstadoAlerta.VISTA))).isTrue();
        assertThat(alertaRepository.existeAlertaEnEstados(varianteAId,
                List.of(EstadoAlerta.ATENDIDA))).isFalse();
    }

    @Test
    void comprasSeListanPorTiendaYProveedor() {
        Compra compra = new Compra(null, LocalDateTime.of(2026, 10, 5, 10, 0),
                tiendaRepository.findById(tienda1Id).orElseThrow(),
                proveedorRepository.findById(proveedorId).orElseThrow());
        compra.agregarDetalle(new DetalleCompra(null, variante(varianteAId), 5, new BigDecimal("80.00")));
        compraRepository.save(compra);
        em.flush();
        em.clear();

        List<Compra> compras = compraRepository.findByTiendaIdOrderByFechaDesc(tienda1Id);
        assertThat(compras).hasSize(1);
        assertThat(compras.get(0).getTotal()).isEqualByComparingTo("400.00");
        assertThat(compraRepository.findByProveedorId(proveedorId)).hasSize(1);
        assertThat(compraRepository.findByTiendaIdOrderByFechaDesc(tienda2Id)).isEmpty();
    }

    @Test
    void ventasSeListanPorTiendaYRangoDeFechas() {
        Tienda tienda = tiendaRepository.findById(tienda1Id).orElseThrow();
        Cliente cliente = clienteRepository.save(new Cliente(null, "Luis Rojas", "977000111", "12345678", tienda));
        VentaPunto venta = new VentaPunto(null, LocalDateTime.of(2026, 10, 6, 15, 30),
                MedioPago.YAPE, tienda, cliente);
        venta.agregarDetalle(new DetalleVenta(null, variante(varianteAId), 2, new BigDecimal("129.90")));
        ventaRepository.save(venta);
        em.flush();
        em.clear();

        assertThat(ventaRepository.findByTiendaIdOrderByFechaDesc(tienda1Id)).hasSize(1);
        assertThat(ventaRepository.findByTiendaIdAndFechaBetween(tienda1Id,
                LocalDateTime.of(2026, 10, 6, 0, 0), LocalDateTime.of(2026, 10, 6, 23, 59))).hasSize(1);
        assertThat(ventaRepository.findByTiendaIdAndFechaBetween(tienda1Id,
                LocalDateTime.of(2026, 10, 7, 0, 0), LocalDateTime.of(2026, 10, 7, 23, 59))).isEmpty();
        assertThat(clienteRepository.findByTiendaId(tienda1Id)).hasSize(1);
        assertThat(clienteRepository.findByTiendaIdAndDocumento(tienda1Id, "12345678")).isPresent();
        assertThat(clienteRepository.findByTiendaIdAndDocumento(tienda2Id, "12345678")).isEmpty();
    }

    @Test
    void busquedasPorClaveDeNegocio() {
        assertThat(comercianteRepository.findByCorreo("ana@correo.com")).isPresent();
        assertThat(comercianteRepository.existsByCorreo("ana@correo.com")).isTrue();
        assertThat(comercianteRepository.existsByCorreo("otro@correo.com")).isFalse();
        assertThat(proveedorRepository.findByRuc("20123456789")).isPresent();
        assertThat(proveedorRepository.existsByRuc("20999999999")).isFalse();
        assertThat(planRepository.findByNombre("Basico")).isPresent();
        assertThat(marcaRepository.findByNombreIgnoreCase("runner")).isPresent();
    }
}
