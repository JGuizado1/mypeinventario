package pe.mypeinventario.consulta;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import pe.mypeinventario.domain.EstadoToma;
import pe.mypeinventario.domain.TipoMovimiento;

/** Los textos que ve el comerciante salen de estos resúmenes: se prueban sin base de datos. */
class ResumenesTest {

    private static VarianteResumen variante(String talla, String color) {
        return new VarianteResumen(1L, "SKU", null, talla, color, null, BigDecimal.ONE, 5, 10, false);
    }

    @Test
    void laDescripcionDeUnaVarianteUneTallaYColorSegunLoQueHaya() {
        assertThat(variante("M", "Azul").descripcion()).isEqualTo("M / Azul");
        assertThat(variante("M", null).descripcion()).isEqualTo("M");
        assertThat(variante(" ", "Azul").descripcion()).isEqualTo("Azul");
        assertThat(variante(null, "").descripcion()).isEqualTo("—");
    }

    @Test
    void cadaTipoDeMovimientoTieneSuTextoYElEfectoSuSigno() {
        LocalDateTime fecha = LocalDateTime.of(2026, 11, 6, 9, 5);
        for (TipoMovimiento tipo : TipoMovimiento.values()) {
            assertThat(new MovimientoResumen(1L, tipo, 2, 2, fecha, null).tipoTexto()).isNotBlank();
        }
        assertThat(new MovimientoResumen(1L, TipoMovimiento.COMPRA, 2, 2, fecha, null).efectoTexto()).isEqualTo("+2");
        assertThat(new MovimientoResumen(1L, TipoMovimiento.VENTA, 2, -2, fecha, null).efectoTexto()).isEqualTo("-2");
        assertThat(new MovimientoResumen(1L, TipoMovimiento.VENTA, 2, -2, fecha, null).fechaTexto())
                .isEqualTo("06/11/2026 09:05");
    }

    @Test
    void elConteoCompaSuDiferenciaConElSistema() {
        ConteoResumen sobra = new ConteoResumen(1L, "SKU", "Polo", "M", "Azul", 5, 8);
        ConteoResumen falta = new ConteoResumen(2L, "SKU2", "Polo", null, null, 5, 2);
        ConteoResumen igual = new ConteoResumen(3L, "SKU3", "Polo", "L", null, 5, 5);

        assertThat(sobra.diferenciaTexto()).isEqualTo("+3");
        assertThat(falta.diferenciaTexto()).isEqualTo("-3");
        assertThat(igual.diferenciaTexto()).isEqualTo("0");
        assertThat(sobra.variante()).isEqualTo("M / Azul");
        assertThat(falta.variante()).isEqualTo("—");
    }

    @Test
    void laTomaSabeSuEstadoSusDiferenciasYQueSeContoYa() {
        ConteoResumen sobra = new ConteoResumen(1L, "SKU", "Polo", "M", "Azul", 5, 8);
        ConteoResumen igual = new ConteoResumen(3L, "SKU3", "Polo", "L", null, 5, 5);
        LocalDateTime fecha = LocalDateTime.of(2026, 11, 6, 19, 30);
        TomaResumen enCurso = new TomaResumen(1L, 5L, fecha, EstadoToma.EN_CURSO, List.of(sobra, igual));
        TomaResumen cerrada = new TomaResumen(2L, 5L, fecha, EstadoToma.CERRADA, List.of());

        assertThat(enCurso.enCurso()).isTrue();
        assertThat(enCurso.estadoTexto()).isEqualTo("En curso");
        assertThat(cerrada.estadoTexto()).isEqualTo("Cerrada");
        assertThat(enCurso.conDiferencia()).isEqualTo(1);
        assertThat(enCurso.contada(1L)).isTrue();
        assertThat(enCurso.contada(9L)).isFalse();
        assertThat(enCurso.fechaTexto()).isEqualTo("06/11/2026 19:30");
    }
}
