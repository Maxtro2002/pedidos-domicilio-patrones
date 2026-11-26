package co.edu.pedidos.dominio;

import java.time.Instant;

/**
 * Aviso que el pedido envía a sus observadores cuando cambia de estado.
 * Cuando el pedido se registra por primera vez, {@code anterior} es {@code null}.
 */
public record EventoPedido(Pedido pedido, EstadoPedido anterior, EstadoPedido nuevo, Instant momento) {

    public boolean esRegistroInicial() {
        return anterior == null;
    }
}
