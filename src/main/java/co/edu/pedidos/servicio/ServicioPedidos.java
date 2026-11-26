package co.edu.pedidos.servicio;

import co.edu.pedidos.dominio.EstadoPedido;
import co.edu.pedidos.dominio.ObservadorPedido;
import co.edu.pedidos.dominio.Pedido;
import co.edu.pedidos.dominio.TipoEntrega;
import co.edu.pedidos.estrategias.Cotizacion;
import co.edu.pedidos.estrategias.Cotizador;
import co.edu.pedidos.estrategias.EstrategiaDescuento;
import co.edu.pedidos.estrategias.EstrategiaEnvio;
import co.edu.pedidos.estrategias.RecogerEnTienda;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Punto de entrada de la aplicación. Junta los tres patrones: entrega los pedidos creados con el
 * Builder, escoge las estrategias de descuento y envío, y suscribe a los observadores.
 */
public class ServicioPedidos {

    private record Cupon(EstrategiaDescuento estrategia, boolean soloPrimeraCompra) {
    }

    private final Map<String, Pedido> pedidos = new LinkedHashMap<>();
    private final Map<String, Cupon> cupones = new HashMap<>();
    private final AtomicInteger secuencia = new AtomicInteger();

    private final EstrategiaEnvio envioDomicilio;
    private final EstrategiaDescuento descuentoAutomatico;
    private final List<ObservadorPedido> observadores;

    public ServicioPedidos(EstrategiaEnvio envioDomicilio, EstrategiaDescuento descuentoAutomatico,
                           List<ObservadorPedido> observadores) {
        this.envioDomicilio = envioDomicilio;
        this.descuentoAutomatico = descuentoAutomatico;
        this.observadores = List.copyOf(observadores);
    }

    public void registrarCupon(String codigo, EstrategiaDescuento estrategia, boolean soloPrimeraCompra) {
        cupones.put(codigo.trim().toUpperCase(), new Cupon(estrategia, soloPrimeraCompra));
    }

    /** Devuelve un Builder que ya trae el siguiente identificador consecutivo. */
    public Pedido.Builder nuevoPedido() {
        return Pedido.builder().id(String.format("P-%04d", secuencia.incrementAndGet()));
    }

    /**
     * Cotiza, fija los totales, suscribe a los observadores y confirma el pedido.
     *
     * @throws IllegalArgumentException si el cupón no existe
     * @throws IllegalStateException si el cupón no aplica para este cliente o el pedido ya existe
     */
    public Pedido registrar(Pedido pedido) {
        if (pedidos.containsKey(pedido.id())) {
            throw new IllegalStateException("Ya existe el pedido " + pedido.id());
        }
        Cotizacion cotizacion = cotizar(pedido);
        pedido.fijarTotales(cotizacion.descuento(), cotizacion.envio());
        observadores.forEach(pedido::suscribir);
        pedido.confirmar();
        pedidos.put(pedido.id(), pedido);
        return pedido;
    }

    private Cotizacion cotizar(Pedido pedido) {
        EstrategiaDescuento descuento = descuentoAutomatico;
        if (pedido.cupon().isPresent()) {
            String codigo = pedido.cupon().get();
            Cupon cupon = cupones.get(codigo);
            if (cupon == null) {
                throw new IllegalArgumentException("El cupón " + codigo + " no existe");
            }
            if (cupon.soloPrimeraCompra() && tieneCompraPrevia(pedido)) {
                throw new IllegalStateException("El cupón " + codigo + " solo es válido en la primera compra");
            }
            descuento = cupon.estrategia();
        }
        EstrategiaEnvio envio = pedido.tipoEntrega() == TipoEntrega.DOMICILIO ? envioDomicilio : new RecogerEnTienda();
        return new Cotizador(descuento, envio).cotizar(pedido);
    }

    private boolean tieneCompraPrevia(Pedido nuevo) {
        return pedidos.values().stream().anyMatch(p ->
                p.cliente().id().equals(nuevo.cliente().id()) && p.estado() != EstadoPedido.CANCELADO);
    }

    /** Pasa el pedido al siguiente estado de su recorrido normal. */
    public Pedido avanzar(String idPedido) {
        Pedido pedido = obtener(idPedido);
        EstadoPedido siguiente = switch (pedido.estado()) {
            case RECIBIDO -> EstadoPedido.EN_PREPARACION;
            case EN_PREPARACION -> EstadoPedido.LISTO;
            case LISTO -> pedido.tipoEntrega() == TipoEntrega.DOMICILIO
                    ? EstadoPedido.EN_CAMINO : EstadoPedido.ENTREGADO;
            case EN_CAMINO -> EstadoPedido.ENTREGADO;
            case ENTREGADO, CANCELADO ->
                    throw new IllegalStateException("El pedido " + idPedido + " ya terminó (" + pedido.estado() + ")");
        };
        pedido.cambiarEstado(siguiente);
        return pedido;
    }

    public Pedido cancelar(String idPedido) {
        Pedido pedido = obtener(idPedido);
        pedido.cambiarEstado(EstadoPedido.CANCELADO);
        return pedido;
    }

    public Pedido obtener(String idPedido) {
        return buscar(idPedido).orElseThrow(() -> new IllegalArgumentException("No existe el pedido " + idPedido));
    }

    public Optional<Pedido> buscar(String idPedido) {
        return Optional.ofNullable(pedidos.get(idPedido));
    }

    public List<Pedido> pedidosEn(EstadoPedido estado) {
        return pedidos.values().stream().filter(p -> p.estado() == estado).toList();
    }

    public List<Pedido> todos() {
        return List.copyOf(pedidos.values());
    }
}
