package club.consola;

import java.io.PrintStream;
import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;

/**
 * Lee y valida las entradas del usuario.
 * Si el formato es inválido muestra el motivo y vuelve a preguntar; si el usuario escribe
 * "cancelar" en cualquier pregunta, se lanza {@link OperacionCancelada} para abandonar la operación.
 */
public class EntradaConsola {
    public static final String CANCELAR = "cancelar";

    private final Scanner in;
    private final PrintStream out;

    public EntradaConsola(Scanner in, PrintStream out) {
        this.in = in;
        this.out = out;
    }

    /** Lee una línea sin validar. Lanza {@link FinDeEntrada} si ya no hay entrada (p. ej. Ctrl+D o Ctrl+Z). */
    private String leerLinea(String mensaje) {
        out.print(mensaje);
        if (!in.hasNextLine()) throw new FinDeEntrada();
        String linea = in.nextLine().trim();
        if (CANCELAR.equalsIgnoreCase(linea)) throw new OperacionCancelada();
        return linea;
    }

    /** Texto obligatorio: no acepta una respuesta vacía. */
    public String texto(String mensaje) {
        while (true) {
            String t = leerLinea(mensaje);
            if (!t.isEmpty()) return t;
            out.println("  Este dato es obligatorio.");
        }
    }

    /** Texto que puede quedar vacío (por ejemplo, un comentario opcional). */
    public String textoOpcional(String mensaje) {
        return leerLinea(mensaje);
    }

    public int entero(String mensaje) {
        while (true) {
            String t = texto(mensaje);
            try { return Integer.parseInt(t); }
            catch (NumberFormatException e) { out.println("  Debe ingresar un número entero."); }
        }
    }

    public int enteroEnRango(String mensaje, int min, int max) {
        while (true) {
            int n = entero(mensaje);
            if (n >= min && n <= max) return n;
            out.printf("  El valor debe estar entre %d y %d.%n", min, max);
        }
    }

    public double decimal(String mensaje) {
        while (true) {
            String t = texto(mensaje);
            Double valor = parsearDecimal(t);
            if (valor != null) return valor;
            out.println("  Debe ingresar un número (use punto o coma para decimales).");
        }
    }

    /** Convierte un número escrito con punto o coma decimal; devuelve null si no es un número finito. */
    public static Double parsearDecimal(String t) {
        try {
            double v = Double.parseDouble(t.replace(',', '.'));
            return Double.isFinite(v) ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public boolean confirmar(String mensaje) {
        while (true) {
            String r = texto(mensaje + " (s/n): ").toLowerCase();
            if (r.equals("s") || r.equals("si") || r.equals("sí")) return true;
            if (r.equals("n") || r.equals("no")) return false;
            out.println("  Responda s o n.");
        }
    }

    public LocalDate fecha(String mensaje) {
        while (true) {
            try { return LocalDate.parse(texto(mensaje)); }
            catch (DateTimeParseException e) { out.println("  Fecha inválida. Use el formato AAAA-MM-DD."); }
        }
    }

    public LocalDateTime fechaHora(String mensaje) {
        while (true) {
            try { return LocalDateTime.parse(texto(mensaje).replace(' ', 'T')); }
            catch (DateTimeParseException e) { out.println("  Fecha y hora inválidas. Use el formato AAAA-MM-DD HH:mm."); }
        }
    }

    public LocalTime hora(String mensaje) {
        while (true) {
            try { return LocalTime.parse(texto(mensaje)); }
            catch (DateTimeParseException e) { out.println("  Hora inválida. Use el formato HH:mm."); }
        }
    }

    public DayOfWeek dia(String mensaje) {
        while (true) {
            DayOfWeek d = convertirDia(texto(mensaje));
            if (d != null) return d;
            out.println("  Día no reconocido (lunes, martes, miércoles, jueves, viernes, sábado o domingo).");
        }
    }

    public static DayOfWeek convertirDia(String texto) {
        switch (Normalizer.normalize(texto.trim().toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "")) {
            case "lunes": return DayOfWeek.MONDAY;
            case "martes": return DayOfWeek.TUESDAY;
            case "miercoles": return DayOfWeek.WEDNESDAY;
            case "jueves": return DayOfWeek.THURSDAY;
            case "viernes": return DayOfWeek.FRIDAY;
            case "sabado": return DayOfWeek.SATURDAY;
            case "domingo": return DayOfWeek.SUNDAY;
            default: return null;
        }
    }

    /**
     * Muestra una lista numerada y devuelve el elemento elegido. La opción 0 cancela la operación.
     * Si la lista está vacía informa que no hay opciones y cancela.
     */
    public <T> T elegir(String titulo, List<T> opciones, Function<T, String> etiqueta) {
        if (opciones.isEmpty()) throw new IllegalStateException("No hay opciones disponibles: " + titulo.replace(":", "").trim().toLowerCase() + ".");
        out.println(titulo);
        for (int i = 0; i < opciones.size(); i++) out.printf("  %d) %s%n", i + 1, etiqueta.apply(opciones.get(i)));
        out.println("  0) Cancelar");
        int n = enteroEnRango("Opción: ", 0, opciones.size());
        if (n == 0) throw new OperacionCancelada();
        return opciones.get(n - 1);
    }

    /** El usuario pidió abandonar la operación en curso. */
    public static class OperacionCancelada extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public OperacionCancelada() { super("Operación cancelada."); }
    }

    /** Ya no hay más entrada disponible: la aplicación debe guardar y terminar. */
    public static class FinDeEntrada extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public FinDeEntrada() { super("Fin de la entrada."); }
    }
}
