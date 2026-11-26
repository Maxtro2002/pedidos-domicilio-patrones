package co.edu.pedidos.estrategias;

import co.edu.pedidos.dominio.Pedido;

/**
 * Descuento de un porcentaje sobre el subtotal. Se usa para cupones como BIENVENIDA.
 */
public class DescuentoPorcentaje implements EstrategiaDescuento {

    private final int porcentaje;

    public DescuentoPorcentaje(int porcentaje) {
        if (porcentaje <= 0 || porcentaje > 100) {
            throw new IllegalArgumentException("El porcentaje debe estar entre 1 y 100");
        }
        this.porcentaje = porcentaje;
    }

    @Override
    public long calcular(Pedido pedido) {
        return pedido.subtotal() * porcentaje / 100;
    }

    @Override
    public String descripcion() {
        return porcentaje + " % de descuento";
    }
}
