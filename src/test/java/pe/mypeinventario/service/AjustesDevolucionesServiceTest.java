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
import pe.mypeinventario.domain.EstadoAlerta;
import pe.mypeinventario.domain.MedioPago;
import pe.mypeinventario.domain.MovimientoInventario;
import pe.mypeinventario.domain.ProductoItem;
import pe.mypeinventario.domain.Tienda;
import pe.mypeinventario.domain.TipoMovimiento;
import pe.mypeinventario.domain.VarianteSKU;
import pe.mypeinventario.domain.VentaPunto;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.exception.StockInsuficienteException;
import pe.mypeinventario.repository.CategoriaRepository;
import pe.mypeinventario.repository.ProductoItemRepository;
import pe.mypeinventario.repository.TiendaRepository;
import pe.mypeinventario.service.dto.LineaVenta;
import pe.mypeinventario.service.dto.SolicitudVenta;

/**
 * Valida los ajustes manuales y las devoluciones. Cada prueba parte de una variante
 * con stock mínimo 2, 10 unidades compradas y una venta de 4 (stock 6).
 */
@SpringBootTest
@Transactional
class AjustesDevolucionesServiceTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired private AjusteInventarioService ajusteService;
    @Autowired private DevolucionService devolucionService;
    @Autowired private VentaService ventaService;
    @Autowired private InventarioService inventarioService;
    @Autowired private ConsultaInventarioService consultaService;
    @Autowired private TiendaRepository tiendaRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private ProductoItemRepository productoRepository;

    private Long tiendaId;
    private Long varianteId;
    private Long ventaId;
    private Long detalleId;

    @BeforeEach
    void prepararDatos() {
        Tienda tienda = tiendaRepository.save(new Tienda(null, "Tienda Centro", "Av. Lima 123", "Gamarra"));
        Categoria categoria = categoriaRepository.save(new Categoria(null, "Polos", "Polos de algodón", null));
        ProductoItem producto = new ProductoItem(null, "Polo básico", "Algodón", tienda, null);
        categoria.agregarProducto(producto);
        producto.agregarVariante(new VarianteSKU(null, "POL-M-AZU", null, "M", "Azul",
                new BigDecimal("80.00"), new BigDecimal("118.00"), 2));
        productoRepository.save(producto);
        tiendaId = tienda.getId();
        varianteId = producto.getVariantes().get(0).getId();
        em.flush();
        em.clear();

        inventarioService.registrarMovimiento(varianteId, new MovimientoInventario(
                null, TipoMovimiento.COMPRA, 10, LocalDateTime.now(), "Stock inicial", null, null));
        VentaPunto venta = venderUnidades(4);
        ventaId = venta.getId();
        detalleId = venta.getDetalles().get(0).getId();
        em.flush();
        em.clear();
    }

    private VentaPunto venderUnidades(int unidades) {
        return ventaService.registrarVenta(new SolicitudVenta(tiendaId, null, MedioPago.EFECTIVO,
                List.of(new LineaVenta(varianteId, unidades)), null, null));
    }

    // ------------------------------------------------------------ devoluciones

    @Test
    void unaDevolucionReponeElStockYQuedaLigadaALaVenta() {
        MovimientoInventario devolucion = devolucionService.registrarDevolucion(
                ventaId, detalleId, 1, "Talla incorrecta");

        assertThat(devolucion.getId()).isNotNull();
        assertThat(devolucion.getTipo()).isEqualTo(TipoMovimiento.DEVOLUCION_CLIENTE);
        assertThat(devolucion.getDetalleVenta().getId()).isEqualTo(detalleId);
        assertThat(devolucion.getMotivo()).isEqualTo("Talla incorrecta");
        assertThat(consultaService.consultarStock(varianteId)).isEqualTo(7);
    }

    @Test
    void laSumaDeLasDevolucionesNoPuedeSuperarLoVendido() {
        devolucionService.registrarDevolucion(ventaId, detalleId, 3, null);
        assertThat(consultaService.consultarStock(varianteId)).isEqualTo(9);

        assertThatThrownBy(() -> devolucionService.registrarDevolucion(ventaId, detalleId, 2, null))
                .isInstanceOf(OperacionInvalidaException.class);

        devolucionService.registrarDevolucion(ventaId, detalleId, 1, null);
        assertThat(consultaService.consultarStock(varianteId)).isEqualTo(10);

        assertThatThrownBy(() -> devolucionService.registrarDevolucion(ventaId, detalleId, 1, null))
                .isInstanceOf(OperacionInvalidaException.class);
    }

    @Test
    void unaVentaOUnaLineaInexistenteSeRechaza() {
        assertThatThrownBy(() -> devolucionService.registrarDevolucion(999_999L, detalleId, 1, null))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> devolucionService.registrarDevolucion(ventaId, 999_999L, 1, null))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void unaCantidadNoPositivaSeRechaza() {
        assertThatThrownBy(() -> devolucionService.registrarDevolucion(ventaId, detalleId, 0, null))
                .isInstanceOf(OperacionInvalidaException.class);
    }

    @Test
    void devolverParteDeLoVendidoPuedeCerrarLaAlertaDeStock() {
        venderUnidades(4); // stock 2: igual al mínimo, se genera una alerta
        assertThat(consultaService.listarAlertas(tiendaId, EstadoAlerta.PENDIENTE)).hasSize(1);

        devolucionService.registrarDevolucion(ventaId, detalleId, 3, null); // stock 5

        assertThat(consultaService.listarAlertas(tiendaId, EstadoAlerta.PENDIENTE)).isEmpty();
        assertThat(consultaService.listarAlertas(tiendaId, EstadoAlerta.ATENDIDA)).hasSize(1);
    }

    // ----------------------------------------------------------------- ajustes

    @Test
    void unAjustePositivoAumentaElStockYNoTieneOrigen() {
        MovimientoInventario ajuste = ajusteService.registrarAjuste(
                varianteId, TipoMovimiento.AJUSTE_POSITIVO, 3, "Conteo físico");

        assertThat(ajuste.getTipo()).isEqualTo(TipoMovimiento.AJUSTE_POSITIVO);
        assertThat(ajuste.getDetalleVenta()).isNull();
        assertThat(ajuste.getDetalleCompra()).isNull();
        assertThat(consultaService.consultarStock(varianteId)).isEqualTo(9);
    }

    @Test
    void unAjusteNegativoDisminuyeElStock() {
        ajusteService.registrarAjuste(varianteId, TipoMovimiento.AJUSTE_NEGATIVO, 2, "Prenda dañada");

        assertThat(consultaService.consultarStock(varianteId)).isEqualTo(4);
    }

    @Test
    void unAjusteNegativoMayorAlStockLanzaStockInsuficiente() {
        assertThatThrownBy(() -> ajusteService.registrarAjuste(
                varianteId, TipoMovimiento.AJUSTE_NEGATIVO, 50, "Pérdida"))
                .isInstanceOf(StockInsuficienteException.class);
        assertThat(consultaService.consultarStock(varianteId)).isEqualTo(6);
    }

    @Test
    void unAjusteSinMotivoOConTipoInvalidoSeRechaza() {
        assertThatThrownBy(() -> ajusteService.registrarAjuste(
                varianteId, TipoMovimiento.AJUSTE_POSITIVO, 1, "   "))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> ajusteService.registrarAjuste(
                varianteId, TipoMovimiento.COMPRA, 1, "Intento de compra"))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> ajusteService.registrarAjuste(varianteId, null, 1, "Sin tipo"))
                .isInstanceOf(OperacionInvalidaException.class);
    }

    @Test
    void unAjusteNegativoHastaElMinimoGeneraUnaAlerta() {
        ajusteService.registrarAjuste(varianteId, TipoMovimiento.AJUSTE_NEGATIVO, 4, "Conteo físico");

        assertThat(consultaService.consultarStock(varianteId)).isEqualTo(2);
        assertThat(consultaService.listarAlertas(tiendaId, EstadoAlerta.PENDIENTE)).hasSize(1);
    }

    @Test
    void unAjusteSobreUnaVarianteInexistenteSeRechaza() {
        assertThatThrownBy(() -> ajusteService.registrarAjuste(
                999_999L, TipoMovimiento.AJUSTE_POSITIVO, 1, "Conteo físico"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
