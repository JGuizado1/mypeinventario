package pe.mypeinventario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.mypeinventario.domain.Categoria;
import pe.mypeinventario.domain.DetalleTomaInventario;
import pe.mypeinventario.domain.EstadoAlerta;
import pe.mypeinventario.domain.EstadoToma;
import pe.mypeinventario.domain.MovimientoInventario;
import pe.mypeinventario.domain.ProductoItem;
import pe.mypeinventario.domain.Tienda;
import pe.mypeinventario.domain.TipoMovimiento;
import pe.mypeinventario.domain.TomaInventario;
import pe.mypeinventario.domain.VarianteSKU;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.exception.StockInsuficienteException;
import pe.mypeinventario.repository.CategoriaRepository;
import pe.mypeinventario.repository.ProductoItemRepository;
import pe.mypeinventario.repository.TiendaRepository;

/**
 * Valida la toma de inventario físico: conteos, cierre con ajustes y reglas.
 * <p>
 * Tienda 1: variante A (stock 10), B (stock 5) y C (sin stock), todas con mínimo 2.
 * Tienda 2: variante D (stock 3).
 */
@SpringBootTest
@Transactional
class InventarioFisicoServiceTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired private InventarioFisicoService inventarioFisicoService;
    @Autowired private InventarioService inventarioService;
    @Autowired private ConsultaInventarioService consultaService;
    @Autowired private TiendaRepository tiendaRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private ProductoItemRepository productoRepository;

    private Long tienda1Id;
    private Long tienda2Id;
    private Long varianteAId;
    private Long varianteBId;
    private Long varianteCId;
    private Long varianteDId;

    @BeforeEach
    void prepararDatos() {
        Tienda tienda1 = tiendaRepository.save(new Tienda(null, "Tienda Centro", "Av. Lima 123", "Gamarra"));
        Tienda tienda2 = tiendaRepository.save(new Tienda(null, "Tienda Norte", "Av. Norte 456", "Mesa Redonda"));
        tienda1Id = tienda1.getId();
        tienda2Id = tienda2.getId();
        Categoria categoria = categoriaRepository.save(new Categoria(null, "Polos", "Polos de algodón", null));

        ProductoItem producto1 = new ProductoItem(null, "Polo básico", "Algodón", tienda1, null);
        categoria.agregarProducto(producto1);
        producto1.agregarVariante(variante("INV-A"));
        producto1.agregarVariante(variante("INV-B"));
        producto1.agregarVariante(variante("INV-C"));
        productoRepository.save(producto1);
        varianteAId = producto1.getVariantes().get(0).getId();
        varianteBId = producto1.getVariantes().get(1).getId();
        varianteCId = producto1.getVariantes().get(2).getId();

        ProductoItem producto2 = new ProductoItem(null, "Polo básico", "Algodón", tienda2, null);
        categoria.agregarProducto(producto2);
        producto2.agregarVariante(variante("INV-D"));
        productoRepository.save(producto2);
        varianteDId = producto2.getVariantes().get(0).getId();

        em.flush();
        em.clear();

        stock(varianteAId, 10);
        stock(varianteBId, 5);
        stock(varianteDId, 3);
    }

    private VarianteSKU variante(String sku) {
        return new VarianteSKU(null, sku, null, "M", "Azul",
                new BigDecimal("15.00"), new BigDecimal("29.90"), 2);
    }

    private void stock(Long varianteId, int cantidad) {
        inventarioService.registrarMovimiento(varianteId, new MovimientoInventario(
                null, TipoMovimiento.COMPRA, cantidad, LocalDateTime.now(), "Stock inicial", null, null));
    }

    @Test
    void iniciarUnaTomaLaDejaEnCurso() {
        TomaInventario toma = inventarioFisicoService.iniciarToma(tienda1Id);

        assertThat(toma.getId()).isNotNull();
        assertThat(toma.getEstado()).isEqualTo(EstadoToma.EN_CURSO);
        assertThat(toma.getDetalles()).isEmpty();
    }

    @Test
    void soloPuedeHaberUnaTomaEnCursoPorTienda() {
        inventarioFisicoService.iniciarToma(tienda1Id);

        assertThatThrownBy(() -> inventarioFisicoService.iniciarToma(tienda1Id))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThat(inventarioFisicoService.iniciarToma(tienda2Id).getEstado()).isEqualTo(EstadoToma.EN_CURSO);
    }

    @Test
    void elConteoGuardaElStockDelSistemaYElContado() {
        TomaInventario toma = inventarioFisicoService.iniciarToma(tienda1Id);

        inventarioFisicoService.registrarConteo(toma.getId(), varianteAId, 8);

        DetalleTomaInventario detalle = inventarioFisicoService.obtenerToma(toma.getId()).getDetalles().get(0);
        assertThat(detalle.getStockSistema()).isEqualTo(10);
        assertThat(detalle.getStockContado()).isEqualTo(8);
        assertThat(detalle.calcularDiferencia()).isEqualTo(-2);
    }

    @Test
    void unaVarianteNoSePuedeContarDosVeces() {
        TomaInventario toma = inventarioFisicoService.iniciarToma(tienda1Id);
        inventarioFisicoService.registrarConteo(toma.getId(), varianteAId, 8);

        assertThatThrownBy(() -> inventarioFisicoService.registrarConteo(toma.getId(), varianteAId, 9))
                .isInstanceOf(OperacionInvalidaException.class);
    }

    @Test
    void losConteosInvalidosSeRechazan() {
        TomaInventario toma = inventarioFisicoService.iniciarToma(tienda1Id);

        assertThatThrownBy(() -> inventarioFisicoService.registrarConteo(toma.getId(), varianteDId, 1))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> inventarioFisicoService.registrarConteo(toma.getId(), varianteAId, -1))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> inventarioFisicoService.registrarConteo(toma.getId(), 999_999L, 1))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void cerrarLaTomaAjustaLasDiferenciasYLaDejaCerrada() {
        TomaInventario toma = inventarioFisicoService.iniciarToma(tienda1Id);
        inventarioFisicoService.registrarConteo(toma.getId(), varianteAId, 8);  // sistema 10: faltan 2
        inventarioFisicoService.registrarConteo(toma.getId(), varianteBId, 7);  // sistema 5: sobran 2
        inventarioFisicoService.registrarConteo(toma.getId(), varianteCId, 0);  // sin diferencia

        TomaInventario cerrada = inventarioFisicoService.cerrarToma(toma.getId());

        assertThat(cerrada.getEstado()).isEqualTo(EstadoToma.CERRADA);
        assertThat(consultaService.consultarStock(varianteAId)).isEqualTo(8);
        assertThat(consultaService.consultarStock(varianteBId)).isEqualTo(7);
        assertThat(consultaService.consultarStock(varianteCId)).isZero();
        assertThat(consultaService.obtenerHistorial(varianteAId)).anyMatch(m ->
                m.getTipo() == TipoMovimiento.AJUSTE_NEGATIVO && m.getMotivo().contains("Toma de inventario #"));
        assertThat(consultaService.obtenerHistorial(varianteBId)).anyMatch(m ->
                m.getTipo() == TipoMovimiento.AJUSTE_POSITIVO);
    }

    @Test
    void sinDiferenciasNoSeGeneranMovimientos() {
        TomaInventario toma = inventarioFisicoService.iniciarToma(tienda1Id);
        inventarioFisicoService.registrarConteo(toma.getId(), varianteAId, 10);

        inventarioFisicoService.cerrarToma(toma.getId());

        assertThat(consultaService.obtenerHistorial(varianteAId)).hasSize(1);
    }

    @Test
    void unAjusteNegativoHastaElMinimoGeneraUnaAlerta() {
        TomaInventario toma = inventarioFisicoService.iniciarToma(tienda1Id);
        inventarioFisicoService.registrarConteo(toma.getId(), varianteAId, 1);

        inventarioFisicoService.cerrarToma(toma.getId());

        assertThat(consultaService.consultarStock(varianteAId)).isEqualTo(1);
        assertThat(consultaService.listarAlertas(tienda1Id, EstadoAlerta.PENDIENTE)).hasSize(1);
    }

    @Test
    void unaTomaCerradaNoAceptaMasConteosNiOtroCierre() {
        TomaInventario toma = inventarioFisicoService.iniciarToma(tienda1Id);
        inventarioFisicoService.registrarConteo(toma.getId(), varianteAId, 9);
        inventarioFisicoService.cerrarToma(toma.getId());

        assertThatThrownBy(() -> inventarioFisicoService.registrarConteo(toma.getId(), varianteBId, 5))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> inventarioFisicoService.cerrarToma(toma.getId()))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThat(consultaService.consultarStock(varianteAId)).isEqualTo(9);
    }

    @Test
    void noSePuedeCerrarUnaTomaSinConteos() {
        TomaInventario toma = inventarioFisicoService.iniciarToma(tienda1Id);

        assertThatThrownBy(() -> inventarioFisicoService.cerrarToma(toma.getId()))
                .isInstanceOf(OperacionInvalidaException.class);
    }

    @Test
    void siElStockBajoDespuesDelConteoElCierreFallaYLaTomaSigueEnCurso() {
        TomaInventario toma = inventarioFisicoService.iniciarToma(tienda1Id);
        inventarioFisicoService.registrarConteo(toma.getId(), varianteAId, 0); // sistema 10: faltan 10
        inventarioService.registrarMovimiento(varianteAId, new MovimientoInventario(
                null, TipoMovimiento.VENTA, 5, LocalDateTime.now(), "Venta durante el conteo", null, null));

        assertThatThrownBy(() -> inventarioFisicoService.cerrarToma(toma.getId()))
                .isInstanceOf(StockInsuficienteException.class);

        assertThat(inventarioFisicoService.obtenerToma(toma.getId()).estaCerrada()).isFalse();
    }

    @Test
    void unaTomaInexistenteSeRechaza() {
        assertThatThrownBy(() -> inventarioFisicoService.obtenerToma(999_999L))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> inventarioFisicoService.registrarConteo(999_999L, varianteAId, 1))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> inventarioFisicoService.iniciarToma(999_999L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
