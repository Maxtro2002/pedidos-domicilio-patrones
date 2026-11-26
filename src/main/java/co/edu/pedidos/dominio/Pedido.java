package co.edu.pedidos.dominio;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Pedido de un cliente.
 *
 * <p>Tiene varios datos obligatorios y otros opcionales (dirección, notas, cupón, forma de pago),
 * y algunos dependen de otros: si la entrega es a domicilio hace falta una dirección. Por eso
 * se crea con el patrón Builder y no con un constructor largo. El constructor es privado y
 * {@link Builder#build()} valida todas las reglas antes de entregar el objeto, así que no existe
 * un pedido a medias dentro del sistema.</p>
 */
public final class Pedido {

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
