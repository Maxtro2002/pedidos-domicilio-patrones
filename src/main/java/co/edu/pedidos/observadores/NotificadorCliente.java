package co.edu.pedidos.observadores;

import co.edu.pedidos.dominio.Dinero;
import co.edu.pedidos.dominio.EventoPedido;
import co.edu.pedidos.dominio.ObservadorPedido;
import co.edu.pedidos.dominio.Pedido;

/**
 * Avisa al cliente cada vez que su pedido cambia de estado.
 */
public class NotificadorCliente implements ObservadorPedido {

    private final CanalNotificacion canal;

    public NotificadorCliente(CanalNotificacion canal) {
        this.canal = canal;
    }

    @Override
    public void actualizar(EventoPedido evento) {
        Pedido pedido = evento.pedido();
        String mensaje = switch (evento.nuevo()) {
            case RECIBIDO -> "Recibimos tu pedido " + pedido.id() + " por " + Dinero.formato(pedido.total()) + ".";
            case EN_PREPARACION -> "La cocina empezó a preparar tu pedido " + pedido.id() + ".";
            case LISTO -> "Tu pedido " + pedido.id() + " está listo.";
            case EN_CAMINO -> "Tu pedido " + pedido.id() + " va en camino.";
            case ENTREGADO -> "Pedido " + pedido.id() + " entregado. ¡Buen provecho!";
            case CANCELADO -> "Tu pedido " + pedido.id() + " fue cancelado.";
        };
        canal.enviar(pedido.cliente().telefono(), mensaje);
    }
}
