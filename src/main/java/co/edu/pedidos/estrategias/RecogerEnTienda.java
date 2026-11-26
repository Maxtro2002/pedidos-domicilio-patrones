package co.edu.pedidos.estrategias;

import co.edu.pedidos.dominio.Pedido;

public class RecogerEnTienda implements EstrategiaEnvio {

    @Override
    public long calcular(Pedido pedido) {
        return 0;
    }

    @Override
    public String descripcion() {
        return "Recoger en tienda";
    }
}
