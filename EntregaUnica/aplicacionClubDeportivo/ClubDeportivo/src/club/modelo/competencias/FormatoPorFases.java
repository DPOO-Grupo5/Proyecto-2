package club.modelo.competencias;

import java.util.List;

public class FormatoPorFases extends Formato implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private FaseGrupos faseGrupos;
    private FaseEliminacion faseEliminacion;

    public FormatoPorFases(String nombreFormato) {
        super(nombreFormato);
    }

    @Override
    public void generarEncuentros(List<EquipoCompetencia> equipos) {
        if (faseGrupos == null && faseEliminacion == null) throw new IllegalStateException("El formato debe definir al menos una fase.");
        if (faseGrupos != null && equipos != null) {
            faseGrupos.generarEncuentrosGrupos(equipos);
        }
        if (faseEliminacion != null) faseEliminacion.generarLlavesEliminacion();
    }

    public List<PartidoCompetencia> getEncuentros() {
        List<PartidoCompetencia> encuentros = new java.util.ArrayList<>();
        if (faseGrupos != null) for (Grupo grupo : faseGrupos.getGrupos()) encuentros.addAll(grupo.getPartidos());
        if (faseEliminacion != null) for (RondaEliminacion ronda : faseEliminacion.getRondas()) encuentros.addAll(ronda.getPartidos());
        return encuentros;
    }

    public List<PartidoCompetencia> generarEliminacionDesdeGrupos(int clasificadosPorGrupo) {
        if (faseGrupos == null || faseEliminacion == null) throw new IllegalStateException("El formato debe incluir fase de grupos y fase de eliminación.");
        faseEliminacion.generarLlavesEliminacion(faseGrupos.obtenerClasificados(clasificadosPorGrupo));
        List<PartidoCompetencia> partidos = new java.util.ArrayList<>();
        for (RondaEliminacion ronda : faseEliminacion.getRondas()) partidos.addAll(ronda.getPartidos());
        return partidos;
    }

    public FaseGrupos getFaseGrupos() { return faseGrupos; }

    public void setFaseGrupos(FaseGrupos faseGrupos) { this.faseGrupos = faseGrupos; }

    public FaseEliminacion getFaseEliminacion() { return faseEliminacion; }

    public void setFaseEliminacion(FaseEliminacion faseEliminacion) { this.faseEliminacion = faseEliminacion; }
}
