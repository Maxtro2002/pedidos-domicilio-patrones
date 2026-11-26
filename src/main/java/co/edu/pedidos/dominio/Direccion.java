package co.edu.pedidos.dominio;

/**
 * Dirección de entrega. La zona sirve para calcular el costo del envío.
 */
public record Direccion(String texto, String zona) {

    public Direccion {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("La dirección no puede estar vacía");
        }
        if (zona == null || zona.isBlank()) {
            throw new IllegalArgumentException("La dirección necesita una zona");
        }
    }
}
