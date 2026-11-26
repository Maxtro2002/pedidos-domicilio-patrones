package co.edu.pedidos.observadores;

public class CanalConsola implements CanalNotificacion {

    @Override
    public void enviar(String telefono, String mensaje) {
        System.out.println("   [mensaje a " + telefono + "] " + mensaje);
    }
}
