package club.modelo.competencias;

import java.util.ArrayList;
import java.util.List;

public class FaseEliminacion implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private List<RondaEliminacion> rondas = new ArrayList<>();
    private List<EquipoCompetencia> pasesDirectos = new ArrayList<>();
    private EquipoCompetencia campeon;

    public void generarLlavesEliminacion() {
        rondas.clear();
        pasesDirectos.clear();
        campeon = null;
    }

    public void generarLlavesEliminacion(List<EquipoCompetencia> clasificados) {
        if (clasificados == null || clasificados.size() < 2) throw new IllegalArgumentException("Se requieren al menos dos clasificados.");
        rondas.clear();
        pasesDirectos.clear();
        campeon = null;
        generarRonda(clasificados, 1);
    }

    private void generarRonda(List<EquipoCompetencia> participantes, int orden) {
        String nombre = "Ronda " + orden;
        RondaEliminacion ronda = new RondaEliminacion(nombre, orden);
        int partidos = participantes.size() / 2;
        for (int i = 0; i < partidos; i++) {
            PartidoCompetencia partido = new PartidoCompetencia(null, "Pendiente", "Por definir");
            partido.agregarEquipo(participantes.get(i));
            partido.agregarEquipo(participantes.get(participantes.size() - 1 - i));
            ronda.getPartidos().add(partido);
        }
        if (participantes.size() % 2 == 1) ronda.getPasesDirectos().add(participantes.get(participantes.size() / 2));
        rondas.add(ronda);
    }

    public RondaEliminacion avanzarGanadores() {
        if (rondas.isEmpty()) throw new IllegalStateException("Todavía no se generan llaves.");
        RondaEliminacion actual = rondas.get(rondas.size() - 1);
        List<EquipoCompetencia> ganadores = new ArrayList<>(actual.getPasesDirectos());
        for (PartidoCompetencia partido : actual.getPartidos()) {
            if (!partido.isFinalizado()) throw new IllegalStateException("Todos los partidos de la ronda deben tener resultado.");
            EquipoCompetencia ganador = partido.getGanador();
            if (ganador == null) throw new IllegalStateException("Un empate debe resolverse antes de avanzar.");
            ganadores.add(ganador);
        }
        if (ganadores.size() == 1) { campeon = ganadores.get(0); return null; }
        if (ganadores.size() < 2) throw new IllegalStateException("No hay suficientes ganadores para la siguiente ronda.");
        generarRonda(ganadores, actual.getOrden() + 1);
        return rondas.get(rondas.size() - 1);
    }

    public List<RondaEliminacion> getRondas() { return rondas; }
    public void setRondas(List<RondaEliminacion> rondas) { this.rondas = rondas == null ? new ArrayList<>() : rondas; }
    public List<EquipoCompetencia> getPasesDirectos() { return rondas.isEmpty() ? pasesDirectos : rondas.get(rondas.size() - 1).getPasesDirectos(); }
    public EquipoCompetencia getCampeon() { return campeon; }
}
