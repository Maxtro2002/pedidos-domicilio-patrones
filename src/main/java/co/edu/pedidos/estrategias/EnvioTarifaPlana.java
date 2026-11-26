package co.edu.pedidos.estrategias;

import co.edu.pedidos.dominio.Dinero;
import co.edu.pedidos.dominio.Pedido;

public class EnvioTarifaPlana implements EstrategiaEnvio {

    private final long tarifa;

    public EnvioTarifaPlana(long tarifa) {
        if (tarifa < 0) {
            throw new IllegalArgumentException("La tarifa no puede ser negativa");
        }
        this.tarifa = tarifa;
    }

    @Override
    public long calcular(Pedido pedido) {
        return tarifa;
    }

    @Override
    public String descripcion() {
        return "Tarifa plana de " + Dinero.formato(tarifa);
    }
}
