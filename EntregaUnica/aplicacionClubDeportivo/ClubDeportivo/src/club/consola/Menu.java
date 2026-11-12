package club.consola;

import java.util.ArrayList;
import java.util.List;

/**
 * Menú de consola formado por opciones (clave, descripción, acción).
 * Agregar una funcionalidad nueva es agregar una línea con {@link #opcion}, sin tocar el ciclo de lectura.
 */
public class Menu {
    private final String titulo;
    private final List<Opcion> opciones = new ArrayList<>();

    public Menu(String titulo) { this.titulo = titulo; }

    public Menu opcion(String clave, String descripcion, Runnable accion) {
        for (Opcion o : opciones) if (o.clave != null && o.clave.equalsIgnoreCase(clave)) throw new IllegalArgumentException("Clave repetida en el menú: " + clave);
        opciones.add(new Opcion(clave, descripcion, accion));
        return this;
    }

    /** Agrega un subtítulo que agrupa las opciones siguientes. */
    public Menu seccion(String nombre) {
        opciones.add(new Opcion(null, nombre, null));
        return this;
    }

    public String getTitulo() { return titulo; }

    public String mostrar() {
        StringBuilder sb = new StringBuilder("\n--- ").append(titulo).append(" ---\n");
        for (Opcion o : opciones) {
            if (o.clave == null) sb.append("[").append(o.descripcion).append("]\n");
            else sb.append(String.format("  %3s) %s%n", o.clave, o.descripcion));
        }
        sb.append("    0) Cerrar sesión\n");
        return sb.toString();
    }

    /** Devuelve la acción asociada a la clave, o null si la clave no existe. */
    public Runnable accion(String clave) {
        for (Opcion o : opciones) if (o.clave != null && o.clave.equalsIgnoreCase(clave.trim())) return o.accion;
        return null;
    }

    private static final class Opcion {
        final String clave;
        final String descripcion;
        final Runnable accion;
        Opcion(String clave, String descripcion, Runnable accion) {
            this.clave = clave;
            this.descripcion = descripcion;
            this.accion = accion;
        }
    }
}
