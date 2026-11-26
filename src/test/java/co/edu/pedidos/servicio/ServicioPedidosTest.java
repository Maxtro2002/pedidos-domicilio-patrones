package co.edu.pedidos.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.pedidos.dominio.Cliente;
import co.edu.pedidos.dominio.Direccion;
import co.edu.pedidos.dominio.EstadoPedido;
import co.edu.pedidos.dominio.Pedido;
import co.edu.pedidos.dominio.Producto;
import co.edu.pedidos.dominio.TipoEntrega;
import co.edu.pedidos.estrategias.DescuentoMontoFijo;
import co.edu.pedidos.estrategias.DescuentoPorVolumen;
import co.edu.pedidos.estrategias.DescuentoPorcentaje;
import co.edu.pedidos.estrategias.EnvioTarifaPlana;
import co.edu.pedidos.observadores.AsignadorRepartidor;
import co.edu.pedidos.observadores.NotificadorCliente;
import co.edu.pedidos.observadores.PantallaCocina;
import co.edu.pedidos.observadores.ReporteVentas;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServicioPedidosTest {

    private static final Producto BANDEJA = new Producto("BP-01", "Bandeja paisa", 28000);
    private static final Producto LIMONADA = new Producto("BE-02", "Limonada de coco", 9000);

    private final List<String> mensajes = new ArrayList<>();
    private PantallaCocina cocina;
    private AsignadorRepartidor repartidores;
    private ReporteVentas ventas;
    private ServicioPedidos servicio;
    private Cliente laura;
    private Cliente pedro;
    private Direccion norte;

    @BeforeEach
    void preparar() {
        mensajes.clear();
        cocina = new PantallaCocina();
        repartidores = new AsignadorRepartidor(List.of("Camilo", "Diana"));
        ventas = new ReporteVentas();
        servicio = new ServicioPedidos(new EnvioTarifaPlana(5000), new DescuentoPorVolumen(4, 10),
                List.of(new NotificadorCliente((tel, msg) -> mensajes.add(msg)), cocina, repartidores, ventas));
        servicio.registrarCupon("BIENVENIDA", new DescuentoPorcentaje(15), true);
        servicio.registrarCupon("FIJO5000", new DescuentoMontoFijo(5000), false);
        laura = new Cliente("C-1", "Laura Gómez", "3001234567");
        pedro = new Cliente("C-2", "Pedro Ruiz", "3109876543");
        norte = new Direccion("Calle 10 # 5-20", "NORTE");
    }

    @Test
    void registrarCotizaConfirmaYAvisaATodos() {
        Pedido p = servicio.registrar(servicio.nuevoPedido().cliente(laura).agregar(BANDEJA, 2)
                .agregar(LIMONADA, 1).direccion(norte).cupon("bienvenida").build());

        assertEquals("P-0001", p.id());
        assertEquals(9750, p.descuento());
        assertEquals(5000, p.costoEnvio());
        assertEquals(60250, p.total());
        assertEquals(List.of("P-0001"), cocina.pendientes());
        assertEquals(1, mensajes.size());
    }

    @Test
    void losIdentificadoresSonConsecutivos() {
        assertEquals("P-0001", servicio.nuevoPedido().cliente(laura).agregar(LIMONADA, 1).direccion(norte).build().id());
        assertEquals("P-0002", servicio.nuevoPedido().cliente(laura).agregar(LIMONADA, 1).direccion(norte).build().id());
    }

    @Test
    void sinCuponAplicaElDescuentoPorVolumenSiCorresponde() {
        Pedido grande = servicio.registrar(servicio.nuevoPedido().cliente(pedro).agregar(LIMONADA, 4)
                .entrega(TipoEntrega.RECOGER_EN_TIENDA).build());
        Pedido chico = servicio.registrar(servicio.nuevoPedido().cliente(pedro).agregar(LIMONADA, 1)
                .entrega(TipoEntrega.RECOGER_EN_TIENDA).build());

        assertEquals(3600, grande.descuento());
        assertEquals(0, chico.descuento());
        assertEquals(0, grande.costoEnvio());
    }

    @Test
    void bienvenidaSoloValeEnLaPrimeraCompra() {
        servicio.registrar(servicio.nuevoPedido().cliente(laura).agregar(BANDEJA, 1).direccion(norte).build());

        IllegalStateException e = assertThrows(IllegalStateException.class, () ->
                servicio.registrar(servicio.nuevoPedido().cliente(laura).agregar(BANDEJA, 1)
                        .direccion(norte).cupon("BIENVENIDA").build()));
        assertTrue(e.getMessage().contains("primera compra"));
        assertEquals(1, servicio.todos().size());
    }

    @Test
    void siLaPrimeraCompraSeCancelaElCuponVuelveAServir() {
        Pedido primero = servicio.registrar(servicio.nuevoPedido().cliente(laura).agregar(BANDEJA, 1)
                .direccion(norte).build());
        servicio.cancelar(primero.id());

        Pedido segundo = servicio.registrar(servicio.nuevoPedido().cliente(laura).agregar(BANDEJA, 1)
                .direccion(norte).cupon("BIENVENIDA").build());
        assertEquals(4200, segundo.descuento());
    }

    @Test
    void unCuponInexistenteSeRechaza() {
        assertThrows(IllegalArgumentException.class, () ->
                servicio.registrar(servicio.nuevoPedido().cliente(laura).agregar(BANDEJA, 1)
                        .direccion(norte).cupon("NOEXISTE").build()));
    }

    @Test
    void unPedidoRechazadoNoQuedaRegistradoNiNotifica() {
        try {
            servicio.registrar(servicio.nuevoPedido().cliente(laura).agregar(BANDEJA, 1)
                    .direccion(norte).cupon("NOEXISTE").build());
        } catch (IllegalArgumentException esperado) {
            // el cupón no existe
        }
        assertTrue(servicio.todos().isEmpty());
        assertTrue(mensajes.isEmpty());
        assertTrue(cocina.pendientes().isEmpty());
    }

    @Test
    void noSePuedeRegistrarDosVecesElMismoPedido() {
        Pedido p = servicio.registrar(servicio.nuevoPedido().cliente(laura).agregar(BANDEJA, 1).direccion(norte).build());
        assertThrows(IllegalStateException.class, () -> servicio.registrar(p));
    }

    @Test
    void elRecorridoCompletoDeUnDomicilioActivaCadaObservador() {
        Pedido p = servicio.registrar(servicio.nuevoPedido().cliente(laura).agregar(BANDEJA, 1).direccion(norte).build());

        servicio.avanzar(p.id());   // EN_PREPARACION
        assertTrue(cocina.pendientes().isEmpty());
        servicio.avanzar(p.id());   // LISTO
        assertEquals("Camilo", repartidores.repartidorDe(p.id()).orElseThrow());
        servicio.avanzar(p.id());   // EN_CAMINO
        servicio.avanzar(p.id());   // ENTREGADO

        assertEquals(EstadoPedido.ENTREGADO, p.estado());
        assertEquals(33000, ventas.totalVendido());
        assertEquals(5, mensajes.size());
        assertThrows(IllegalStateException.class, () -> servicio.avanzar(p.id()));
    }

    @Test
    void recogerEnTiendaSaltaElEstadoEnCamino() {
        Pedido p = servicio.registrar(servicio.nuevoPedido().cliente(pedro).agregar(LIMONADA, 1)
                .entrega(TipoEntrega.RECOGER_EN_TIENDA).build());
        servicio.avanzar(p.id());
        servicio.avanzar(p.id());
        servicio.avanzar(p.id());

        assertEquals(EstadoPedido.ENTREGADO, p.estado());
    }

    @Test
    void consultasPorEstado() {
        Pedido a = servicio.registrar(servicio.nuevoPedido().cliente(laura).agregar(BANDEJA, 1).direccion(norte).build());
        Pedido b = servicio.registrar(servicio.nuevoPedido().cliente(pedro).agregar(LIMONADA, 1).direccion(norte).build());
        servicio.avanzar(a.id());

        assertEquals(List.of(a), servicio.pedidosEn(EstadoPedido.EN_PREPARACION));
        assertEquals(List.of(b), servicio.pedidosEn(EstadoPedido.RECIBIDO));
        assertThrows(IllegalArgumentException.class, () -> servicio.obtener("P-9999"));
    }
}
