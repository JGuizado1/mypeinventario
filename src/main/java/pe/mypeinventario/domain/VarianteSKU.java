package pe.mypeinventario.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.mypeinventario.config.ParametrosStock;

/**
 * Unidad mínima de inventario: una combinación de talla y color de un
 * {@link ProductoItem}. Su stock no se guarda: se deriva de la suma de sus
 * {@link MovimientoInventario}, que solo se agregan y nunca se editan.
 */
@Getter
@Setter
@NoArgsConstructor
public class VarianteSKU {

    private Long id;
    private String sku;
    private String codigoBarras;
    private String talla;
    private String color;
    private BigDecimal precioCompra;
    private BigDecimal precioVenta;
    /** Stock mínimo propio; si es {@code null} se usa el valor por defecto de la tienda. */
    private Integer stockMinimo;

    @Setter(AccessLevel.NONE)
    private List<MovimientoInventario> movimientos = new ArrayList<>();

    @Setter(AccessLevel.NONE)
    private List<AlertaReabastecimiento> alertas = new ArrayList<>();

    public VarianteSKU(Long id, String sku, String codigoBarras, String talla, String color,
                       BigDecimal precioCompra, BigDecimal precioVenta, Integer stockMinimo) {
        this.id = id;
        this.sku = sku;
        this.codigoBarras = codigoBarras;
        this.talla = talla;
        this.color = color;
        this.precioCompra = precioCompra;
        this.precioVenta = precioVenta;
        this.stockMinimo = stockMinimo;
    }

    /** @return vista de solo lectura del historial de movimientos */
    public List<MovimientoInventario> getMovimientos() {
        return Collections.unmodifiableList(movimientos);
    }

    /** @return vista de solo lectura de las alertas generadas */
    public List<AlertaReabastecimiento> getAlertas() {
        return Collections.unmodifiableList(alertas);
    }

    /**
     * Calcula el stock actual sumando el efecto de todos los movimientos.
     *
     * @return unidades disponibles
     */
    // JPA (Etapa 2): con muchos movimientos conviene calcularlo con una consulta SUM en el repository.
    public int calcularStock() {
        int stock = 0;
        for (MovimientoInventario movimiento : movimientos) {
            stock += movimiento.calcularEfecto();
        }
        return stock;
    }

    /**
     * Registra un movimiento en el historial, siempre que no deje el stock negativo.
     *
     * @param movimiento movimiento a registrar
     * @throws IllegalArgumentException si el movimiento es nulo
     * @throws IllegalStateException    si la salida supera el stock disponible
     */
    public void registrarMovimiento(MovimientoInventario movimiento) {
        if (movimiento == null) {
            throw new IllegalArgumentException("El movimiento no puede ser nulo");
        }
        int stockResultante = calcularStock() + movimiento.calcularEfecto();
        if (stockResultante < 0) {
            throw new IllegalStateException(
                    "Stock insuficiente para la variante " + sku + ": quedaría en " + stockResultante);
        }
        movimientos.add(movimiento);
    }

    /**
     * Determina el stock mínimo que aplica a esta variante: el propio, si lo tiene,
     * o el valor por defecto de los parámetros recibidos.
     *
     * @param parametros parámetros de stock (por ejemplo, la configuración de la tienda)
     * @return el stock mínimo vigente
     * @throws IllegalArgumentException si los parámetros son nulos
     */
    public int resolverStockMinimo(ParametrosStock parametros) {
        if (parametros == null) {
            throw new IllegalArgumentException("Los parámetros de stock son obligatorios");
        }
        return stockMinimo != null ? stockMinimo : parametros.getStockMinimoPorDefecto();
    }

    /**
     * Indica si el stock actual alcanzó o bajó del mínimo.
     *
     * @param parametros parámetros de stock (por ejemplo, la configuración de la tienda)
     * @return {@code true} si hace falta reabastecer
     */
    public boolean requiereReabastecimiento(ParametrosStock parametros) {
        return calcularStock() <= resolverStockMinimo(parametros);
    }

    /**
     * Asocia una alerta generada a esta variante.
     *
     * @param alerta alerta a asociar
     * @throws IllegalArgumentException si la alerta es nula
     */
    public void agregarAlerta(AlertaReabastecimiento alerta) {
        if (alerta == null) {
            throw new IllegalArgumentException("La alerta no puede ser nula");
        }
        alertas.add(alerta);
    }
}