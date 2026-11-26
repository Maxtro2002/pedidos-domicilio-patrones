package co.edu.pedidos.estrategias;

/**
 * Resultado de cotizar un pedido.
 */
public record Cotizacion(long subtotal, long descuento, long envio, String detalleDescuento, String detalleEnvio) {

    public long total() {
        return subtotal - descuento + envio;
    }
}
