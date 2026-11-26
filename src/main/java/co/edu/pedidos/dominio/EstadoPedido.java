package co.edu.pedidos.dominio;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estados por los que pasa un pedido y las transiciones permitidas entre ellos.
 */
public enum EstadoPedido {
    RECIBIDO,
    EN_PREPARACION,
    LISTO,
    EN_CAMINO,
    ENTREGADO,
    CANCELADO;

    /** Estados a los que se puede pasar desde este. */
    public Set<EstadoPedido> siguientesPermitidos() {
        return switch (this) {
            case RECIBIDO -> EnumSet.of(EN_PREPARACION, CANCELADO);
            case EN_PREPARACION -> EnumSet.of(LISTO, CANCELADO);
            case LISTO -> EnumSet.of(EN_CAMINO, ENTREGADO, CANCELADO);
            case EN_CAMINO -> EnumSet.of(ENTREGADO);
            case ENTREGADO, CANCELADO -> EnumSet.noneOf(EstadoPedido.class);
        };
    }

    public boolean puedePasarA(EstadoPedido destino) {
        return siguientesPermitidos().contains(destino);
    }

    public boolean esFinal() {
        return siguientesPermitidos().isEmpty();
    }
}
