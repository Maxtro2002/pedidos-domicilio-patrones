package co.edu.pedidos.observadores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.pedidos.dominio.Cliente;
import co.edu.pedidos.dominio.Direccion;
import co.edu.pedidos.dominio.EstadoPedido;
import co.edu.pedidos.dominio.Pedido;
import co.edu.pedidos.dominio.Producto;
import co.edu.pedidos.dominio.TipoEntrega;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ObservadoresTest {

    private static final Producto BANDEJA = new Producto("BP-01", "Bandeja paisa", 28000);

    private final List<String> mensajes = new ArrayList<>();
    private Cliente cliente;

    @BeforeEach
    void preparar() {
        mensajes.clear();
        cliente = new Cliente("C-1", "Laura Gómez", "3001234567");
    }

    private Pedido domicilio(String id) {
        Pedido p = Pedido.builder().id(id).cliente(cliente).agregar(BANDEJA, 1)
                .direccion(new Direccion("Calle 10 # 5-20", "NORTE")).build();
        p.fijarTotales(0, 5000);
        return p;
    }

    private Pedido enTienda(String id) {
        Pedido p = Pedido.builder().id(id).cliente(cliente).agregar(BANDEJA, 1)
                .entrega(TipoEntrega.RECOGER_EN_TIENDA).build();
        p.fijarTotales(0, 0);
        return p;
    }

    @Test
    void elClienteRecibeUnMensajePorCadaCambio() {
        Pedido pedido = domicilio("P-1");
        pedido.suscribir(new NotificadorCliente((tel, msg) -> mensajes.add(tel + ": " + msg)));

        pedido.confirmar();
        pedido.cambiarEstado(EstadoPedido.EN_PREPARACION);
        pedido.cambiarEstado(EstadoPedido.LISTO);

        assertEquals(3, mensajes.size());
        assertTrue(mensajes.get(0).startsWith("3001234567: Recibimos tu pedido P-1"));
        assertTrue(mensajes.get(0).contains("$ 33.000"));
        assertTrue(mensajes.get(2).contains("está listo"));
    }

    @Test
    void laCocinaVeSoloLosPedidosSinPreparar() {
        PantallaCocina cocina = new PantallaCocina();
        Pedido uno = domicilio("P-1");
        Pedido dos = domicilio("P-2");
        Pedido tres = enTienda("P-3");
        for (Pedido p : List.of(uno, dos, tres)) {
            p.suscribir(cocina);
            p.confirmar();
        }
        assertEquals(List.of("P-1", "P-2", "P-3"), cocina.pendientes());

        uno.cambiarEstado(EstadoPedido.EN_PREPARACION);
        dos.cambiarEstado(EstadoPedido.CANCELADO);

        assertEquals(List.of("P-3"), cocina.pendientes());
    }

    @Test
    void losRepartidoresSeAsignanPorTurnos() {
        AsignadorRepartidor asignador = new AsignadorRepartidor(List.of("Camilo", "Diana"));
        List<Pedido> pedidos = List.of(domicilio("P-1"), domicilio("P-2"), domicilio("P-3"));
        for (Pedido p : pedidos) {
            p.suscribir(asignador);
            p.confirmar();
            p.cambiarEstado(EstadoPedido.EN_PREPARACION);
            p.cambiarEstado(EstadoPedido.LISTO);
        }

        assertEquals("Camilo", asignador.repartidorDe("P-1").orElseThrow());
        assertEquals("Diana", asignador.repartidorDe("P-2").orElseThrow());
        assertEquals("Camilo", asignador.repartidorDe("P-3").orElseThrow());
    }

    @Test
    void losPedidosEnTiendaNoNecesitanRepartidor() {
        AsignadorRepartidor asignador = new AsignadorRepartidor(List.of("Camilo"));
        Pedido pedido = enTienda("P-9");
        pedido.suscribir(asignador);
        pedido.confirmar();
        pedido.cambiarEstado(EstadoPedido.EN_PREPARACION);
        pedido.cambiarEstado(EstadoPedido.LISTO);

        assertFalse(asignador.repartidorDe("P-9").isPresent());
    }

    @Test
    void elAsignadorExigeAlMenosUnRepartidor() {
        assertThrows(IllegalArgumentException.class, () -> new AsignadorRepartidor(List.of()));
    }

    @Test
    void elReporteSoloSumaLoEntregado() {
        ReporteVentas reporte = new ReporteVentas();
        Pedido entregado = enTienda("P-1");
        Pedido cancelado = domicilio("P-2");
        Pedido enCurso = domicilio("P-3");
        for (Pedido p : List.of(entregado, cancelado, enCurso)) {
            p.suscribir(reporte);
            p.confirmar();
        }
        entregado.cambiarEstado(EstadoPedido.EN_PREPARACION);
        entregado.cambiarEstado(EstadoPedido.LISTO);
        entregado.cambiarEstado(EstadoPedido.ENTREGADO);
        cancelado.cambiarEstado(EstadoPedido.CANCELADO);

        assertEquals(28000, reporte.totalVendido());
        assertEquals(1, reporte.pedidosEntregados());
        assertEquals(1, reporte.pedidosCancelados());
    }
}
