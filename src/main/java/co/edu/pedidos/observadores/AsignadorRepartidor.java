package co.edu.pedidos.observadores;

import co.edu.pedidos.dominio.EstadoPedido;
import co.edu.pedidos.dominio.EventoPedido;
import co.edu.pedidos.dominio.ObservadorPedido;
import co.edu.pedidos.dominio.TipoEntrega;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Asigna un repartidor, por turnos, cuando un pedido a domicilio queda listo.
 */
public class AsignadorRepartidor implements ObservadorPedido {

    private final List<String> repartidores;
    private final Map<String, String> asignaciones = new LinkedHashMap<>();
    private int siguiente = 0;

    public AsignadorRepartidor(List<String> repartidores) {
        if (repartidores == null || repartidores.isEmpty()) {
            throw new IllegalArgumentException("Se necesita al menos un repartidor");
        }
        this.repartidores = List.copyOf(repartidores);
    }

    @Override
    public synchronized void actualizar(EventoPedido evento) {
        if (evento.nuevo() == EstadoPedido.LISTO && evento.pedido().tipoEntrega() == TipoEntrega.DOMICILIO) {
            String repartidor = repartidores.get(siguiente % repartidores.size());
            siguiente++;
            asignaciones.put(evento.pedido().id(), repartidor);
        }
    }

    public synchronized Optional<String> repartidorDe(String idPedido) {
        return Optional.ofNullable(asignaciones.get(idPedido));
    }
}
