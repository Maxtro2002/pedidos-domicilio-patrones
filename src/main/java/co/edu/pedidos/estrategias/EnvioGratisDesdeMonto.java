package co.edu.pedidos.estrategias;

import co.edu.pedidos.dominio.Dinero;
import co.edu.pedidos.dominio.Pedido;

/**
 * El envío es gratis si el subtotal llega al umbral. Si no, cobra la tarifa indicada.
 */
public class EnvioGratisDesdeMonto implements EstrategiaEnvio {

    private final long umbral;
    private final long tarifa;

    public EnvioGratisDesdeMonto(long umbral, long tarifa) {
        if (umbral <= 0 || tarifa < 0) {
            throw new IllegalArgumentException("El umbral debe ser positivo y la tarifa no negativa");
        }
        this.umbral = umbral;
        this.tarifa = tarifa;
    }

    @Override
    public long calcular(Pedido pedido) {
        return pedido.subtotal() >= umbral ? 0 : tarifa;
    }

    @Override
    public String descripcion() {
        return "Envío gratis desde " + Dinero.formato(umbral);
    }
}
