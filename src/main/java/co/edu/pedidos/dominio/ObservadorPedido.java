package co.edu.pedidos.dominio;

/**
 * Contrato que cumple cualquier objeto interesado en los cambios de un pedido.
 * El pedido solo conoce esta interfaz, no las clases concretas que lo observan.
 */
@FunctionalInterface
public interface ObservadorPedido {

    void actualizar(EventoPedido evento);
}
