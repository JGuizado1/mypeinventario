package pe.mypeinventario.fachada;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import pe.mypeinventario.consulta.ProductoResumen;
import pe.mypeinventario.consulta.TomaResumen;
import pe.mypeinventario.domain.TomaInventario;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.service.ConsultaCatalogoService;
import pe.mypeinventario.service.ConsultaTomaService;
import pe.mypeinventario.service.InventarioFisicoService;
import pe.mypeinventario.service.TiendaService;

/**
 * Patrón Facade de la toma de inventario físico: iniciarla, anotar lo contado y cerrarla.
 * Comprueba que la tienda (o la toma) sea del comerciante y devuelve solo resúmenes. Una
 * toma ajena se responde como "no existe".
 */
@Service
public class TomaFacade {

    private final InventarioFisicoService fisico;
    private final ConsultaTomaService consulta;
    private final ConsultaCatalogoService catalogo;
    private final TiendaService tiendaService;

    public TomaFacade(InventarioFisicoService fisico, ConsultaTomaService consulta,
                      ConsultaCatalogoService catalogo, TiendaService tiendaService) {
        this.fisico = fisico;
        this.consulta = consulta;
        this.catalogo = catalogo;
        this.tiendaService = tiendaService;
    }

    /** @return las tomas de una tienda del comerciante, de la más reciente a la más antigua */
    public List<TomaResumen> tomas(Long comercianteId, Long tiendaId) {
        tiendaService.verificarPropiedad(comercianteId, tiendaId);
        return consulta.listarTomas(tiendaId);
    }

    /**
     * Devuelve una toma, siempre que sea de una tienda del comerciante.
     *
     * @throws RecursoNoEncontradoException si no existe o es de otro comerciante
     */
    public TomaResumen toma(Long comercianteId, Long tomaId) {
        TomaResumen toma = consulta.obtenerToma(tomaId);
        try {
            tiendaService.verificarPropiedad(comercianteId, toma.tiendaId());
        } catch (RecursoNoEncontradoException e) {
            throw new RecursoNoEncontradoException("Toma de inventario", tomaId);
        }
        return toma;
    }

    /** @return todos los productos de la tienda, también los inactivos: el stock físico existe igual */
    public List<ProductoResumen> productosParaContar(Long comercianteId, Long tiendaId) {
        tiendaService.verificarPropiedad(comercianteId, tiendaId);
        return catalogo.listarProductos(tiendaId, false);
    }

    /** @see InventarioFisicoService#iniciarToma(Long) */
    public TomaResumen iniciar(Long comercianteId, Long tiendaId) {
        tiendaService.verificarPropiedad(comercianteId, tiendaId);
        TomaInventario nueva = fisico.iniciarToma(tiendaId);
        return consulta.obtenerToma(nueva.getId());
    }

    /**
     * Anota los conteos de varias variantes en una toma del comerciante.
     *
     * @param conteos unidades contadas por identificador de variante
     * @return cuántos conteos se anotaron
     * @see InventarioFisicoService#registrarConteo(Long, Long, int)
     */
    public int registrarConteos(Long comercianteId, Long tomaId, Map<Long, Integer> conteos) {
        toma(comercianteId, tomaId);
        conteos.forEach((varianteId, contado) -> fisico.registrarConteo(tomaId, varianteId, contado));
        return conteos.size();
    }

    /**
     * Cierra la toma y aplica las diferencias como ajustes de stock.
     *
     * @see InventarioFisicoService#cerrarToma(Long)
     */
    public TomaResumen cerrar(Long comercianteId, Long tomaId) {
        toma(comercianteId, tomaId);
        fisico.cerrarToma(tomaId);
        return consulta.obtenerToma(tomaId);
    }
}
