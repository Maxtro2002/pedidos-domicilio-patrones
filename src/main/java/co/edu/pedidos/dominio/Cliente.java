package co.edu.pedidos.dominio;

/**
 * Persona que hace el pedido. El teléfono se usa para las notificaciones.
 */
public record Cliente(String id, String nombre, String telefono) {

    public Cliente {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El cliente necesita un identificador");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El cliente necesita un nombre");
        }
        if (telefono == null || !telefono.matches("\\d{7,10}")) {
            throw new IllegalArgumentException("El teléfono debe tener entre 7 y 10 dígitos");
        }
    }
}
