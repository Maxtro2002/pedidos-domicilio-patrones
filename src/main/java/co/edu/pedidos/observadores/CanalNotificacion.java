package co.edu.pedidos.observadores;

/**
 * Medio por el que se avisa al cliente (mensaje de texto, WhatsApp, correo...).
 * En esta versión solo hay una implementación que escribe en consola.
 */
@FunctionalInterface
public interface CanalNotificacion {

    void enviar(String telefono, String mensaje);
}
