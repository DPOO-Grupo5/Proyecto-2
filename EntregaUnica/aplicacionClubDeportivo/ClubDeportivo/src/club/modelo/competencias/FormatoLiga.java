package club.modelo.competencias;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FormatoLiga extends Formato implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private ReglaPuntuacion regla;
    private List<PosicionEquipo> posiciones = new ArrayList<>();
    private List<PartidoCompetencia> encuentros = new ArrayList<>();

    public FormatoLiga(String nombreFormato, ReglaPuntuacion regla) {
        super(nombreFormato);
        if (regla == null) throw new IllegalArgumentException("La regla de puntuación es obligatoria.");
        this.regla = regla;
    }

    @Override
    public void generarEncuentros(List<EquipoCompetencia> equipos) {
        encuentros.clear();
        posiciones.clear();
        if (equipos == null || equipos.size() < 2) return;
        for (EquipoCompetencia equipo : equipos) {
            if (equipo == null) throw new IllegalArgumentException("La lista contiene un equipo nulo.");
            if (!contieneEquipo(equipo)) posiciones.add(new PosicionEquipo(equipo));
        }
        for (int i = 0; i < equipos.size(); i++) {
            for (int j = i + 1; j < equipos.size(); j++) {
                PartidoCompetencia partido = new PartidoCompetencia(null, "Pendiente", "Por definir");
                partido.agregarEquipo(equipos.get(i));
                partido.agregarEquipo(equipos.get(j));
                encuentros.add(partido);
            }
        }
        ordenarPosiciones();
    }

    public void actualizarPosiciones() {
        for (PosicionEquipo posicion : posiciones) {
            posicion.setPuntuacion(0);
        }
        // Cada nueva tabla se reconstruye para evitar acumular un resultado al corregirlo.
        List<PosicionEquipo> tabla = new ArrayList<>();
        for (PosicionEquipo posicion : posiciones) tabla.add(new PosicionEquipo(posicion.getEquipo()));
        posiciones = tabla;
        for (PartidoCompetencia partido : encuentros) {
            if (!partido.isFinalizado() || partido.getEquipos().size() != 2) continue;
            PosicionEquipo local = buscarPosicion(partido.getEquipos().get(0));
            PosicionEquipo visitante = buscarPosicion(partido.getEquipos().get(1));
            local.registrarResultado(partido.getMarcadorLocal(), partido.getMarcadorVisitante(), regla.getVictoria(), regla.getEmpate(), regla.getDerrota());
            visitante.registrarResultado(partido.getMarcadorVisitante(), partido.getMarcadorLocal(), regla.getVictoria(), regla.getEmpate(), regla.getDerrota());
        }
        ordenarPosiciones();
    }

    private PosicionEquipo buscarPosicion(EquipoCompetencia equipo) {
        for (PosicionEquipo posicion : posiciones) if (posicion.getEquipo() == equipo) return posicion;
        throw new IllegalStateException("El equipo del partido no pertenece a esta tabla.");
    }

    private boolean contieneEquipo(EquipoCompetencia equipo) {
        for (PosicionEquipo posicion : posiciones) if (posicion.getEquipo() == equipo) return true;
        return false;
    }

    private void ordenarPosiciones() {
        posiciones.sort((a, b) -> {
            int cmp = Integer.compare(b.getPuntuacion(), a.getPuntuacion());
            if (cmp == 0) cmp = Integer.compare(b.getTantosFavor() - b.getTantosContra(), a.getTantosFavor() - a.getTantosContra());
            if (cmp == 0) cmp = a.getEquipo().getNombre().compareToIgnoreCase(b.getEquipo().getNombre());
            return cmp;
        });
        for (int i = 0; i < posiciones.size(); i++) posiciones.get(i).setPosicion(i + 1);
    }

    public ReglaPuntuacion getRegla() { return regla; }
    public void setRegla(ReglaPuntuacion regla) { if (regla == null) throw new IllegalArgumentException("La regla es obligatoria."); this.regla = regla; }
    public List<PosicionEquipo> getPosiciones() { return posiciones; }
    public void setPosiciones(List<PosicionEquipo> posiciones) { this.posiciones = posiciones == null ? new ArrayList<>() : posiciones; }
    public List<PartidoCompetencia> getEncuentros() { return encuentros; }
}
