package co.edu.pedidos.dominio;

/**
 * Producto del menú. El precio se maneja en pesos colombianos, sin decimales.
 */
public record Producto(String codigo, String nombre, long precio) {

    public Producto {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("El producto necesita un código");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El producto necesita un nombre");
        }
        if (precio <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor que cero");
        }
    }
}
