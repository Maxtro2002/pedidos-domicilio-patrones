package co.edu.pedidos.dominio;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utilidad para mostrar valores en pesos colombianos.
 */
public final class Dinero {

    private static final Locale COLOMBIA = Locale.forLanguageTag("es-CO");

    private Dinero() {
    }

    public static String formato(long pesos) {
        return "$ " + NumberFormat.getIntegerInstance(COLOMBIA).format(pesos);
    }
}
