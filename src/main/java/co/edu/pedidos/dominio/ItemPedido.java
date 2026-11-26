package co.edu.pedidos.dominio;

/**
 * Línea de un pedido: un producto y la cantidad pedida.
 */
public record ItemPedido(Producto producto, int cantidad) {

    public ItemPedido {
        if (producto == null) {
            throw new IllegalArgumentException("El ítem necesita un producto");
        }
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        }
    }

    public long subtotal() {
        return producto.precio() * cantidad;
    }
}
