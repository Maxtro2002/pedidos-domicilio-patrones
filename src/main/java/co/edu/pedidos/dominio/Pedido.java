package co.edu.pedidos.dominio;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Pedido de un cliente.
 *
 * <p>Tiene varios datos obligatorios y otros opcionales (dirección, notas, cupón, forma de pago),
 * y algunos dependen de otros: si la entrega es a domicilio hace falta una dirección. Por eso
 * se crea con el patrón Builder y no con un constructor largo. El constructor es privado y
 * {@link Builder#build()} valida todas las reglas antes de entregar el objeto, así que no existe
 * un pedido a medias dentro del sistema.</p>
 *
 * <p>Además es el sujeto del patrón Observer: guarda una lista de {@link ObservadorPedido} y les
 * avisa cada vez que cambia de estado.</p>
 */
public final class Pedido {

    private static final Logger LOG = Logger.getLogger(Pedido.class.getName());
    private static final int MAX_NOTAS = 200;
    private static final int MAX_UNIDADES_POR_PRODUCTO = 20;

    private final String id;
    private final Cliente cliente;
    private final List<ItemPedido> items;
    private final TipoEntrega tipoEntrega;
    private final Direccion direccion;
    private final MetodoPago metodoPago;
    private final String notas;
    private final String cupon;
    private final Instant creadoEn;

    private EstadoPedido estado = EstadoPedido.RECIBIDO;
    private long descuento;
    private long costoEnvio;
    private boolean totalesFijados;
    private boolean confirmado;
    private final List<ObservadorPedido> observadores = new CopyOnWriteArrayList<>();

    private Pedido(Builder b) {
        this.id = b.id;
        this.cliente = b.cliente;
        this.items = List.copyOf(b.items.values());
        this.tipoEntrega = b.tipoEntrega;
        this.direccion = b.direccion;
        this.metodoPago = b.metodoPago;
        this.notas = b.notas;
        this.cupon = b.cupon;
        this.creadoEn = Instant.now();
    }

    public String id() {
        return id;
    }

    public Cliente cliente() {
        return cliente;
    }

    public List<ItemPedido> items() {
        return items;
    }

    public TipoEntrega tipoEntrega() {
        return tipoEntrega;
    }

    public Optional<Direccion> direccion() {
        return Optional.ofNullable(direccion);
    }

    public MetodoPago metodoPago() {
        return metodoPago;
    }

    public Optional<String> notas() {
        return Optional.ofNullable(notas);
    }

    public Optional<String> cupon() {
        return Optional.ofNullable(cupon);
    }

    public Instant creadoEn() {
        return creadoEn;
    }

    public EstadoPedido estado() {
        return estado;
    }

    public long subtotal() {
        return items.stream().mapToLong(ItemPedido::subtotal).sum();
    }

    public int unidades() {
        return items.stream().mapToInt(ItemPedido::cantidad).sum();
    }

    public long descuento() {
        return descuento;
    }

    public long costoEnvio() {
        return costoEnvio;
    }

    public long total() {
        return subtotal() - descuento + costoEnvio;
    }

    /**
     * Guarda el descuento y el costo de envío calculados. Solo se puede hacer una vez,
     * para que el valor que se le cotizó al cliente no cambie después.
     */
    public void fijarTotales(long descuento, long costoEnvio) {
        if (totalesFijados) {
            throw new IllegalStateException("Los totales del pedido " + id + " ya fueron fijados");
        }
        if (descuento < 0 || descuento > subtotal()) {
            throw new IllegalArgumentException("El descuento debe estar entre 0 y el subtotal");
        }
        if (costoEnvio < 0) {
            throw new IllegalArgumentException("El costo de envío no puede ser negativo");
        }
        this.descuento = descuento;
        this.costoEnvio = costoEnvio;
        this.totalesFijados = true;
    }

    // ------------------------------------------------------------------
    // Observer: el pedido es el sujeto
    // ------------------------------------------------------------------

    public void suscribir(ObservadorPedido observador) {
        if (observador == null) {
            throw new IllegalArgumentException("El observador no puede ser nulo");
        }
        observadores.add(observador);
    }

    public void cancelarSuscripcion(ObservadorPedido observador) {
        observadores.remove(observador);
    }

    /** Registra el pedido en el sistema y lo anuncia a los observadores como RECIBIDO. */
    public void confirmar() {
        if (confirmado) {
            throw new IllegalStateException("El pedido " + id + " ya fue confirmado");
        }
        if (!totalesFijados) {
            throw new IllegalStateException("Hay que fijar los totales antes de confirmar el pedido " + id);
        }
        confirmado = true;
        notificar(new EventoPedido(this, null, estado, Instant.now()));
    }

    /**
     * Cambia el estado si la transición está permitida y avisa a los observadores.
     *
     * @throws IllegalStateException si el cambio no es válido para el estado o el tipo de entrega
     */
    public void cambiarEstado(EstadoPedido nuevo) {
        if (!confirmado) {
            throw new IllegalStateException("El pedido " + id + " todavía no fue confirmado");
        }
        if (!estado.puedePasarA(nuevo)) {
            throw new IllegalStateException("No se puede pasar de " + estado + " a " + nuevo);
        }
        if (nuevo == EstadoPedido.EN_CAMINO && tipoEntrega != TipoEntrega.DOMICILIO) {
            throw new IllegalStateException("Solo los pedidos a domicilio pasan a EN_CAMINO");
        }
        if (nuevo == EstadoPedido.ENTREGADO && estado == EstadoPedido.LISTO
                && tipoEntrega == TipoEntrega.DOMICILIO) {
            throw new IllegalStateException("Un pedido a domicilio debe pasar por EN_CAMINO antes de entregarse");
        }
        EstadoPedido anterior = estado;
        estado = nuevo;
        notificar(new EventoPedido(this, anterior, nuevo, Instant.now()));
    }

    /**
     * Recorre una copia de la lista, así un observador puede darse de baja mientras recibe el aviso.
     * Si un observador falla, se registra el error y los demás siguen recibiendo el evento.
     */
    private void notificar(EventoPedido evento) {
        for (ObservadorPedido observador : observadores) {
            try {
                observador.actualizar(evento);
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING, "Falló un observador del pedido " + id, e);
            }
        }
    }

    @Override
    public String toString() {
        return "Pedido " + id + " (" + cliente.nombre() + ", " + estado + ", " + Dinero.formato(total()) + ")";
    }

    // ------------------------------------------------------------------
    // Builder
    // ------------------------------------------------------------------

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String id = "P-" + UUID.randomUUID().toString().substring(0, 8);
        private Cliente cliente;
        private final Map<String, ItemPedido> items = new LinkedHashMap<>();
        private TipoEntrega tipoEntrega = TipoEntrega.DOMICILIO;
        private Direccion direccion;
        private MetodoPago metodoPago = MetodoPago.EFECTIVO;
        private String notas;
        private String cupon;

        private Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder cliente(Cliente cliente) {
            this.cliente = cliente;
            return this;
        }

        /** Agrega un producto. Si ya estaba en el pedido, suma las unidades. */
        public Builder agregar(Producto producto, int cantidad) {
            if (producto == null) {
                throw new IllegalArgumentException("El producto no puede ser nulo");
            }
            ItemPedido existente = items.get(producto.codigo());
            int total = cantidad + (existente == null ? 0 : existente.cantidad());
            if (total > MAX_UNIDADES_POR_PRODUCTO) {
                throw new IllegalArgumentException("Máximo " + MAX_UNIDADES_POR_PRODUCTO
                        + " unidades por producto en un mismo pedido");
            }
            items.put(producto.codigo(), new ItemPedido(producto, total));
            return this;
        }

        public Builder entrega(TipoEntrega tipoEntrega) {
            this.tipoEntrega = tipoEntrega;
            return this;
        }

        public Builder direccion(Direccion direccion) {
            this.direccion = direccion;
            return this;
        }

        public Builder pago(MetodoPago metodoPago) {
            this.metodoPago = metodoPago;
            return this;
        }

        public Builder notas(String notas) {
            this.notas = (notas == null || notas.isBlank()) ? null : notas.trim();
            return this;
        }

        public Builder cupon(String cupon) {
            this.cupon = (cupon == null || cupon.isBlank()) ? null : cupon.trim().toUpperCase();
            return this;
        }

        /** Valida todas las reglas y crea el pedido. */
        public Pedido build() {
            List<String> errores = new ArrayList<>();
            if (id == null || id.isBlank()) {
                errores.add("falta el identificador");
            }
            if (cliente == null) {
                errores.add("falta el cliente");
            }
            if (items.isEmpty()) {
                errores.add("el pedido debe tener al menos un producto");
            }
            if (tipoEntrega == null) {
                errores.add("falta el tipo de entrega");
            }
            if (metodoPago == null) {
                errores.add("falta el método de pago");
            }
            if (tipoEntrega == TipoEntrega.DOMICILIO && direccion == null) {
                errores.add("un pedido a domicilio necesita dirección");
            }
            if (notas != null && notas.length() > MAX_NOTAS) {
                errores.add("las notas no pueden superar " + MAX_NOTAS + " caracteres");
            }
            if (!errores.isEmpty()) {
                throw new IllegalStateException("Pedido inválido: " + String.join("; ", errores));
            }
            if (tipoEntrega == TipoEntrega.RECOGER_EN_TIENDA) {
                direccion = null;
            }
            return new Pedido(this);
        }
    }
}
