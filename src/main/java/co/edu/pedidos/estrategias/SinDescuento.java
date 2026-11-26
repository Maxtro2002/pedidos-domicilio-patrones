package co.edu.pedidos.estrategias;

import co.edu.pedidos.dominio.Pedido;

public class SinDescuento implements EstrategiaDescuento {

    @Override
    public long calcular(Pedido pedido) {
        return 0;
    }

    @Override
    public String descripcion() {
        return "Sin descuento";
    }
}
