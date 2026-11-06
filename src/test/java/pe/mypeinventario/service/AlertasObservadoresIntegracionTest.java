package pe.mypeinventario.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pe.mypeinventario.domain.Categoria;
import pe.mypeinventario.domain.MovimientoInventario;
import pe.mypeinventario.domain.ProductoItem;
import pe.mypeinventario.domain.Tienda;
import pe.mypeinventario.domain.TipoMovimiento;
import pe.mypeinventario.domain.VarianteSKU;
import pe.mypeinventario.observer.BandejaNotificaciones;
import pe.mypeinventario.repository.CategoriaRepository;
import pe.mypeinventario.repository.ProductoItemRepository;
import pe.mypeinventario.repository.TiendaRepository;

/**
 * Valida que los observadores de las alertas se notifiquen solo cuando la transacción
 * se confirma. Estas pruebas confirman datos de verdad, por eso usan una base de datos
 * propia y no contaminan a las demás pruebas.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:observador")
class AlertasObservadoresIntegracionTest {

    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private InventarioService inventarioService;
    @Autowired private BandejaNotificaciones bandeja;
    @Autowired private TiendaRepository tiendaRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private ProductoItemRepository productoRepository;

    private TransactionTemplate transaccion;
    private Long tiendaId;
    private Long varianteId;

    /** Variante con stock mínimo 2 y 10 unidades: todavía sin alerta. */
    @BeforeEach
    void prepararDatos() {
        bandeja.limpiar();
        transaccion = new TransactionTemplate(transactionManager);
        transaccion.executeWithoutResult(estado -> {
            Tienda tienda = tiendaRepository.save(new Tienda(null, "Tienda Centro", "Av. Lima 123", "Gamarra"));
            Categoria categoria = categoriaRepository.save(new Categoria(null, "Polos", "Polos de algodón", null));
            ProductoItem producto = new ProductoItem(null, "Polo básico", "Algodón", tienda, null);
            categoria.agregarProducto(producto);
            producto.agregarVariante(new VarianteSKU(null, "OBS-01", null, "M", "Azul",
                    new BigDecimal("15.00"), new BigDecimal("29.90"), 2));
            productoRepository.save(producto);
            tiendaId = tienda.getId();
            varianteId = producto.getVariantes().get(0).getId();
        });
        inventarioService.registrarMovimiento(varianteId, movimiento(TipoMovimiento.COMPRA, 10));
        bandeja.limpiar();
    }

    private MovimientoInventario movimiento(TipoMovimiento tipo, int cantidad) {
        return new MovimientoInventario(null, tipo, cantidad, LocalDateTime.now(), "Prueba", null, null);
    }

    @Test
    void alConfirmarLaTransaccionSeNotificaAlObservador() {
        inventarioService.registrarMovimiento(varianteId, movimiento(TipoMovimiento.VENTA, 8)); // stock 2

        assertThat(bandeja.mensajesDe(tiendaId)).hasSize(1);
        assertThat(bandeja.mensajesDe(tiendaId).get(0)).contains("OBS-01").contains("quedan 2");
    }

    @Test
    void unaTransaccionRevertidaNoNotificaAlObservador() {
        transaccion.execute(estado -> {
            inventarioService.registrarMovimiento(varianteId, movimiento(TipoMovimiento.VENTA, 8));
            estado.setRollbackOnly();
            return null;
        });

        assertThat(bandeja.mensajesDe(tiendaId)).isEmpty();
    }

    @Test
    void noSeRepiteLaNotificacionMientrasLaAlertaSigueAbierta() {
        inventarioService.registrarMovimiento(varianteId, movimiento(TipoMovimiento.VENTA, 8)); // stock 2: alerta
        inventarioService.registrarMovimiento(varianteId, movimiento(TipoMovimiento.VENTA, 1)); // stock 1: misma alerta

        assertThat(bandeja.mensajesDe(tiendaId)).hasSize(1);
    }

    @Test
    void unStockPorEncimaDelMinimoNoNotifica() {
        inventarioService.registrarMovimiento(varianteId, movimiento(TipoMovimiento.VENTA, 3)); // stock 7

        assertThat(bandeja.mensajesDe(tiendaId)).isEmpty();
    }
}
