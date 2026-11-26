package co.edu.pedidos.estrategias;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import co.edu.pedidos.dominio.Cliente;
import co.edu.pedidos.dominio.Direccion;
import co.edu.pedidos.dominio.Pedido;
import co.edu.pedidos.dominio.Producto;
import co.edu.pedidos.dominio.TipoEntrega;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EstrategiasTest {

    private Pedido pedido;     // 2 bandejas + 1 limonada = 65.000, 3 unidades, zona NORTE
    private Pedido pequeno;    // 1 limonada = 9.000

    @BeforeEach
    void preparar() {
        Cliente cliente = new Cliente("C-1", "Laura Gómez", "3001234567");
        Producto bandeja = new Producto("BP-01", "Bandeja paisa", 28000);
        Producto limonada = new Producto("BE-02", "Limonada de coco", 9000);
        Direccion norte = new Direccion("Calle 10 # 5-20", "norte");
        pedido = Pedido.builder().cliente(cliente).agregar(bandeja, 2).agregar(limonada, 1)
                .direccion(norte).build();
        pequeno = Pedido.builder().cliente(cliente).agregar(limonada, 1).direccion(norte).build();
    }

    @Test
    void sinDescuentoNoDescuentaNada() {
        assertEquals(0, new SinDescuento().calcular(pedido));
    }

    @Test
    void descuentoPorcentajeSeCalculaSobreElSubtotal() {
        assertEquals(9750, new DescuentoPorcentaje(15).calcular(pedido));
    }

    @Test
    void descuentoMontoFijoNuncaSuperaElSubtotal() {
        assertEquals(5000, new DescuentoMontoFijo(5000).calcular(pedido));
        assertEquals(9000, new DescuentoMontoFijo(15000).calcular(pequeno));
    }

    @Test
    void descuentoPorVolumenSoloAplicaDesdeElMinimo() {
        EstrategiaDescuento volumen = new DescuentoPorVolumen(3, 10);
        assertEquals(6500, volumen.calcular(pedido));
        assertEquals(0, volumen.calcular(pequeno));
    }

    @Test
    void lasEstrategiasDeDescuentoValidanSusParametros() {
        assertThrows(IllegalArgumentException.class, () -> new DescuentoPorcentaje(0));
        assertThrows(IllegalArgumentException.class, () -> new DescuentoPorcentaje(101));
        assertThrows(IllegalArgumentException.class, () -> new DescuentoMontoFijo(0));
        assertThrows(IllegalArgumentException.class, () -> new DescuentoPorVolumen(0, 10));
    }

    @Test
    void envioTarifaPlanaYRecogerEnTienda() {
        assertEquals(4000, new EnvioTarifaPlana(4000).calcular(pedido));
        assertEquals(0, new RecogerEnTienda().calcular(pedido));
    }

    @Test
    void envioPorZonaIgnoraMayusculasYUsaLaTarifaPorDefecto() {
        EstrategiaEnvio porZona = new EnvioPorZona(Map.of("NORTE", 3000L, "SUR", 6000L), 8000);
        assertEquals(3000, porZona.calcular(pedido));

        Pedido lejano = Pedido.builder().cliente(new Cliente("C-2", "Pedro Ruiz", "3109876543"))
                .agregar(new Producto("BE-02", "Limonada de coco", 9000), 1)
                .direccion(new Direccion("Vereda El Alto", "RURAL")).build();
        assertEquals(8000, porZona.calcular(lejano));
    }

    @Test
    void envioGratisSoloAlLlegarAlUmbral() {
        EstrategiaEnvio gratis = new EnvioGratisDesdeMonto(50000, 5000);
        assertEquals(0, gratis.calcular(pedido));
        assertEquals(5000, gratis.calcular(pequeno));
    }

    @Test
    void elCotizadorCombinaLasDosEstrategias() {
        Cotizador cotizador = new Cotizador(new DescuentoPorcentaje(10), new EnvioTarifaPlana(5000));
        Cotizacion c = cotizador.cotizar(pedido);

        assertEquals(65000, c.subtotal());
        assertEquals(6500, c.descuento());
        assertEquals(5000, c.envio());
        assertEquals(63500, c.total());
    }

    @Test
    void elCotizadorPermiteCambiarDeEstrategiaEnTiempoDeEjecucion() {
        Cotizador cotizador = new Cotizador(new SinDescuento(), new EnvioTarifaPlana(5000));
        assertEquals(70000, cotizador.cotizar(pedido).total());

        cotizador.cambiarEnvio(new EnvioGratisDesdeMonto(50000, 5000));
        cotizador.cambiarDescuento(new DescuentoMontoFijo(10000));
        assertEquals(55000, cotizador.cotizar(pedido).total());
    }

    @Test
    void elCotizadorNoAceptaEstrategiasNulas() {
        assertThrows(IllegalArgumentException.class, () -> new Cotizador(null, new RecogerEnTienda()));
        assertThrows(IllegalArgumentException.class, () -> new Cotizador(new SinDescuento(), null));
    }
}
