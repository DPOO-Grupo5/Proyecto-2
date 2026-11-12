package club.modelo.competencias;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Grupo implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private String nombre;
    private int numeroEquipos;
    private List<EquipoCompetencia> equipos = new ArrayList<>();
    private List<PosicionEquipo> posiciones = new ArrayList<>();
    private List<PartidoCompetencia> partidos = new ArrayList<>();
    private ReglaPuntuacion regla;

    public Grupo(String nombre, int numeroEquipos) {
        if (nombre == null || nombre.trim().isEmpty() || numeroEquipos < 2) throw new IllegalArgumentException("Nombre y cupo válidos son obligatorios.");
        this.nombre = nombre.trim();
        this.numeroEquipos = numeroEquipos;
    }

    public void agregarEquipo(EquipoCompetencia equipo) {
        if (equipo == null) throw new IllegalArgumentException("El equipo es obligatorio.");
        if (equipos.contains(equipo)) return;
        if (equipos.size() >= numeroEquipos) throw new IllegalStateException("El grupo alcanzó su cupo de equipos.");
        equipos.add(equipo);
        posiciones.add(new PosicionEquipo(equipo));
    }

    public void generarPartidos(ReglaPuntuacion regla) {
        if (regla == null) throw new IllegalArgumentException("La regla de puntuación es obligatoria.");
        this.regla = regla;
        partidos.clear();
        posiciones.clear();
        for (EquipoCompetencia equipo : equipos) posiciones.add(new PosicionEquipo(equipo));
        for (int i = 0; i < equipos.size(); i++) {
            for (int j = i + 1; j < equipos.size(); j++) {
                PartidoCompetencia partido = new PartidoCompetencia(null, "Pendiente", "Por definir");
                partido.agregarEquipo(equipos.get(i));
                partido.agregarEquipo(equipos.get(j));
                partido.setGrupo(this);
                partidos.add(partido);
            }
        }
    }

    public void actualizarPosiciones() {
        List<PosicionEquipo> tabla = new ArrayList<>();
        for (EquipoCompetencia equipo : equipos) tabla.add(new PosicionEquipo(equipo));
        for (PartidoCompetencia partido : partidos) {
            if (!partido.isFinalizado()) continue;
            PosicionEquipo local = buscarPosicion(tabla, partido.getEquipos().get(0));
            PosicionEquipo visitante = buscarPosicion(tabla, partido.getEquipos().get(1));
            local.registrarResultado(partido.getMarcadorLocal(), partido.getMarcadorVisitante(), regla.getVictoria(), regla.getEmpate(), regla.getDerrota());
            visitante.registrarResultado(partido.getMarcadorVisitante(), partido.getMarcadorLocal(), regla.getVictoria(), regla.getEmpate(), regla.getDerrota());
        }
        tabla.sort((a, b) -> {
            int comparacion = Integer.compare(b.getPuntuacion(), a.getPuntuacion());
            if (comparacion == 0) comparacion = enfrentamientoDirecto(a.getEquipo(), b.getEquipo());
            if (comparacion == 0) comparacion = Integer.compare(b.getTantosFavor() - b.getTantosContra(), a.getTantosFavor() - a.getTantosContra());
            if (comparacion == 0) comparacion = a.getEquipo().getNombre().compareToIgnoreCase(b.getEquipo().getNombre());
            return comparacion;
        });
        for (int i = 0; i < tabla.size(); i++) tabla.get(i).setPosicion(i + 1);
        posiciones = tabla;
    }
    
    private int enfrentamientoDirecto(EquipoCompetencia x, EquipoCompetencia y) {
        for (PartidoCompetencia p : partidos) {
            if (!p.isFinalizado() || !p.getEquipos().contains(x) || !p.getEquipos().contains(y)) continue;
            EquipoCompetencia ganador = p.getGanador();
            if (ganador != null) return ganador == x ? -1 : 1;
        }
        return 0;
    }

    private PosicionEquipo buscarPosicion(List<PosicionEquipo> tabla, EquipoCompetencia equipo) {
        for (PosicionEquipo posicion : tabla) if (posicion.getEquipo() == equipo) return posicion;
        throw new IllegalStateException("El partido tiene un equipo que no pertenece a este grupo.");
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { if (nombre == null || nombre.trim().isEmpty()) throw new IllegalArgumentException("El nombre es obligatorio."); this.nombre = nombre.trim(); }
    public int getNumeroEquipos() { return numeroEquipos; }
    public void setNumeroEquipos(int numeroEquipos) { if (numeroEquipos < 2 || numeroEquipos < equipos.size()) throw new IllegalArgumentException("El cupo debe ser al menos dos y no menor a los equipos inscritos."); this.numeroEquipos = numeroEquipos; }
    public List<EquipoCompetencia> getEquipos() { return equipos; }
    public void setEquipos(List<EquipoCompetencia> equipos) { if (equipos == null || equipos.size() > numeroEquipos) throw new IllegalArgumentException("La lista supera el cupo del grupo."); this.equipos = new ArrayList<>(equipos); }
    public List<PosicionEquipo> getPosiciones() { return posiciones; }
    public void setPosiciones(List<PosicionEquipo> posiciones) { this.posiciones = posiciones == null ? new ArrayList<>() : posiciones; }
    public List<PartidoCompetencia> getPartidos() { return partidos; }
}
