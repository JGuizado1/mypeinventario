package pe.mypeinventario.infraestructura;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pe.mypeinventario.consulta.ProductoResumen;
import pe.mypeinventario.consulta.VarianteResumen;
import pe.mypeinventario.domain.MedioPago;
import pe.mypeinventario.repository.ComercianteRepository;
import pe.mypeinventario.service.CatalogoService;
import pe.mypeinventario.service.ClienteService;
import pe.mypeinventario.service.CompraService;
import pe.mypeinventario.service.CuentaService;
import pe.mypeinventario.service.ProveedorService;
import pe.mypeinventario.service.TiendaService;
import pe.mypeinventario.service.VentaService;
import pe.mypeinventario.service.dto.LineaCompra;
import pe.mypeinventario.service.dto.LineaVenta;
import pe.mypeinventario.service.dto.SolicitudCliente;
import pe.mypeinventario.service.dto.SolicitudCompra;
import pe.mypeinventario.service.dto.SolicitudProducto;
import pe.mypeinventario.service.dto.SolicitudProveedor;
import pe.mypeinventario.service.dto.SolicitudRegistro;
import pe.mypeinventario.service.dto.SolicitudTienda;
import pe.mypeinventario.service.dto.SolicitudVariante;
import pe.mypeinventario.service.dto.SolicitudVenta;

/**
 * Carga datos de ejemplo al arrancar con el perfil {@code demo}: un comerciante con dos
 * tiendas, productos, compras, ventas y alertas. Solo usa los servicios, de modo que los
 * datos pasan por las mismas reglas que tendrían si los ingresara una persona.
 * Sin el perfil {@code demo} no se crea ni se ejecuta (las pruebas no lo ven).
 */
@Component
@Profile("demo")
public class CargadorDatosDemo implements ApplicationRunner {

    public static final String CORREO_DEMO = "demo@mypeinventario.pe";
    public static final String CONTRASENA_DEMO = "demo12345";

    private final ComercianteRepository comercianteRepository;
    private final CuentaService cuentaService;
    private final TiendaService tiendaService;
    private final CatalogoService catalogoService;
    private final ClienteService clienteService;
    private final ProveedorService proveedorService;
    private final CompraService compraService;
    private final VentaService ventaService;

    public CargadorDatosDemo(ComercianteRepository comercianteRepository, CuentaService cuentaService,
                             TiendaService tiendaService, CatalogoService catalogoService,
                             ClienteService clienteService, ProveedorService proveedorService,
                             CompraService compraService, VentaService ventaService) {
        this.comercianteRepository = comercianteRepository;
        this.cuentaService = cuentaService;
        this.tiendaService = tiendaService;
        this.catalogoService = catalogoService;
        this.clienteService = clienteService;
        this.proveedorService = proveedorService;
        this.compraService = compraService;
        this.ventaService = ventaService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (comercianteRepository.existsByCorreo(CORREO_DEMO)) {
            return; // ya cargado: no se duplica
        }
        Long comercianteId = cuentaService.registrarComerciante(
                new SolicitudRegistro("Rosa Quispe", CORREO_DEMO, "987654321", CONTRASENA_DEMO)).id();
        Long modaId = tiendaService.crearTienda(
                new SolicitudTienda(comercianteId, "Moda Rosa", "Jr. Gamarra 450", "Gamarra")).id();
        Long calzadoId = tiendaService.crearTienda(
                new SolicitudTienda(comercianteId, "Calzado Rosa", "Av. Aviación 1200", "Mesa Redonda")).id();

        Long ropa = catalogoService.crearCategoria("Ropa", "Prendas de vestir", null).id();
        Long polos = catalogoService.crearCategoria("Polos", "Polos de algodón", ropa).id();
        Long pantalones = catalogoService.crearCategoria("Pantalones", "Jeans y drill", ropa).id();
        Long calzado = catalogoService.crearCategoria("Calzado", "Todo tipo de calzado", null).id();
        Long zapatillas = catalogoService.crearCategoria("Zapatillas", "Deportivas y urbanas", calzado).id();
        Long marcaGamarra = catalogoService.crearMarca("Gamarra Style").getId();
        Long marcaRunner = catalogoService.crearMarca("Runner").getId();

        Long proveedorId = proveedorService.registrarProveedor(new SolicitudProveedor(
                "Luis Mamani", "ventas@textilesgamarra.example", "912345678",
                "Textiles Gamarra SAC", "20123456789")).id();

        Map<String, Long> variantes = new HashMap<>();
        registrar(variantes, new SolicitudProducto(modaId, "Polo básico", "Polo de algodón pima", polos,
                marcaGamarra, List.of(
                variante("POL-M-AZU", "M", "Azul", "12.00", "29.90", 5),
                variante("POL-L-AZU", "L", "Azul", "12.00", "29.90", 5),
                variante("POL-M-NEG", "M", "Negro", "12.00", "29.90", 5))));
        registrar(variantes, new SolicitudProducto(modaId, "Jean clásico", "Jean corte recto", pantalones,
                marcaGamarra, List.of(
                variante("JEA-30-AZU", "30", "Azul", "45.00", "89.90", 3),
                variante("JEA-32-AZU", "32", "Azul", "45.00", "89.90", 3))));
        registrar(variantes, new SolicitudProducto(modaId, "Casaca de invierno", "Casaca térmica", ropa,
                null, List.of(variante("CAS-M-GRI", "M", "Gris", "70.00", "159.90", 2))));
        registrar(variantes, new SolicitudProducto(calzadoId, "Zapatilla Runner", "Running", zapatillas,
                marcaRunner, List.of(
                variante("RUN-40-NEG", "40", "Negro", "80.00", "149.90", 2),
                variante("RUN-42-NEG", "42", "Negro", "80.00", "149.90", 2))));

        compraService.registrarCompra(new SolicitudCompra(modaId, proveedorId, List.of(
                compra(variantes, "POL-M-AZU", 30, "12.00"), compra(variantes, "POL-L-AZU", 25, "12.00"),
                compra(variantes, "POL-M-NEG", 20, "12.00"), compra(variantes, "JEA-30-AZU", 15, "45.00"),
                compra(variantes, "JEA-32-AZU", 12, "45.00"), compra(variantes, "CAS-M-GRI", 6, "70.00"))));
        compraService.registrarCompra(new SolicitudCompra(calzadoId, proveedorId, List.of(
                compra(variantes, "RUN-40-NEG", 10, "80.00"), compra(variantes, "RUN-42-NEG", 8, "80.00"))));

        Long ana = clienteService.registrarCliente(
                new SolicitudCliente(modaId, "Ana Soto", "987000111", "45678912")).id();
        clienteService.registrarCliente(
                new SolicitudCliente(modaId, "Boutique Lupita", "987000222", "20456789123"));

        ventaService.registrarVenta(new SolicitudVenta(modaId, ana, MedioPago.EFECTIVO, List.of(
                new LineaVenta(variantes.get("POL-M-AZU"), 4), new LineaVenta(variantes.get("JEA-30-AZU"), 2)),
                null, null));
        ventaService.registrarVenta(new SolicitudVenta(modaId, null, MedioPago.YAPE, List.of(
                new LineaVenta(variantes.get("POL-L-AZU"), 3)), BigDecimal.TEN, null));
        // Deja el polo negro por debajo de su mínimo: genera una alerta de reabastecimiento.
        ventaService.registrarVenta(new SolicitudVenta(modaId, null, MedioPago.PLIN, List.of(
                new LineaVenta(variantes.get("POL-M-NEG"), 18)), null, null));
        // Igual con la zapatilla 42, en la otra tienda.
        ventaService.registrarVenta(new SolicitudVenta(calzadoId, null, MedioPago.TARJETA, List.of(
                new LineaVenta(variantes.get("RUN-42-NEG"), 7)), null, null));
    }

    private void registrar(Map<String, Long> variantes, SolicitudProducto solicitud) {
        ProductoResumen producto = catalogoService.registrarProducto(solicitud);
        for (VarianteResumen variante : producto.variantes()) {
            variantes.put(variante.sku(), variante.id());
        }
    }

    private static SolicitudVariante variante(String sku, String talla, String color,
                                              String compra, String venta, int minimo) {
        return new SolicitudVariante(sku, null, talla, color, new BigDecimal(compra), new BigDecimal(venta), minimo);
    }

    private static LineaCompra compra(Map<String, Long> variantes, String sku, int cantidad, String costo) {
        return new LineaCompra(variantes.get(sku), cantidad, new BigDecimal(costo));
    }
}
