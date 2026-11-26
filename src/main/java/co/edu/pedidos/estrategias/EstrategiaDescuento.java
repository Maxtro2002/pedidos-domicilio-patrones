package co.edu.pedidos.estrategias;

import co.edu.pedidos.dominio.Pedido;

/**
 * Forma de calcular el descuento de un pedido. Cada regla comercial es una implementación
 * distinta y se puede cambiar sin tocar el resto del sistema (patrón Strategy).
 */
public interface EstrategiaDescuento {

    /** Valor en pesos que se descuenta. Nunca supera el subtotal del pedido. */
    long calcular(Pedido pedido);

    String descripcion();
}
