package pe.mypeinventario.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Una línea de venta calcula su subtotal y guarda el costo del momento para medir la ganancia. */
class DetalleVentaTest {

    private static VarianteSKU variante(BigDecimal costo) {
        return new VarianteSKU(1L, "POL-M-AZU", null, "M", "Azul", costo, new BigDecimal("29.90"), 5);
    }

    @Test
    void elSubtotalYElCostoSeCalculanPorLasUnidades() {
        DetalleVenta detalle = new DetalleVenta(null, variante(new BigDecimal("12.00")), 3, new BigDecimal("29.90"));

        assertThat(detalle.calcularSubtotal()).isEqualByComparingTo("89.70");
        assertThat(detalle.calcularCosto()).isEqualByComparingTo("36.00");
    }

    @Test
    void sinCostoConocidoNoSeInventaUnaGanancia() {
        DetalleVenta detalle = new DetalleVenta(null, variante(null), 1, new BigDecimal("29.90"));

        assertThat(detalle.calcularCosto()).isNull();
    }

    @Test
    void losDatosInvalidosSeRechazan() {
        VarianteSKU variante = variante(new BigDecimal("12.00"));

        assertThatThrownBy(() -> new DetalleVenta(null, null, 1, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DetalleVenta(null, variante, 0, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DetalleVenta(null, variante, 1, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DetalleVenta(null, variante, 1, new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
