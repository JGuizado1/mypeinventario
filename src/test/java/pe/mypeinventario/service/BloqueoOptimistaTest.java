package pe.mypeinventario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pe.mypeinventario.domain.Categoria;
import pe.mypeinventario.domain.MovimientoInventario;
import pe.mypeinventario.domain.ProductoItem;
import pe.mypeinventario.domain.Tienda;
import pe.mypeinventario.domain.TipoMovimiento;
import pe.mypeinventario.domain.VarianteSKU;
import pe.mypeinventario.repository.CategoriaRepository;
import pe.mypeinventario.repository.ProductoItemRepository;
import pe.mypeinventario.repository.TiendaRepository;
import pe.mypeinventario.repository.VarianteSKURepository;

/**
 * Valida el bloqueo optimista de {@link VarianteSKU}: cada movimiento sube su versión
 * y una copia desactualizada no puede sobrescribir los cambios de otra operación.
 * <p>
 * Estas pruebas confirman (guardan) sus datos en transacciones reales, por eso usan
 * una base de datos propia y no contaminan a las demás pruebas.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:concurrencia")
class BloqueoOptimistaTest {

    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private InventarioService inventarioService;
    @Autowired private TiendaRepository tiendaRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private ProductoItemRepository productoRepository;
    @Autowired private VarianteSKURepository varianteRepository;

    private TransactionTemplate transaccion;
    private Long varianteId;

    @BeforeEach
    void prepararDatos() {
        transaccion = new TransactionTemplate(transactionManager);
        varianteId = transaccion.execute(estado -> {
            Tienda tienda = tiendaRepository.save(new Tienda(null, "Tienda Centro", "Av. Lima 123", "Gamarra"));
            Categoria categoria = categoriaRepository.save(new Categoria(null, "Polos", "Polos de algodón", null));
            ProductoItem producto = new ProductoItem(null, "Polo básico", "Algodón", tienda, null);
            categoria.agregarProducto(producto);
            producto.agregarVariante(new VarianteSKU(null, "POL-M-AZU", null, "M", "Azul",
                    new BigDecimal("80.00"), new BigDecimal("118.00"), 2));
            productoRepository.save(producto);
            return producto.getVariantes().get(0).getId();
        });
        inventarioService.registrarMovimiento(varianteId, movimiento(TipoMovimiento.COMPRA, 10));
    }

    private MovimientoInventario movimiento(TipoMovimiento tipo, int cantidad) {
        return new MovimientoInventario(null, tipo, cantidad, LocalDateTime.now(), "Prueba", null, null);
    }

    private Long versionActual() {
        return transaccion.execute(estado -> varianteRepository.findById(varianteId).orElseThrow().getVersion());
    }

    @Test
    void cadaMovimientoIncrementaLaVersionDeLaVariante() {
        Long antes = versionActual();

        inventarioService.registrarMovimiento(varianteId, movimiento(TipoMovimiento.VENTA, 1));

        assertThat(versionActual()).isGreaterThan(antes);
    }

    @Test
    void unaCopiaDesactualizadaNoPuedeSobrescribirLosCambiosDeOtraOperacion() {
        // Una operación lee la variante y se queda con una copia...
        VarianteSKU copiaVieja = transaccion.execute(
                estado -> varianteRepository.findById(varianteId).orElseThrow());

        // ...mientras otra operación registra un movimiento y sube la versión.
        inventarioService.registrarMovimiento(varianteId, movimiento(TipoMovimiento.VENTA, 1));

        // Guardar la copia vieja debe fallar en lugar de pisar el cambio.
        copiaVieja.setColor("Verde");
        assertThatThrownBy(() -> transaccion.executeWithoutResult(
                estado -> varianteRepository.saveAndFlush(copiaVieja)))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }
}