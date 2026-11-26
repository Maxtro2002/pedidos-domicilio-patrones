package co.edu.pedidos.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PedidoObserverTest {

    private Pedido domicilio;
    private Pedido enTienda;
    private final List<EventoPedido> eventos = new ArrayList<>();

    @BeforeEach
    void preparar() {
        Cliente cliente = new Cliente("C-1", "Laura Gómez", "3001234567");
        Producto bandeja = new Producto("BP-01", "Bandeja paisa", 28000);
        domicilio = Pedido.builder().cliente(cliente).agregar(bandeja, 1)
                .direccion(new Direccion("Calle 10 # 5-20", "NORTE")).build();
        domicilio.fijarTotales(0, 5000);
        enTienda = Pedido.builder().cliente(cliente).agregar(bandeja, 1)
                .entrega(TipoEntrega.RECOGER_EN_TIENDA).build();
        enTienda.fijarTotales(0, 0);
        eventos.clear();
    }

    @Test
    void confirmarAnunciaElRegistroInicial() {
        domicilio.suscribir(eventos::add);
        domicilio.confirmar();

        assertEquals(1, eventos.size());
        assertTrue(eventos.get(0).esRegistroInicial());
        assertNull(eventos.get(0).anterior());
        assertEquals(EstadoPedido.RECIBIDO, eventos.get(0).nuevo());
    }

    @Test
    void cadaCambioDeEstadoLlegaATodosLosObservadores() {
        List<EventoPedido> otro = new ArrayList<>();
        domicilio.suscribir(eventos::add);
        domicilio.suscribir(otro::add);
        domicilio.confirmar();

        domicilio.cambiarEstado(EstadoPedido.EN_PREPARACION);

        assertEquals(2, eventos.size());
        assertEquals(2, otro.size());
        assertEquals(EstadoPedido.RECIBIDO, eventos.get(1).anterior());
        assertEquals(EstadoPedido.EN_PREPARACION, eventos.get(1).nuevo());
    }

    @Test
    void unObservadorDadoDeBajaDejaDeRecibirAvisos() {
        ObservadorPedido observador = eventos::add;
        domicilio.suscribir(observador);
        domicilio.confirmar();
        domicilio.cancelarSuscripcion(observador);

        domicilio.cambiarEstado(EstadoPedido.EN_PREPARACION);

        assertEquals(1, eventos.size());
    }

    @Test
    void unObservadorQueFallaNoImpideAvisarALosDemas() {
        domicilio.suscribir(e -> {
            throw new IllegalStateException("fallo simulado");
        });
        domicilio.suscribir(eventos::add);

        domicilio.confirmar();

        assertEquals(1, eventos.size());
    }

    @Test
    void unObservadorPuedeDarseDeBajaMientrasRecibeElAviso() {
        ObservadorPedido[] unico = new ObservadorPedido[1];
        unico[0] = e -> {
            eventos.add(e);
            domicilio.cancelarSuscripcion(unico[0]);
        };
        domicilio.suscribir(unico[0]);
        domicilio.suscribir(eventos::add);

        domicilio.confirmar();
        domicilio.cambiarEstado(EstadoPedido.EN_PREPARACION);

        assertEquals(3, eventos.size());
    }

    @Test
    void noSePuedeConfirmarDosVecesNiCambiarEstadoSinConfirmar() {
        assertThrows(IllegalStateException.class, () -> domicilio.cambiarEstado(EstadoPedido.EN_PREPARACION));
        domicilio.confirmar();
        assertThrows(IllegalStateException.class, () -> domicilio.confirmar());
    }

    @Test
    void noSePuedeConfirmarSinTotales() {
        Pedido sinTotales = Pedido.builder().cliente(new Cliente("C-2", "Pedro Ruiz", "3109876543"))
                .agregar(new Producto("BE-02", "Limonada", 9000), 1)
                .entrega(TipoEntrega.RECOGER_EN_TIENDA).build();
        assertThrows(IllegalStateException.class, sinTotales::confirmar);
    }

    @Test
    void elDomicilioRecorreTodosLosEstados() {
        domicilio.confirmar();
        domicilio.cambiarEstado(EstadoPedido.EN_PREPARACION);
        domicilio.cambiarEstado(EstadoPedido.LISTO);
        domicilio.cambiarEstado(EstadoPedido.EN_CAMINO);
        domicilio.cambiarEstado(EstadoPedido.ENTREGADO);

        assertEquals(EstadoPedido.ENTREGADO, domicilio.estado());
        assertTrue(domicilio.estado().esFinal());
    }

    @Test
    void elDomicilioNoSePuedeEntregarSinPasarPorEnCamino() {
        domicilio.confirmar();
        domicilio.cambiarEstado(EstadoPedido.EN_PREPARACION);
        domicilio.cambiarEstado(EstadoPedido.LISTO);

        assertThrows(IllegalStateException.class, () -> domicilio.cambiarEstado(EstadoPedido.ENTREGADO));
    }

    @Test
    void recogerEnTiendaNuncaPasaPorEnCamino() {
        enTienda.confirmar();
        enTienda.cambiarEstado(EstadoPedido.EN_PREPARACION);
        enTienda.cambiarEstado(EstadoPedido.LISTO);

        assertThrows(IllegalStateException.class, () -> enTienda.cambiarEstado(EstadoPedido.EN_CAMINO));
        enTienda.cambiarEstado(EstadoPedido.ENTREGADO);
        assertEquals(EstadoPedido.ENTREGADO, enTienda.estado());
    }

    @Test
    void unPedidoCanceladoNoCambiaMas() {
        domicilio.confirmar();
        domicilio.cambiarEstado(EstadoPedido.CANCELADO);

        assertThrows(IllegalStateException.class, () -> domicilio.cambiarEstado(EstadoPedido.EN_PREPARACION));
    }

    @Test
    void noSePuedeSaltarUnEstado() {
        domicilio.confirmar();
        assertThrows(IllegalStateException.class, () -> domicilio.cambiarEstado(EstadoPedido.LISTO));
    }
}
