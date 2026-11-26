package co.edu.pedidos.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PedidoBuilderTest {

    private Cliente cliente;
    private Producto bandeja;
    private Producto limonada;
    private Direccion direccion;

    @BeforeEach
    void preparar() {
        cliente = new Cliente("C-1", "Laura Gómez", "3001234567");
        bandeja = new Producto("BP-01", "Bandeja paisa", 28000);
        limonada = new Producto("BE-02", "Limonada de coco", 9000);
        direccion = new Direccion("Calle 10 # 5-20", "NORTE");
    }

    @Test
    void creaUnPedidoCompletoALaPrimera() {
        Pedido pedido = Pedido.builder()
                .cliente(cliente)
                .agregar(bandeja, 2)
                .agregar(limonada, 1)
                .direccion(direccion)
                .pago(MetodoPago.TARJETA)
                .notas("Sin cebolla, por favor")
                .cupon("bienvenida")
                .build();

        assertEquals(EstadoPedido.RECIBIDO, pedido.estado());
        assertEquals(65000, pedido.subtotal());
        assertEquals(3, pedido.unidades());
        assertEquals(MetodoPago.TARJETA, pedido.metodoPago());
        assertEquals("BIENVENIDA", pedido.cupon().orElseThrow());
    }

    @Test
    void sumaLasUnidadesCuandoSeRepiteUnProducto() {
        Pedido pedido = Pedido.builder()
                .cliente(cliente)
                .agregar(bandeja, 1)
                .agregar(bandeja, 2)
                .direccion(direccion)
                .build();

        assertEquals(1, pedido.items().size());
        assertEquals(3, pedido.items().get(0).cantidad());
    }

    @Test
    void usaValoresPorDefectoParaLoOpcional() {
        Pedido pedido = Pedido.builder().cliente(cliente).agregar(limonada, 1).direccion(direccion).build();

        assertEquals(TipoEntrega.DOMICILIO, pedido.tipoEntrega());
        assertEquals(MetodoPago.EFECTIVO, pedido.metodoPago());
        assertFalse(pedido.notas().isPresent());
        assertFalse(pedido.cupon().isPresent());
        assertTrue(pedido.id().startsWith("P-"));
    }

    @Test
    void rechazaUnPedidoSinProductos() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> Pedido.builder().cliente(cliente).direccion(direccion).build());
        assertTrue(e.getMessage().contains("al menos un producto"));
    }

    @Test
    void rechazaUnPedidoSinCliente() {
        assertThrows(IllegalStateException.class,
                () -> Pedido.builder().agregar(bandeja, 1).direccion(direccion).build());
    }

    @Test
    void exigeDireccionSoloParaDomicilio() {
        assertThrows(IllegalStateException.class,
                () -> Pedido.builder().cliente(cliente).agregar(bandeja, 1).entrega(TipoEntrega.DOMICILIO).build());

        Pedido enTienda = Pedido.builder().cliente(cliente).agregar(bandeja, 1)
                .entrega(TipoEntrega.RECOGER_EN_TIENDA).build();
        assertFalse(enTienda.direccion().isPresent());
    }

    @Test
    void informaTodosLosErroresJuntos() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> Pedido.builder().build());
        assertTrue(e.getMessage().contains("falta el cliente"));
        assertTrue(e.getMessage().contains("al menos un producto"));
        assertTrue(e.getMessage().contains("necesita dirección"));
    }

    @Test
    void limitaLasUnidadesPorProductoYElLargoDeLasNotas() {
        assertThrows(IllegalArgumentException.class,
                () -> Pedido.builder().agregar(bandeja, 21));
        assertThrows(IllegalStateException.class,
                () -> Pedido.builder().cliente(cliente).agregar(bandeja, 1).direccion(direccion)
                        .notas("x".repeat(201)).build());
    }

    @Test
    void losTotalesSoloSePuedenFijarUnaVez() {
        Pedido pedido = Pedido.builder().cliente(cliente).agregar(bandeja, 1).direccion(direccion).build();
        pedido.fijarTotales(2000, 5000);

        assertEquals(31000, pedido.total());
        assertThrows(IllegalStateException.class, () -> pedido.fijarTotales(0, 0));
    }

    @Test
    void elDescuentoNoPuedeSuperarElSubtotal() {
        Pedido pedido = Pedido.builder().cliente(cliente).agregar(limonada, 1).direccion(direccion).build();
        assertThrows(IllegalArgumentException.class, () -> pedido.fijarTotales(9001, 0));
    }
}
