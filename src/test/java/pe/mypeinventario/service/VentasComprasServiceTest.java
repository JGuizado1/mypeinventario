package pe.mypeinventario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.mypeinventario.domain.Categoria;
import pe.mypeinventario.domain.Cliente;
import pe.mypeinventario.domain.Compra;
import pe.mypeinventario.domain.EstadoAlerta;
import pe.mypeinventario.domain.MedioPago;
import pe.mypeinventario.domain.MovimientoInventario;
import pe.mypeinventario.domain.ProductoItem;
import pe.mypeinventario.domain.ProveedorMayorista;
import pe.mypeinventario.domain.Tienda;
import pe.mypeinventario.domain.TipoMovimiento;
import pe.mypeinventario.domain.VarianteSKU;
import pe.mypeinventario.domain.VentaPunto;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.exception.StockInsuficienteException;
import pe.mypeinventario.repository.CategoriaRepository;
import pe.mypeinventario.repository.ClienteRepository;
import pe.mypeinventario.repository.ProductoItemRepository;
import pe.mypeinventario.repository.ProveedorMayoristaRepository;
import pe.mypeinventario.repository.TiendaRepository;
import pe.mypeinventario.service.dto.LineaCompra;
import pe.mypeinventario.service.dto.LineaVenta;
import pe.mypeinventario.service.dto.SolicitudCompra;
import pe.mypeinventario.service.dto.SolicitudVenta;

/**
 * Valida las reglas de los services de ventas y compras: totales, IGV, descuentos,
 * stock, alertas y validaciones de tienda. Cada prueba corre en una transacción
 * que se revierte al terminar.
 */
@SpringBootTest
@Transactional
class VentasComprasServiceTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired private VentaService ventaService;
    @Autowired private CompraService compraService;
    @Autowired private InventarioService inventarioService;
    @Autowired private ConsultaInventarioService consultaService;
    @Autowired private TiendaRepository tiendaRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private ProveedorMayoristaRepository proveedorRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private ProductoItemRepository productoRepository;

    private Long tienda1Id;
    private Long cliente1Id;
    private Long cliente2Id;
    private Long proveedorId;
    private Long producto1Id;
    private Long varianteAId;
    private Long varianteBId;
    private Long varianteTienda2Id;

    /**
     * Tienda 1: variante A (venta 118.00, compra 80.00, mínimo 2) y variante B
     * (venta 59.00, compra 40.00, mínimo 2). Tienda 2: variante C.
     */
    @BeforeEach
    void prepararDatos() {
        Tienda tienda1 = tiendaRepository.save(new Tienda(null, "Tienda Centro", "Av. Lima 123", "Gamarra"));
        Tienda tienda2 = tiendaRepository.save(new Tienda(null, "Tienda Norte", "Av. Norte 456", "Mesa Redonda"));
        tienda1Id = tienda1.getId();

        cliente1Id = clienteRepository.save(new Cliente(null, "Luis Rojas", "977000111", "12345678", tienda1)).getId();
        cliente2Id = clienteRepository.save(new Cliente(null, "Rosa Paz", "966000222", "87654321", tienda2)).getId();
        proveedorId = proveedorRepository.save(new ProveedorMayorista(null, "Mayorista Sur", "sur@correo.com",
                "988777666", "hash", "Distribuidora Sur SAC", "20123456789")).getId();

        Categoria categoria = categoriaRepository.save(new Categoria(null, "Polos", "Polos de algodón", null));

        ProductoItem producto1 = new ProductoItem(null, "Polo básico", "Algodón", tienda1, null);
        categoria.agregarProducto(producto1);
        producto1.agregarVariante(new VarianteSKU(null, "POL-M-AZU", null, "M", "Azul",
                new BigDecimal("80.00"), new BigDecimal("118.00"), 2));
        producto1.agregarVariante(new VarianteSKU(null, "POL-L-ROJ", null, "L", "Rojo",
                new BigDecimal("40.00"), new BigDecimal("59.00"), 2));
        productoRepository.save(producto1);
        producto1Id = producto1.getId();
        varianteAId = producto1.getVariantes().get(0).getId();
        varianteBId = producto1.getVariantes().get(1).getId();

        ProductoItem producto2 = new ProductoItem(null, "Polo básico", "Algodón", tienda2, null);
        categoria.agregarProducto(producto2);
        producto2.agregarVariante(new VarianteSKU(null, "POL-M-AZU", null, "M", "Azul",
                new BigDecimal("80.00"), new BigDecimal("118.00"), 2));
        productoRepository.save(producto2);
        varianteTienda2Id = producto2.getVariantes().get(0).getId();

        em.flush();
        em.clear();
    }

    private void stockInicial(Long varianteId, int cantidad) {
        inventarioService.registrarMovimiento(varianteId, new MovimientoInventario(
                null, TipoMovimiento.COMPRA, cantidad, LocalDateTime.now(), "Stock inicial", null, null));
    }

    private SolicitudVenta venta(Long clienteId, BigDecimal porcentaje, BigDecimal monto, LineaVenta... lineas) {
        return new SolicitudVenta(tienda1Id, clienteId, MedioPago.EFECTIVO, List.of(lineas), porcentaje, monto);
    }

    // ---------------------------------------------------------------- ventas

    @Test
    void laVentaDescuentaElStockYCalculaElTotalConIgvIncluido() {
        stockInicial(varianteAId, 10);

        VentaPunto venta = ventaService.registrarVenta(venta(null, null, null, new LineaVenta(varianteAId, 2)));

        assertThat(venta.getId()).isNotNull();
        assertThat(venta.getCliente()).isNull();
        assertThat(venta.getSubtotal()).isEqualByComparingTo("236.00");
        assertThat(venta.getDescuento()).isEqualByComparingTo("0.00");
        assertThat(venta.getTotal()).isEqualByComparingTo("236.00");
        assertThat(venta.getIgv()).isEqualByComparingTo("36.00");
        assertThat(consultaService.consultarStock(varianteAId)).isEqualTo(8);

        MovimientoInventario salida = consultaService.obtenerHistorial(varianteAId).stream()
                .filter(m -> m.getTipo() == TipoMovimiento.VENTA).findFirst().orElseThrow();
        assertThat(salida.getDetalleVenta()).isNotNull();
    }

    @Test
    void losDescuentosSeAplicanEnOrdenYQuedanGuardados() {
        stockInicial(varianteAId, 10);

        VentaPunto venta = ventaService.registrarVenta(venta(cliente1Id,
                new BigDecimal("10"), new BigDecimal("5.00"), new LineaVenta(varianteAId, 1)));

        // 118.00 - 10 % = 106.20; luego - 5.00 = 101.20
        assertThat(venta.getCliente().getId()).isEqualTo(cliente1Id);
        assertThat(venta.getSubtotal()).isEqualByComparingTo("118.00");
        assertThat(venta.getTotal()).isEqualByComparingTo("101.20");
        assertThat(venta.getDescuento()).isEqualByComparingTo("16.80");
        assertThat(venta.getIgv()).isEqualByComparingTo("15.44");
    }

    @Test
    void unaVentaMayorAlStockLanzaStockInsuficienteYNoDescuentaStock() {
        stockInicial(varianteAId, 10);

        assertThatThrownBy(() -> ventaService.registrarVenta(venta(null, null, null, new LineaVenta(varianteAId, 11))))
                .isInstanceOf(StockInsuficienteException.class);

        // El revertido completo de la venta lo hace la transacción del service; aquí,
        // como la prueba corre dentro de una transacción externa, solo se comprueba el stock.
        assertThat(consultaService.consultarStock(varianteAId)).isEqualTo(10);
    }

    @Test
    void unaVarianteDeOtraTiendaSeRechaza() {
        assertThatThrownBy(() -> ventaService.registrarVenta(
                venta(null, null, null, new LineaVenta(varianteTienda2Id, 1))))
                .isInstanceOf(OperacionInvalidaException.class);
    }

    @Test
    void unClienteDeOtraTiendaSeRechaza() {
        stockInicial(varianteAId, 10);

        assertThatThrownBy(() -> ventaService.registrarVenta(
                venta(cliente2Id, null, null, new LineaVenta(varianteAId, 1))))
                .isInstanceOf(OperacionInvalidaException.class);
    }

    @Test
    void unProductoInactivoNoSePuedeVender() {
        stockInicial(varianteAId, 10);
        productoRepository.findById(producto1Id).orElseThrow().setActivo(false);
        em.flush();

        assertThatThrownBy(() -> ventaService.registrarVenta(venta(null, null, null, new LineaVenta(varianteAId, 1))))
                .isInstanceOf(OperacionInvalidaException.class);
    }

    @Test
    void venderHastaElMinimoGeneraUnaAlerta() {
        stockInicial(varianteAId, 10);

        ventaService.registrarVenta(venta(null, null, null, new LineaVenta(varianteAId, 8)));

        assertThat(consultaService.consultarStock(varianteAId)).isEqualTo(2);
        assertThat(consultaService.listarAlertas(tienda1Id, EstadoAlerta.PENDIENTE)).hasSize(1);
    }

    @Test
    void unaSolicitudDeVentaSinLineasSeRechaza() {
        assertThatThrownBy(() -> new SolicitudVenta(tienda1Id, null, MedioPago.EFECTIVO, List.of(), null, null))
                .isInstanceOf(OperacionInvalidaException.class);
    }

    // ---------------------------------------------------------------- compras

    @Test
    void laCompraAumentaElStockYCalculaElTotal() {
        Compra compra = compraService.registrarCompra(new SolicitudCompra(tienda1Id, proveedorId, List.of(
                new LineaCompra(varianteAId, 10, new BigDecimal("80.00")),
                new LineaCompra(varianteBId, 5, new BigDecimal("40.00")))));

        assertThat(compra.getId()).isNotNull();
        assertThat(compra.getDetalles()).hasSize(2);
        assertThat(compra.getTotal()).isEqualByComparingTo("1000.00");
        assertThat(consultaService.consultarStock(varianteAId)).isEqualTo(10);
        assertThat(consultaService.consultarStock(varianteBId)).isEqualTo(5);

        MovimientoInventario entrada = consultaService.obtenerHistorial(varianteAId).get(0);
        assertThat(entrada.getTipo()).isEqualTo(TipoMovimiento.COMPRA);
        assertThat(entrada.getDetalleCompra()).isNotNull();
    }

    @Test
    void reabastecerConUnaCompraCierraLaAlertaAbierta() {
        stockInicial(varianteAId, 2);
        assertThat(consultaService.listarAlertas(tienda1Id, EstadoAlerta.PENDIENTE)).hasSize(1);

        compraService.registrarCompra(new SolicitudCompra(tienda1Id, proveedorId,
                List.of(new LineaCompra(varianteAId, 10, new BigDecimal("80.00")))));

        assertThat(consultaService.listarAlertas(tienda1Id, EstadoAlerta.PENDIENTE)).isEmpty();
        assertThat(consultaService.listarAlertas(tienda1Id, EstadoAlerta.ATENDIDA)).hasSize(1);
    }

    @Test
    void unProveedorInexistenteSeRechaza() {
        assertThatThrownBy(() -> compraService.registrarCompra(new SolicitudCompra(tienda1Id, 999_999L,
                List.of(new LineaCompra(varianteAId, 1, new BigDecimal("80.00"))))))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void unaCompraDeUnaVarianteDeOtraTiendaSeRechaza() {
        assertThatThrownBy(() -> compraService.registrarCompra(new SolicitudCompra(tienda1Id, proveedorId,
                List.of(new LineaCompra(varianteTienda2Id, 1, new BigDecimal("80.00"))))))
                .isInstanceOf(OperacionInvalidaException.class);
    }

    @Test
    void unaCompraDeUnProductoInactivoSeAcepta() {
        productoRepository.findById(producto1Id).orElseThrow().setActivo(false);
        em.flush();

        compraService.registrarCompra(new SolicitudCompra(tienda1Id, proveedorId,
                List.of(new LineaCompra(varianteAId, 3, new BigDecimal("80.00")))));

        assertThat(consultaService.consultarStock(varianteAId)).isEqualTo(3);
    }

    @Test
    void unaLineaDeCompraConCostoNegativoOSinLineasSeRechaza() {
        assertThatThrownBy(() -> new LineaCompra(varianteAId, 1, new BigDecimal("-1")))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> new SolicitudCompra(tienda1Id, proveedorId, List.of()))
                .isInstanceOf(OperacionInvalidaException.class);
    }
}
