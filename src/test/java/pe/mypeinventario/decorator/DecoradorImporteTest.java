package pe.mypeinventario.decorator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import pe.mypeinventario.exception.OperacionInvalidaException;

/**
 * Valida los descuentos combinables (patrón Decorator) y el desglose del IGV
 * incluido en el precio. No usa Spring: son clases simples.
 */
class DecoradorImporteTest {

    private static BigDecimal monto(String valor) {
        return new BigDecimal(valor);
    }

    @Test
    void elImporteBaseSinDecoradoresEsElSubtotal() {
        CalculoImporte importe = new ImporteBase(monto("100.00"));

        assertThat(importe.importe()).isEqualByComparingTo("100.00");
        assertThat(importe.descripcion()).isEqualTo("Subtotal");
    }

    @Test
    void elDescuentoPorcentualReduceElImporte() {
        CalculoImporte importe = new DescuentoPorcentual(new ImporteBase(monto("100.00")), monto("10"));

        assertThat(importe.importe()).isEqualByComparingTo("90.00");
        assertThat(importe.descripcion()).isEqualTo("Subtotal + descuento 10 %");
    }

    @Test
    void elDescuentoDeMontoFijoReduceElImporte() {
        CalculoImporte importe = new DescuentoMontoFijo(new ImporteBase(monto("100.00")), monto("5.00"));

        assertThat(importe.importe()).isEqualByComparingTo("95.00");
    }

    @Test
    void elOrdenDeLosDecoradoresImporta() {
        CalculoImporte porcentajeLuegoCupon = new DescuentoMontoFijo(
                new DescuentoPorcentual(new ImporteBase(monto("100.00")), monto("10")), monto("5.00"));
        CalculoImporte cuponLuegoPorcentaje = new DescuentoPorcentual(
                new DescuentoMontoFijo(new ImporteBase(monto("100.00")), monto("5.00")), monto("10"));

        assertThat(porcentajeLuegoCupon.importe()).isEqualByComparingTo("85.00");
        assertThat(cuponLuegoPorcentaje.importe()).isEqualByComparingTo("85.50");
    }

    @Test
    void elMontoFijoNuncaDejaElImporteNegativo() {
        CalculoImporte importe = new DescuentoMontoFijo(new ImporteBase(monto("20.00")), monto("50.00"));

        assertThat(importe.importe()).isEqualByComparingTo("0.00");
    }

    @Test
    void elDescuentoSeRedondeaADosDecimales() {
        CalculoImporte importe = new DescuentoPorcentual(new ImporteBase(monto("99.99")), monto("15"));

        // 99.99 x 15 % = 14.9985, que se redondea a 15.00
        assertThat(importe.importe()).isEqualByComparingTo("84.99");
    }

    @Test
    void losDecoradoresSePuedenAnidarVariasVeces() {
        CalculoImporte importe = new DescuentoPorcentual(
                new DescuentoPorcentual(new ImporteBase(monto("100.00")), monto("10")), monto("10"));

        assertThat(importe.importe()).isEqualByComparingTo("81.00");
        assertThat(importe.descripcion()).isEqualTo("Subtotal + descuento 10 % + descuento 10 %");
    }

    @Test
    void losDatosInvalidosSeRechazan() {
        CalculoImporte base = new ImporteBase(monto("100.00"));

        assertThatThrownBy(() -> new ImporteBase(monto("-1"))).isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> new ImporteBase(null)).isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> new DescuentoPorcentual(base, monto("0")))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> new DescuentoPorcentual(base, monto("101")))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> new DescuentoPorcentual(base, null))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> new DescuentoMontoFijo(base, monto("0")))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> new DescuentoMontoFijo(base, null))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> new DescuentoPorcentual(null, monto("10")))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void elDesgloseSeparaElIgvIncluidoEnElTotal() {
        DesgloseIgv desglose = DesgloseIgv.desde(monto("118.00"), monto("0.18"));

        assertThat(desglose.total()).isEqualByComparingTo("118.00");
        assertThat(desglose.igv()).isEqualByComparingTo("18.00");
        assertThat(desglose.baseImponible()).isEqualByComparingTo("100.00");
    }

    @Test
    void elDesgloseRedondeaElIgvADosDecimales() {
        DesgloseIgv desglose = DesgloseIgv.desde(monto("100.00"), monto("0.18"));

        // 100 x 0.18 / 1.18 = 15.2542..., que se redondea a 15.25
        assertThat(desglose.igv()).isEqualByComparingTo("15.25");
        assertThat(desglose.baseImponible()).isEqualByComparingTo("84.75");
    }

    @Test
    void conIgvCeroTodoEsBaseImponible() {
        DesgloseIgv desglose = DesgloseIgv.desde(monto("50.00"), BigDecimal.ZERO);

        assertThat(desglose.igv()).isEqualByComparingTo("0.00");
        assertThat(desglose.baseImponible()).isEqualByComparingTo("50.00");
    }

    @Test
    void elDesgloseRechazaDatosInvalidos() {
        assertThatThrownBy(() -> DesgloseIgv.desde(null, monto("0.18")))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> DesgloseIgv.desde(monto("-1"), monto("0.18")))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> DesgloseIgv.desde(monto("100"), null))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> DesgloseIgv.desde(monto("100"), monto("1.5")))
                .isInstanceOf(OperacionInvalidaException.class);
    }
}
