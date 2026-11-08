package pe.mypeinventario.consulta;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import pe.mypeinventario.domain.EstadoToma;

/**
 * Vista de solo lectura de una toma de inventario con sus conteos.
 *
 * @param id       identificador
 * @param tiendaId tienda donde se cuenta
 * @param fecha    cuándo se inició
 * @param estado   en curso o cerrada
 * @param conteos  variantes ya contadas
 */
public record TomaResumen(Long id, Long tiendaId, LocalDateTime fecha, EstadoToma estado,
                          List<ConteoResumen> conteos) {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** @return la fecha como «06/11/2026 19:30» */
    public String fechaTexto() {
        return FORMATO_FECHA.format(fecha);
    }

    /** @return {@code true} si todavía se puede contar */
    public boolean enCurso() {
        return estado == EstadoToma.EN_CURSO;
    }

    /** @return «En curso» o «Cerrada» */
    public String estadoTexto() {
        return enCurso() ? "En curso" : "Cerrada";
    }

    /** @return cuántas variantes tienen una diferencia entre lo contado y el sistema */
    public long conDiferencia() {
        return conteos.stream().filter(conteo -> conteo.diferencia() != 0).count();
    }

    /** @return {@code true} si la variante ya fue contada en esta toma */
    public boolean contada(Long varianteId) {
        return conteos.stream().anyMatch(conteo -> conteo.varianteId().equals(varianteId));
    }
}
