package co.edu.pedidos.estrategias;

import co.edu.pedidos.dominio.Pedido;
import java.util.Map;

/**
 * Cobra según la zona de la dirección. Si la zona no está en la tabla, usa la tarifa por defecto.
 */
public class EnvioPorZona implements EstrategiaEnvio {

    private final Map<String, Long> tarifasPorZona;
    private final long tarifaPorDefecto;

    public EnvioPorZona(Map<String, Long> tarifasPorZona, long tarifaPorDefecto) {
        if (tarifaPorDefecto < 0 || tarifasPorZona.values().stream().anyMatch(t -> t < 0)) {
            throw new IllegalArgumentException("Las tarifas no pueden ser negativas");
        }
        this.tarifasPorZona = Map.copyOf(tarifasPorZona);
        this.tarifaPorDefecto = tarifaPorDefecto;
    }

    @Override
    public long calcular(Pedido pedido) {
        return pedido.direccion()
                .map(d -> tarifasPorZona.getOrDefault(d.zona().toUpperCase(), tarifaPorDefecto))
                .orElse(0L);
    }

    @Override
    public String descripcion() {
        return "Tarifa según la zona de entrega";
    }
}
