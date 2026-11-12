package club.modelo.competencias;

import java.util.ArrayList;
import java.util.List;

public class FaseGrupos implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private ReglaPuntuacion regla;
    private List<Grupo> grupos = new ArrayList<>();

    public FaseGrupos(ReglaPuntuacion regla) {
        if (regla == null) throw new IllegalArgumentException("La regla de puntuación es obligatoria.");
        this.regla = regla;
    }

    public void generarEncuentrosGrupos(List<EquipoCompetencia> equipos) {
        if (equipos == null || grupos.isEmpty()) throw new IllegalStateException("Se requieren equipos y al menos un grupo.");
        int capacidadRestante = 0;
        for (Grupo grupo : grupos) capacidadRestante += grupo.getNumeroEquipos() - grupo.getEquipos().size();
        int nuevos = 0;
        for (EquipoCompetencia equipo : equipos) if (!yaAsignado(equipo)) nuevos++;
        if (nuevos > capacidadRestante) throw new IllegalStateException("La capacidad total de grupos no alcanza para todos los equipos.");

        int indexGrupo = 0;
        for (EquipoCompetencia equipo : equipos) {
            if (yaAsignado(equipo)) continue;
            int intentos = 0;
            while (intentos < grupos.size() && grupos.get(indexGrupo).getEquipos().size() >= grupos.get(indexGrupo).getNumeroEquipos()) {
                indexGrupo = (indexGrupo + 1) % grupos.size();
                intentos++;
            }
            grupos.get(indexGrupo).agregarEquipo(equipo);
            indexGrupo = (indexGrupo + 1) % grupos.size();
        }
        for (Grupo grupo : grupos) grupo.generarPartidos(regla);
    }

    private boolean yaAsignado(EquipoCompetencia equipo) {
        for (Grupo grupo : grupos) if (grupo.getEquipos().contains(equipo)) return true;
        return false;
    }

    public List<EquipoCompetencia> obtenerClasificados(int porGrupo) {
        if (porGrupo <= 0) throw new IllegalArgumentException("El número de clasificados debe ser positivo.");
        List<EquipoCompetencia> clasificados = new ArrayList<>();
        for (Grupo grupo : grupos) {
            grupo.actualizarPosiciones();
            for (int i = 0; i < Math.min(porGrupo, grupo.getPosiciones().size()); i++) clasificados.add(grupo.getPosiciones().get(i).getEquipo());
        }
        return clasificados;
    }

    public ReglaPuntuacion getRegla() { return regla; }
    public void setRegla(ReglaPuntuacion regla) { if (regla == null) throw new IllegalArgumentException("La regla es obligatoria."); this.regla = regla; }
    public List<Grupo> getGrupos() { return grupos; }
    public void setGrupos(List<Grupo> grupos) { this.grupos = grupos == null ? new ArrayList<>() : grupos; }
}
