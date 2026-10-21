package pe.mypeinventario.service.impl;

import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.mypeinventario.domain.Compra;
import pe.mypeinventario.domain.DetalleCompra;
import pe.mypeinventario.domain.MovimientoInventario;
import pe.mypeinventario.domain.ProveedorMayorista;
import pe.mypeinventario.domain.Tienda;
import pe.mypeinventario.domain.VarianteSKU;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.factory.CreadorMovimiento;
import pe.mypeinventario.factory.OrigenMovimiento;
import pe.mypeinventario.repository.CompraRepository;
import pe.mypeinventario.repository.ProveedorMayoristaRepository;
import pe.mypeinventario.repository.TiendaRepository;
import pe.mypeinventario.service.CompraService;
import pe.mypeinventario.service.InventarioService;
import pe.mypeinventario.service.dto.LineaCompra;
import pe.mypeinventario.service.dto.SolicitudCompra;

/** Implementación de {@link CompraService}. */
@Service
public class CompraServiceImpl implements CompraService {

    private final TiendaRepository tiendaRepository;
    private final ProveedorMayoristaRepository proveedorRepository;
    private final CompraRepository compraRepository;
    private final VariantesDeTienda variantesDeTienda;
    private final InventarioService inventarioService;
    private final CreadorMovimiento creadorCompra;
    private final Clock reloj;

    public CompraServiceImpl(TiendaRepository tiendaRepository,
                             ProveedorMayoristaRepository proveedorRepository,
                             CompraRepository compraRepository,
                             VariantesDeTienda variantesDeTienda,
                             InventarioService inventarioService,
                             @Qualifier("creadorCompra") CreadorMovimiento creadorCompra,
                             Clock reloj) {
        this.tiendaRepository = tiendaRepository;
        this.proveedorRepository = proveedorRepository;
        this.compraRepository = compraRepository;
        this.variantesDeTienda = variantesDeTienda;
        this.inventarioService = inventarioService;
        this.creadorCompra = creadorCompra;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public Compra registrarCompra(SolicitudCompra solicitud) {
        Tienda tienda = tiendaRepository.findById(solicitud.tiendaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Tienda", solicitud.tiendaId()));
        ProveedorMayorista proveedor = proveedorRepository.findById(solicitud.proveedorId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Proveedor", solicitud.proveedorId()));

        Compra compra = new Compra(null, LocalDateTime.now(reloj), tienda, proveedor);
        for (LineaCompra linea : solicitud.lineas()) {
            // Se puede comprar un producto inactivo: es normal reponer antes de reactivarlo.
            VarianteSKU variante = variantesDeTienda.obtener(linea.varianteId(), tienda.getId(), false);
            compra.agregarDetalle(new DetalleCompra(null, variante, linea.cantidad(), linea.costoUnitario()));
        }

        compraRepository.save(compra);
        for (DetalleCompra detalle : compra.getDetalles()) {
            MovimientoInventario movimiento = creadorCompra.crear(
                    OrigenMovimiento.deCompra(detalle), detalle.getCantidad(), null);
            inventarioService.registrarMovimiento(detalle.getVariante().getId(), movimiento);
        }
        return compra;
    }
}
