package pe.mypeinventario.service.impl;

import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.mypeinventario.config.ParametrosTributarios;
import pe.mypeinventario.decorator.CalculoImporte;
import pe.mypeinventario.decorator.DesgloseIgv;
import pe.mypeinventario.decorator.DescuentoMontoFijo;
import pe.mypeinventario.decorator.DescuentoPorcentual;
import pe.mypeinventario.decorator.ImporteBase;
import pe.mypeinventario.domain.Cliente;
import pe.mypeinventario.domain.DetalleVenta;
import pe.mypeinventario.domain.MovimientoInventario;
import pe.mypeinventario.domain.Tienda;
import pe.mypeinventario.domain.VarianteSKU;
import pe.mypeinventario.domain.VentaPunto;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.factory.CreadorMovimiento;
import pe.mypeinventario.factory.OrigenMovimiento;
import pe.mypeinventario.repository.ClienteRepository;
import pe.mypeinventario.repository.TiendaRepository;
import pe.mypeinventario.repository.VentaPuntoRepository;
import pe.mypeinventario.service.InventarioService;
import pe.mypeinventario.service.VentaService;
import pe.mypeinventario.service.dto.LineaVenta;
import pe.mypeinventario.service.dto.SolicitudVenta;

/** Implementación de {@link VentaService}. */
@Service
public class VentaServiceImpl implements VentaService {

    private final TiendaRepository tiendaRepository;
    private final ClienteRepository clienteRepository;
    private final VentaPuntoRepository ventaRepository;
    private final VariantesDeTienda variantesDeTienda;
    private final InventarioService inventarioService;
    private final CreadorMovimiento creadorVenta;
    private final ParametrosTributarios parametrosTributarios;
    private final Clock reloj;

    public VentaServiceImpl(TiendaRepository tiendaRepository,
                            ClienteRepository clienteRepository,
                            VentaPuntoRepository ventaRepository,
                            VariantesDeTienda variantesDeTienda,
                            InventarioService inventarioService,
                            @Qualifier("creadorVenta") CreadorMovimiento creadorVenta,
                            ParametrosTributarios parametrosTributarios,
                            Clock reloj) {
        this.tiendaRepository = tiendaRepository;
        this.clienteRepository = clienteRepository;
        this.ventaRepository = ventaRepository;
        this.variantesDeTienda = variantesDeTienda;
        this.inventarioService = inventarioService;
        this.creadorVenta = creadorVenta;
        this.parametrosTributarios = parametrosTributarios;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public VentaPunto registrarVenta(SolicitudVenta solicitud) {
        Tienda tienda = tiendaRepository.findById(solicitud.tiendaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Tienda", solicitud.tiendaId()));
        Cliente cliente = obtenerCliente(solicitud.clienteId(), tienda);

        VentaPunto venta = new VentaPunto(null, LocalDateTime.now(reloj), solicitud.medioPago(), tienda, cliente);
        for (LineaVenta linea : solicitud.lineas()) {
            VarianteSKU variante = variantesDeTienda.obtener(linea.varianteId(), tienda.getId(), true);
            // El precio se congela en la línea: un cambio posterior no altera esta venta.
            venta.agregarDetalle(new DetalleVenta(null, variante, linea.cantidad(), variante.getPrecioVenta()));
        }
        aplicarImporteFinal(venta, solicitud);

        // Se guarda primero la venta (y sus líneas, por cascada) para que cada movimiento
        // pueda referirse a una línea ya persistida.
        ventaRepository.save(venta);
        for (DetalleVenta detalle : venta.getDetalles()) {
            MovimientoInventario movimiento = creadorVenta.crear(
                    OrigenMovimiento.deVenta(detalle), detalle.getCantidad(), null);
            inventarioService.registrarMovimiento(detalle.getVariante().getId(), movimiento);
        }
        return venta;
    }

    private Cliente obtenerCliente(Long clienteId, Tienda tienda) {
        if (clienteId == null) {
            return null;
        }
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", clienteId));
        if (!cliente.getTienda().getId().equals(tienda.getId())) {
            throw new OperacionInvalidaException("El cliente no pertenece a la tienda de la venta");
        }
        return cliente;
    }

    /** Arma la cadena de decoradores (porcentual y luego monto fijo) y desglosa el IGV. */
    private void aplicarImporteFinal(VentaPunto venta, SolicitudVenta solicitud) {
        CalculoImporte importe = new ImporteBase(venta.getSubtotal());
        if (solicitud.descuentoPorcentaje() != null) {
            importe = new DescuentoPorcentual(importe, solicitud.descuentoPorcentaje());
        }
        if (solicitud.descuentoMonto() != null) {
            importe = new DescuentoMontoFijo(importe, solicitud.descuentoMonto());
        }
        DesgloseIgv desglose = DesgloseIgv.desde(importe.importe(), parametrosTributarios.getIgv());
        venta.aplicarImporteFinal(desglose.total(), desglose.igv());
    }
}