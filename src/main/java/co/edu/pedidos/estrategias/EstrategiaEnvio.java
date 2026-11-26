package co.edu.pedidos.estrategias;

import co.edu.pedidos.dominio.Pedido;

/**
 * Forma de calcular el costo de envío. Es una familia de algoritmos distinta de los descuentos,
 * así que tiene su propia interfaz y se puede cambiar de forma independiente.
 */
public interface EstrategiaEnvio {

    long calcular(Pedido pedido);

    String descripcion();
}
