package pe.mypeinventario.exception;

/** Se lanza cuando una salida de inventario supera el stock disponible. */
public class StockInsuficienteException extends MypeInventarioException {

    private final String sku;
    private final int disponible;
    private final int solicitado;

    /**
     * @param sku        código de la variante
     * @param disponible unidades disponibles
     * @param solicitado unidades que se intentó retirar
     */
    public StockInsuficienteException(String sku, int disponible, int solicitado) {
        super("Stock insuficiente para " + sku + ": disponible " + disponible + ", solicitado " + solicitado);
        this.sku = sku;
        this.disponible = disponible;
        this.solicitado = solicitado;
    }

    public String getSku() {
        return sku;
    }

    public int getDisponible() {
        return disponible;
    }

    public int getSolicitado() {
        return solicitado;
    }
}