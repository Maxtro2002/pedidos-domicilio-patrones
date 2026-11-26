package co.edu.pedidos.observadores;

import co.edu.pedidos.dominio.EstadoPedido;
import co.edu.pedidos.dominio.EventoPedido;
import co.edu.pedidos.dominio.ObservadorPedido;

/**
 * Acumula las ventas del día. Solo cuentan los pedidos entregados.
 */
public class ReporteVentas implements ObservadorPedido {

    private long totalVendido;
    private int entregados;
    private int cancelados;

    @Override
    public synchronized void actualizar(EventoPedido evento) {
        if (evento.nuevo() == EstadoPedido.ENTREGADO) {
            totalVendido += evento.pedido().total();
            entregados++;
        } else if (evento.nuevo() == EstadoPedido.CANCELADO) {
            cancelados++;
        }
    }

    public synchronized long totalVendido() {
        return totalVendido;
    }

    public synchronized int pedidosEntregados() {
        return entregados;
    }

    public synchronized int pedidosCancelados() {
        return cancelados;
    }
}
