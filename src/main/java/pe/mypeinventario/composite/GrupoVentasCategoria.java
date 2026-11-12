package pe.mypeinventario.composite;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Compuesto del patrón Composite: una categoría con subcategorías. Sus totales son los
 * de sus ventas propias más los de todos sus hijos.
 */
public class GrupoVentasCategoria implements NodoVentasCategoria {

    private final String nombre;
    private final VentasDeCategoria propias;
    private final List<NodoVentasCategoria> hijos = new ArrayList<>();

    /**
     * @param nombre  nombre de la categoría
     * @param propias ventas de los productos que están directamente en esta categoría
     */
    public GrupoVentasCategoria(String nombre, VentasDeCategoria propias) {
        this.nombre = Objects.requireNonNull(nombre, "El nombre es obligatorio");
        this.propias = Objects.requireNonNull(propias, "Las ventas propias son obligatorias");
    }

    /**
     * Agrega una subcategoría al grupo.
     *
     * @param nodo subcategoría, simple o grupo
     */
    public void agregar(NodoVentasCategoria nodo) {
        hijos.add(Objects.requireNonNull(nodo, "La subcategoría es obligatoria"));
    }

    @Override
    public String nombre() {
        return nombre;
    }

    @Override
    public long unidades() {
        return propias.unidades() + hijos.stream().mapToLong(NodoVentasCategoria::unidades).sum();
    }

    @Override
    public BigDecimal ingresos() {
        return hijos.stream().map(NodoVentasCategoria::ingresos).reduce(propias.ingresos(), BigDecimal::add);
    }

    @Override
    public BigDecimal costo() {
        return hijos.stream().map(NodoVentasCategoria::costo).reduce(propias.costo(), BigDecimal::add);
    }

    @Override
    public List<NodoVentasCategoria> subcategorias() {
        return Collections.unmodifiableList(hijos);
    }
}
