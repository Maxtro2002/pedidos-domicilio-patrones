package co.edu.pedidos.estrategias;

import co.edu.pedidos.dominio.Dinero;
import co.edu.pedidos.dominio.Pedido;

/**
 * Descuento de un valor fijo. Si el subtotal es menor, se descuenta solo el subtotal.
 */
public class DescuentoMontoFijo implements EstrategiaDescuento {

    private final long monto;

    public DescuentoMontoFijo(long monto) {
        if (monto <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor que cero");
        }
        this.monto = monto;
    }

    @Override
    public long calcular(Pedido pedido) {
        return Math.min(monto, pedido.subtotal());
    }

    @Override
    public String descripcion() {
        return Dinero.formato(monto) + " de descuento";
    }
}
