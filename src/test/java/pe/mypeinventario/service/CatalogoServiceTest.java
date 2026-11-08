package pe.mypeinventario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.mypeinventario.config.ConfiguracionTienda;
import pe.mypeinventario.consulta.CategoriaResumen;
import pe.mypeinventario.consulta.ProductoResumen;
import pe.mypeinventario.consulta.VarianteResumen;
import pe.mypeinventario.domain.MovimientoInventario;
import pe.mypeinventario.domain.Tienda;
import pe.mypeinventario.domain.TipoMovimiento;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.repository.TiendaRepository;
import pe.mypeinventario.service.dto.SolicitudProducto;
import pe.mypeinventario.service.dto.SolicitudVariante;

/**
 * Valida el alta y la consulta del catálogo: categorías, marcas, productos con sus
 * variantes, precios y estado. Cada prueba corre en una transacción que se revierte.
 */
@SpringBootTest
@Transactional
class CatalogoServiceTest {

    @Autowired private CatalogoService catalogoService;
    @Autowired private ConsultaCatalogoService consultaService;
    @Autowired private InventarioService inventarioService;
    @Autowired private TiendaRepository tiendaRepository;

    private Long tienda1Id;
    private Long tienda2Id;
    private Long categoriaId;
    private Long marcaId;

    @BeforeEach
    void prepararDatos() {
        tienda1Id = tiendaRepository.save(new Tienda(null, "Tienda Centro", "Av. Lima 123", "Gamarra")).getId();
        tienda2Id = tiendaRepository.save(new Tienda(null, "Tienda Norte", "Av. Norte 456", "Mesa Redonda")).getId();
        categoriaId = catalogoService.crearCategoria("Calzado", "Todo calzado", null).id();
        marcaId = catalogoService.crearMarca("Runner").getId();
    }

    private SolicitudVariante variante(String sku, String barras, String precioVenta, Integer stockMinimo) {
        return new SolicitudVariante(sku, barras, "42", "Negro",
                new BigDecimal("80.00"), new BigDecimal(precioVenta), stockMinimo);
    }

    private SolicitudProducto solicitud(Long tiendaId, SolicitudVariante... variantes) {
        return new SolicitudProducto(tiendaId, "Zapatilla Runner", "Running", categoriaId, marcaId, List.of(variantes));
    }

    // ---------------------------------------------------- categorías y marcas

    @Test
    void unaCategoriaHijaGuardaSuPadre() {
        CategoriaResumen hija = catalogoService.crearCategoria("Zapatillas", "Deportivas", categoriaId);

        assertThat(hija.padreId()).isEqualTo(categoriaId);
        assertThat(hija.padre()).isEqualTo("Calzado");
        assertThat(consultaService.listarCategorias()).extracting(CategoriaResumen::nombre)
                .containsExactly("Calzado", "Zapatillas");
    }

    @Test
    void lasCategoriasInvalidasSeRechazan() {
        assertThatThrownBy(() -> catalogoService.crearCategoria("Sandalias", null, 999_999L))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> catalogoService.crearCategoria("   ", null, null))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> catalogoService.crearCategoria("calzado", null, null))
                .isInstanceOf(OperacionInvalidaException.class);
    }

    @Test
    void unaMarcaNoSePuedeRepetirSinImportarMayusculas() {
        assertThatThrownBy(() -> catalogoService.crearMarca("RUNNER")).isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> catalogoService.crearMarca(" ")).isInstanceOf(OperacionInvalidaException.class);
        assertThat(consultaService.listarMarcas()).extracting(m -> m.getNombre()).containsExactly("Runner");
    }

    // ------------------------------------------------------------- productos

    @Test
    void unProductoNuevoQuedaConSusVariantesYSuStockMinimo() {
        ProductoResumen producto = catalogoService.registrarProducto(solicitud(tienda1Id,
                variante("ZAP-42", null, "129.90", null), variante("ZAP-40", null, "129.90", 2)));

        assertThat(producto.id()).isNotNull();
        assertThat(producto.categoria()).isEqualTo("Calzado");
        assertThat(producto.marca()).isEqualTo("Runner");
        assertThat(producto.activo()).isTrue();
        assertThat(producto.variantes()).hasSize(2);

        VarianteResumen conMinimoPorDefecto = producto.variantes().get(0);
        assertThat(conMinimoPorDefecto.stockMinimo())
                .isEqualTo(ConfiguracionTienda.getInstance().getStockMinimoPorDefecto());
        assertThat(producto.variantes().get(1).stockMinimo()).isEqualTo(2);
        assertThat(conMinimoPorDefecto.stock()).isZero();
        assertThat(conMinimoPorDefecto.stockBajo()).isTrue();
    }

    @Test
    void elStockDeLaVarianteSeReflejaEnLaConsulta() {
        ProductoResumen producto = catalogoService.registrarProducto(
                solicitud(tienda1Id, variante("ZAP-42", null, "129.90", 2)));
        Long varianteId = producto.variantes().get(0).id();

        inventarioService.registrarMovimiento(varianteId, new MovimientoInventario(
                null, TipoMovimiento.COMPRA, 10, LocalDateTime.now(), "Stock inicial", null, null));

        VarianteResumen variante = consultaService.listarProductos(tienda1Id, false).get(0).variantes().get(0);
        assertThat(variante.stock()).isEqualTo(10);
        assertThat(variante.stockBajo()).isFalse();
    }

    @Test
    void unSkuSeRepiteEntreTiendasPeroNoDentroDeLaMisma() {
        catalogoService.registrarProducto(solicitud(tienda1Id, variante("ZAP-42", null, "129.90", null)));

        assertThatThrownBy(() -> catalogoService.registrarProducto(
                solicitud(tienda1Id, variante("ZAP-42", null, "129.90", null))))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThat(catalogoService.registrarProducto(
                solicitud(tienda2Id, variante("ZAP-42", null, "129.90", null))).id()).isNotNull();
    }

    @Test
    void unSkuOUnCodigoDeBarrasRepetidoSeRechaza() {
        catalogoService.registrarProducto(solicitud(tienda1Id, variante("ZAP-42", "7751234567890", "129.90", null)));

        assertThatThrownBy(() -> catalogoService.registrarProducto(solicitud(tienda1Id,
                variante("ZAP-43", "7751234567890", "129.90", null))))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> catalogoService.registrarProducto(solicitud(tienda1Id,
                variante("ZAP-50", null, "129.90", null), variante("zap-50", null, "129.90", null))))
                .isInstanceOf(OperacionInvalidaException.class);
    }

    @Test
    void lasSolicitudesInvalidasSeRechazan() {
        assertThatThrownBy(() -> new SolicitudProducto(tienda1Id, "Zapatilla", null, categoriaId, null, List.of()))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> variante("ZAP-42", null, "0", null)).isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> variante("  ", null, "129.90", null)).isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> variante("ZAP-42", null, "129.90", -1)).isInstanceOf(OperacionInvalidaException.class);
    }

    @Test
    void lasReferenciasInexistentesSeRechazan() {
        SolicitudVariante variante = variante("ZAP-42", null, "129.90", null);

        assertThatThrownBy(() -> catalogoService.registrarProducto(solicitud(999_999L, variante)))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> catalogoService.registrarProducto(
                new SolicitudProducto(tienda1Id, "Zapatilla", null, 999_999L, null, List.of(variante))))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> catalogoService.registrarProducto(
                new SolicitudProducto(tienda1Id, "Zapatilla", null, categoriaId, 999_999L, List.of(variante))))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void sePuedeAgregarUnaVarianteAUnProductoExistente() {
        ProductoResumen producto = catalogoService.registrarProducto(
                solicitud(tienda1Id, variante("ZAP-42", null, "129.90", null)));

        ProductoResumen ampliado = catalogoService.agregarVariante(
                producto.id(), variante("ZAP-44", null, "129.90", null));

        assertThat(ampliado.variantes()).extracting(VarianteResumen::sku).containsExactly("ZAP-42", "ZAP-44");
        assertThatThrownBy(() -> catalogoService.agregarVariante(producto.id(), variante("ZAP-44", null, "99.90", null)))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> catalogoService.agregarVariante(999_999L, variante("ZAP-45", null, "99.90", null)))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThat(consultaService.obtenerProducto(producto.id()).variantes()).hasSize(2);
    }

    @Test
    void sePuedenActualizarLosPrecios() {
        ProductoResumen producto = catalogoService.registrarProducto(
                solicitud(tienda1Id, variante("ZAP-42", null, "129.90", null)));
        Long varianteId = producto.variantes().get(0).id();

        catalogoService.actualizarPrecios(varianteId, new BigDecimal("90.00"), new BigDecimal("149.90"));

        VarianteResumen variante = consultaService.obtenerProducto(producto.id()).variantes().get(0);
        assertThat(variante.precioCompra()).isEqualByComparingTo("90.00");
        assertThat(variante.precioVenta()).isEqualByComparingTo("149.90");
        assertThatThrownBy(() -> catalogoService.actualizarPrecios(varianteId, new BigDecimal("90.00"), BigDecimal.ZERO))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> catalogoService.actualizarPrecios(999_999L, null, new BigDecimal("10.00")))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void losProductosInactivosSoloAparecenSiSePidenTodos() {
        ProductoResumen producto = catalogoService.registrarProducto(
                solicitud(tienda1Id, variante("ZAP-42", null, "129.90", null)));

        catalogoService.cambiarEstadoProducto(producto.id(), false);

        assertThat(consultaService.listarProductos(tienda1Id, true)).isEmpty();
        assertThat(consultaService.listarProductos(tienda1Id, false)).hasSize(1);

        catalogoService.cambiarEstadoProducto(producto.id(), true);
        assertThat(consultaService.listarProductos(tienda1Id, true)).hasSize(1);
    }

    @Test
    void cadaTiendaSoloVeSusProductos() {
        catalogoService.registrarProducto(solicitud(tienda1Id, variante("ZAP-42", null, "129.90", null)));

        assertThat(consultaService.listarProductos(tienda1Id, false)).hasSize(1);
        assertThat(consultaService.listarProductos(tienda2Id, false)).isEmpty();
        assertThatThrownBy(() -> consultaService.listarProductos(999_999L, false))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
