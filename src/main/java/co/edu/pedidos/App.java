package co.edu.pedidos;

import co.edu.pedidos.dominio.Cliente;
import co.edu.pedidos.dominio.Dinero;
import co.edu.pedidos.dominio.Direccion;
import co.edu.pedidos.dominio.MetodoPago;
import co.edu.pedidos.dominio.Pedido;
import co.edu.pedidos.dominio.Producto;
import co.edu.pedidos.dominio.TipoEntrega;
import co.edu.pedidos.estrategias.DescuentoPorVolumen;
import co.edu.pedidos.estrategias.DescuentoPorcentaje;
import co.edu.pedidos.estrategias.EnvioPorZona;
import co.edu.pedidos.observadores.AsignadorRepartidor;
import co.edu.pedidos.observadores.CanalConsola;
import co.edu.pedidos.observadores.NotificadorCliente;
import co.edu.pedidos.observadores.PantallaCocina;
import co.edu.pedidos.observadores.ReporteVentas;
import co.edu.pedidos.servicio.ServicioPedidos;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Demostración por consola de un día de trabajo de la cocina.
 * Se ejecuta con {@code mvn compile exec:java}.
 */
public class App {

    public static void main(String[] args) {
        // Para que las tildes se vean bien en consolas configuradas en UTF-8
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

        // Observadores compartidos por todos los pedidos
        PantallaCocina cocina = new PantallaCocina();
        AsignadorRepartidor repartidores = new AsignadorRepartidor(List.of("Camilo", "Diana"));
        ReporteVentas ventas = new ReporteVentas();

        // Estrategias: envío por zona y 10 % por 4 unidades o más cuando no hay cupón
        ServicioPedidos servicio = new ServicioPedidos(
                new EnvioPorZona(Map.of("NORTE", 3000L, "SUR", 6000L), 8000),
                new DescuentoPorVolumen(4, 10),
                List.of(new NotificadorCliente(new CanalConsola()), cocina, repartidores, ventas));
        servicio.registrarCupon("BIENVENIDA", new DescuentoPorcentaje(15), true);

        Producto bandeja = new Producto("BP-01", "Bandeja paisa", 28000);
        Producto limonada = new Producto("BE-02", "Limonada de coco", 9000);
        Cliente laura = new Cliente("C-1", "Laura Gómez", "3001234567");
        Cliente pedro = new Cliente("C-2", "Pedro Ruiz", "3109876543");

        titulo("1. Pedido a domicilio con cupón de bienvenida (Builder + Strategy)");
        Pedido p1 = servicio.registrar(servicio.nuevoPedido()
                .cliente(laura)
                .agregar(bandeja, 2)
                .agregar(limonada, 1)
                .direccion(new Direccion("Calle 10 # 5-20", "NORTE"))
                .pago(MetodoPago.TARJETA)
                .notas("Sin cebolla, por favor")
                .cupon("BIENVENIDA")
                .build());
        resumen(p1);

        titulo("2. Pedido para recoger con descuento por volumen");
        Pedido p2 = servicio.registrar(servicio.nuevoPedido()
                .cliente(pedro)
                .agregar(limonada, 4)
                .entrega(TipoEntrega.RECOGER_EN_TIENDA)
                .build());
        resumen(p2);

        titulo("3. El cupón de bienvenida no sirve en la segunda compra");
        try {
            servicio.registrar(servicio.nuevoPedido()
                    .cliente(laura)
                    .agregar(limonada, 1)
                    .direccion(new Direccion("Calle 10 # 5-20", "NORTE"))
                    .cupon("BIENVENIDA")
                    .build());
        } catch (IllegalStateException e) {
            System.out.println("   Rechazado: " + e.getMessage());
        }

        titulo("4. Un pedido incompleto no llega a existir (Builder valida al construir)");
        try {
            Pedido.builder().cliente(pedro).build();
        } catch (IllegalStateException e) {
            System.out.println("   " + e.getMessage());
        }

        titulo("5. La cocina trabaja (Observer: cada cambio avisa a todos)");
        System.out.println("   En pantalla de cocina: " + cocina.pendientes());
        servicio.avanzar(p1.id());
        servicio.avanzar(p2.id());
        System.out.println("   En pantalla de cocina: " + cocina.pendientes());
        servicio.avanzar(p1.id());
        System.out.println("   Repartidor de " + p1.id() + ": " + repartidores.repartidorDe(p1.id()).orElse("-"));
        servicio.avanzar(p1.id());
        servicio.avanzar(p1.id());
        servicio.avanzar(p2.id());
        servicio.avanzar(p2.id());

        titulo("6. Cierre del día");
        System.out.println("   Pedidos entregados: " + ventas.pedidosEntregados());
        System.out.println("   Total vendido:      " + Dinero.formato(ventas.totalVendido()));
    }

    private static void resumen(Pedido p) {
        System.out.println("   " + p.id() + " | subtotal " + Dinero.formato(p.subtotal())
                + " | descuento " + Dinero.formato(p.descuento())
                + " | envío " + Dinero.formato(p.costoEnvio())
                + " | total " + Dinero.formato(p.total()));
    }

    private static void titulo(String texto) {
        System.out.println();
        System.out.println(texto);
    }
}
