package pe.mypeinventario.fachada;

import java.util.List;
import org.springframework.stereotype.Service;
import pe.mypeinventario.consulta.ProductoResumen;
import pe.mypeinventario.consulta.ResumenInicio;
import pe.mypeinventario.consulta.TiendaResumen;
import pe.mypeinventario.consulta.UsuarioSesion;
import pe.mypeinventario.domain.EstadoAlerta;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.service.ConsultaCatalogoService;
import pe.mypeinventario.service.ConsultaInventarioService;
import pe.mypeinventario.service.CuentaService;
import pe.mypeinventario.service.TiendaService;
import pe.mypeinventario.service.dto.SolicitudRegistro;
import pe.mypeinventario.service.dto.SolicitudTienda;

/**
 * Patrón Facade. Ofrece a la web operaciones simples ("entrar", "ver el inicio de mi
 * tienda") y esconde los servicios que hay detrás. Hace dos cosas que ningún controlador
 * debe repetir: comprueba que cada tienda sea del comerciante que la pide, y entrega solo
 * resúmenes (records), nunca entidades, porque la web no tiene transacción abierta.
 * Va creciendo pantalla por pantalla.
 */
@Service
public class InventarioFacade {

    private final CuentaService cuentaService;
    private final TiendaService tiendaService;
    private final ConsultaCatalogoService catalogo;
    private final ConsultaInventarioService inventario;

    public InventarioFacade(CuentaService cuentaService, TiendaService tiendaService,
                            ConsultaCatalogoService catalogo, ConsultaInventarioService inventario) {
        this.cuentaService = cuentaService;
        this.tiendaService = tiendaService;
        this.catalogo = catalogo;
        this.inventario = inventario;
    }

    /** @see CuentaService#registrarComerciante(SolicitudRegistro) */
    public UsuarioSesion registrarse(SolicitudRegistro solicitud) {
        return cuentaService.registrarComerciante(solicitud);
    }

    /** @see CuentaService#autenticar(String, String) */
    public UsuarioSesion ingresar(String correo, String contrasena) {
        return cuentaService.autenticar(correo, contrasena);
    }

    /** @return las tiendas del comerciante, ordenadas por nombre */
    public List<TiendaResumen> tiendasDe(Long comercianteId) {
        return tiendaService.listarTiendas(comercianteId);
    }

    /** @see TiendaService#crearTienda(SolicitudTienda) */
    public TiendaResumen crearTienda(SolicitudTienda solicitud) {
        return tiendaService.crearTienda(solicitud);
    }

    /**
     * Devuelve una tienda, siempre que sea del comerciante.
     *
     * @throws RecursoNoEncontradoException si no existe o es de otro comerciante
     */
    public TiendaResumen tienda(Long comercianteId, Long tiendaId) {
        tiendaService.verificarPropiedad(comercianteId, tiendaId);
        return tiendaService.listarTiendas(comercianteId).stream()
                .filter(tienda -> tienda.id().equals(tiendaId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Tienda", tiendaId));
    }

    /**
     * Arma las cifras del inicio de una tienda.
     *
     * @throws RecursoNoEncontradoException si la tienda no existe o es de otro comerciante
     */
    public ResumenInicio resumenInicio(Long comercianteId, Long tiendaId) {
        TiendaResumen tienda = tienda(comercianteId, tiendaId);
        List<ProductoResumen> productos = catalogo.listarProductos(tiendaId, true);
        int stockBajo = (int) productos.stream()
                .flatMap(producto -> producto.variantes().stream())
                .filter(variante -> variante.stockBajo())
                .count();
        int alertas = inventario.listarAlertas(tiendaId, EstadoAlerta.PENDIENTE).size();
        return new ResumenInicio(tienda, productos.size(), stockBajo, alertas);
    }
}
