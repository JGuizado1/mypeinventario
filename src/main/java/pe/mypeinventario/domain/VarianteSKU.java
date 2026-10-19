package pe.mypeinventario.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.mypeinventario.config.ParametrosStock;
import pe.mypeinventario.exception.StockInsuficienteException;

/**
 * Unidad mínima de inventario: una combinación de talla y color de un
 * {@link ProductoItem}. Su stock no se guarda: se deriva de la suma de sus
 * {@link MovimientoInventario}, que solo se agregan y nunca se editan.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class VarianteSKU {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 50)
    private String sku;
    @Column(length = 30)
    private String codigoBarras;
    @Column(length = 20)
    private String talla;
    @Column(length = 30)
    private String color;
    @Column(precision = 12, scale = 2)
    private BigDecimal precioCompra;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precioVenta;
    /** Stock mínimo propio; si es {@code null} se usa el valor por defecto de la tienda. */
    private Integer stockMinimo;

    @Setter(AccessLevel.NONE)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "variante_id", nullable = false)
    private List<MovimientoInventario> movimientos = new ArrayList<>();

    @Setter(AccessLevel.NONE)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "variante_id", nullable = false)
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
     * @throws StockInsuficienteException si la salida supera el stock disponible
     */
    public void registrarMovimiento(MovimientoInventario movimiento) {
        if (movimiento == null) {
            throw new IllegalArgumentException("El movimiento no puede ser nulo");
        }
        int stockActual = calcularStock();
        int stockResultante = stockActual + movimiento.calcularEfecto();
        if (stockResultante < 0) {
            throw new StockInsuficienteException(sku, stockActual, -movimiento.calcularEfecto());
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