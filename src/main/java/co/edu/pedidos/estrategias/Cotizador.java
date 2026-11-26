package co.edu.pedidos.estrategias;

import co.edu.pedidos.dominio.Pedido;

/**
 * Contexto del patrón Strategy. Delega el cálculo del descuento y del envío en las estrategias
 * que tenga configuradas, y estas se pueden reemplazar en cualquier momento.
 */
public class Cotizador {

    private EstrategiaDescuento descuento;
    private EstrategiaEnvio envio;

    public Cotizador(EstrategiaDescuento descuento, EstrategiaEnvio envio) {
        cambiarDescuento(descuento);
        cambiarEnvio(envio);
    }

    public void cambiarDescuento(EstrategiaDescuento descuento) {
        if (descuento == null) {
            throw new IllegalArgumentException("La estrategia de descuento no puede ser nula");
        }
        this.descuento = descuento;
    }

    public void cambiarEnvio(EstrategiaEnvio envio) {
        if (envio == null) {
            throw new IllegalArgumentException("La estrategia de envío no puede ser nula");
        }
        this.envio = envio;
    }

    public Cotizacion cotizar(Pedido pedido) {
        long valorDescuento = Math.min(descuento.calcular(pedido), pedido.subtotal());
        long valorEnvio = envio.calcular(pedido);
        return new Cotizacion(pedido.subtotal(), valorDescuento, valorEnvio,
                descuento.descripcion(), envio.descripcion());
    }
}
