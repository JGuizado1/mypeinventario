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
import pe.mypeinventario.domain.AlertaReabastecimiento;
import pe.mypeinventario.domain.Categoria;
import pe.mypeinventario.domain.EstadoAlerta;
import pe.mypeinventario.domain.MovimientoInventario;
import pe.mypeinventario.domain.ProductoItem;
import pe.mypeinventario.domain.Tienda;
import pe.mypeinventario.domain.TipoMovimiento;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.exception.StockInsuficienteException;
import pe.mypeinventario.domain.VarianteSKU;
import pe.mypeinventario.repository.CategoriaRepository;
import pe.mypeinventario.repository.ProductoItemRepository;
import pe.mypeinventario.repository.TiendaRepository;

/**
 * Valida las reglas de los servicios de inventario: stock, alertas y errores.
 * Cada prueba corre en una transacción que se revierte al terminar.
 */
@SpringBootTest
@Transactional
class InventarioServiceTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired private InventarioService inventarioService;
    @Autowired private ConsultaInventarioService consultaService;
    @Autowired private TiendaRepository tiendaRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private ProductoItemRepository productoRepository;

    private Long tiendaId;
    private Long varianteId;

    /** Una variante con stock mínimo propio de 3 unidades y sin movimientos. */
    @BeforeEach
    void prepararDatos() {
        Tienda tienda = tiendaRepository.save(new Tienda(null, "Tienda Centro", "Av. Lima 123", "Gamarra"));
        Categoria categoria = categoriaRepository.save(new Categoria(null, "Polos", "Polos de algodón", null));
        ProductoItem producto = new ProductoItem(null, "Polo básico", "Algodón", tienda, null);
        categoria.agregarProducto(producto);
        producto.agregarVariante(new VarianteSKU(null, "POL-M-AZU", null, "M", "Azul",
                new BigDecimal("15.00"), new BigDecimal("29.90"), 3));
        productoRepository.save(producto);
        tiendaId = tienda.getId();
        varianteId = producto.getVariantes().get(0).getId();
        em.flush();
        em.clear();
    }

    private MovimientoInventario movimiento(TipoMovimiento tipo, int cantidad) {
        return new MovimientoInventario(null, tipo, cantidad, LocalDateTime.now(), "Prueba", null, null);
    }

    @Test
    void unaEntradaAumentaElStockYQuedaEnElHistorial() {
        MovimientoInventario guardado = inventarioService.registrarMovimiento(
                varianteId, movimiento(TipoMovimiento.COMPRA, 10));

        assertThat(guardado.getId()).isNotNull();
        assertThat(consultaService.consultarStock(varianteId)).isEqualTo(10);
        assertThat(consultaService.obtenerHistorial(varianteId)).hasSize(1);
        assertThat(consultaService.listarAlertas(tiendaId, EstadoAlerta.PENDIENTE)).isEmpty();
    }

    @Test
    void unaSalidaMayorAlStockLanzaStockInsuficiente() {
        inventarioService.registrarMovimiento(varianteId, movimiento(TipoMovimiento.COMPRA, 2));

        assertThatThrownBy(() -> inventarioService.registrarMovimiento(
                varianteId, movimiento(TipoMovimiento.VENTA, 5)))
                .isInstanceOf(StockInsuficienteException.class);
        assertThat(consultaService.consultarStock(varianteId)).isEqualTo(2);
    }

    @Test
    void bajarAlMinimoGeneraUnaAlertaPendiente() {
        inventarioService.registrarMovimiento(varianteId, movimiento(TipoMovimiento.COMPRA, 10));
        inventarioService.registrarMovimiento(varianteId, movimiento(TipoMovimiento.VENTA, 7));

        assertThat(consultaService.consultarStock(varianteId)).isEqualTo(3);
        assertThat(consultaService.listarAlertas(tiendaId, EstadoAlerta.PENDIENTE)).hasSize(1);
    }

    @Test
    void noSeDuplicanLasAlertasAbiertas() {
        inventarioService.registrarMovimiento(varianteId, movimiento(TipoMovimiento.COMPRA, 10));
        inventarioService.registrarMovimiento(varianteId, movimiento(TipoMovimiento.VENTA, 7));
        inventarioService.registrarMovimiento(varianteId, movimiento(TipoMovimiento.VENTA, 1));

        assertThat(consultaService.consultarStock(varianteId)).isEqualTo(2);
        assertThat(consultaService.listarAlertas(tiendaId, EstadoAlerta.PENDIENTE)).hasSize(1);
    }

    @Test
    void reabastecerPorEncimaDelMinimoCierraLasAlertas() {
        inventarioService.registrarMovimiento(varianteId, movimiento(TipoMovimiento.COMPRA, 10));
        inventarioService.registrarMovimiento(varianteId, movimiento(TipoMovimiento.VENTA, 7));
        inventarioService.registrarMovimiento(varianteId, movimiento(TipoMovimiento.COMPRA, 10));

        assertThat(consultaService.listarAlertas(tiendaId, EstadoAlerta.PENDIENTE)).isEmpty();
        assertThat(consultaService.listarAlertas(tiendaId, EstadoAlerta.ATENDIDA)).hasSize(1);
    }

    @Test
    void unaAlertaPendienteSePuedeMarcarVistaUnaSolaVez() {
        inventarioService.registrarMovimiento(varianteId, movimiento(TipoMovimiento.COMPRA, 3));
        AlertaReabastecimiento alerta = consultaService.listarAlertas(tiendaId, EstadoAlerta.PENDIENTE).get(0);

        AlertaReabastecimiento vista = inventarioService.marcarAlertaVista(alerta.getId());

        assertThat(vista.getEstado()).isEqualTo(EstadoAlerta.VISTA);
        assertThatThrownBy(() -> inventarioService.marcarAlertaVista(alerta.getId()))
                .isInstanceOf(OperacionInvalidaException.class);
    }

    @Test
    void unaVarianteInexistenteLanzaRecursoNoEncontrado() {
        assertThatThrownBy(() -> inventarioService.registrarMovimiento(
                999_999L, movimiento(TipoMovimiento.COMPRA, 1)))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> consultaService.consultarStock(999_999L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
