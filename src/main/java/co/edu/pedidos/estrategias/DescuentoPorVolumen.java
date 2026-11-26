package co.edu.pedidos.estrategias;

import co.edu.pedidos.dominio.Pedido;

/**
 * Descuento automático cuando el pedido supera cierta cantidad de unidades.
 */
public class DescuentoPorVolumen implements EstrategiaDescuento {

    private final int unidadesMinimas;
    private final int porcentaje;

    public DescuentoPorVolumen(int unidadesMinimas, int porcentaje) {
        if (unidadesMinimas <= 0) {
            throw new IllegalArgumentException("Las unidades mínimas deben ser mayores que cero");
        }
        if (porcentaje <= 0 || porcentaje > 100) {
            throw new IllegalArgumentException("El porcentaje debe estar entre 1 y 100");
        }
        this.unidadesMinimas = unidadesMinimas;
        this.porcentaje = porcentaje;
    }

    @Override
    public long calcular(Pedido pedido) {
        return pedido.unidades() >= unidadesMinimas ? pedido.subtotal() * porcentaje / 100 : 0;
    }

    @Override
    public String descripcion() {
        return porcentaje + " % por " + unidadesMinimas + " unidades o más";
    }
}
