package club.modelo.competencias;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class DeporteConjunto implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private String nombre;
    private int jugadoresPorEquipo;
    private List<ReglaPuntuacion> reglasPuntuacion = new ArrayList<>();

    public DeporteConjunto(String nombre, int jugadoresPorEquipo) {
        setNombre(nombre);
        setJugadoresPorEquipo(jugadoresPorEquipo);
    }

    public void agregarRegla(ReglaPuntuacion regla) {
        if (regla == null) throw new IllegalArgumentException("La regla es obligatoria.");
        if (!reglasPuntuacion.contains(regla)) reglasPuntuacion.add(regla);
    }
    public void validarMarcador(int local, int visitante) { }

    public int getMinJugadores() { return jugadoresPorEquipo; }

    public boolean posicionValida(String posicion) {
        if (posicion == null || posicion.trim().isEmpty()) return false;
        String deporte = normalizar(nombre);
        String valor = normalizar(posicion).replace(' ', '-');
        if (deporte.contains("futbol")) return Arrays.asList("portero", "defensa", "mediocampista", "delantero").contains(valor);
        if (deporte.contains("baloncesto")) return Arrays.asList("base", "escolta", "alero", "ala-pivot", "pivot").contains(valor);
        if (deporte.contains("voleibol")) return Arrays.asList("colocador", "opuesto", "central", "punta", "libero").contains(valor);
        return false;
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto.trim().toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { if (nombre == null || nombre.trim().isEmpty()) throw new IllegalArgumentException("El nombre es obligatorio."); this.nombre = nombre.trim(); }
    public int getJugadoresPorEquipo() { return jugadoresPorEquipo; }
    public void setJugadoresPorEquipo(int jugadoresPorEquipo) { if (jugadoresPorEquipo <= 0) throw new IllegalArgumentException("La cantidad mínima de jugadores debe ser positiva."); this.jugadoresPorEquipo = jugadoresPorEquipo; }
    public List<ReglaPuntuacion> getReglasPuntuacion() { return reglasPuntuacion; }
}
