package co.edu.pedidos.observadores;

import co.edu.pedidos.dominio.EstadoPedido;
import co.edu.pedidos.dominio.EventoPedido;
import co.edu.pedidos.dominio.ObservadorPedido;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Lista de pedidos que la cocina todavía no empieza a preparar, en orden de llegada.
 */
public class PantallaCocina implements ObservadorPedido {

    private final Set<String> pendientes = new LinkedHashSet<>();

    @Override
    public synchronized void actualizar(EventoPedido evento) {
        String id = evento.pedido().id();
        if (evento.nuevo() == EstadoPedido.RECIBIDO) {
            pendientes.add(id);
        } else {
            pendientes.remove(id);
        }
    }

    public synchronized List<String> pendientes() {
        return new ArrayList<>(pendientes);
    }
}
