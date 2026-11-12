package club.modelo.entrenamiento;

public class EstadisticaTenisPadel extends EstadisticaIndividual implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private int gamesGanados;
    private int aces;
    private int erroresNoForzados;

    public EstadisticaTenisPadel(int sets, int gamesGanados, int aces, int erroresNoForzados) {
        super(sets);
        if (gamesGanados < 0 || aces < 0 || erroresNoForzados < 0) throw new IllegalArgumentException("Las estadísticas no pueden ser negativas.");
        this.gamesGanados = gamesGanados;
        this.aces = aces;
        this.erroresNoForzados = erroresNoForzados;
    }

    public int getGamesGanados() { return gamesGanados; }

    public void setGamesGanados(int gamesGanados) { if (gamesGanados < 0) throw new IllegalArgumentException("Los games no pueden ser negativos."); this.gamesGanados = gamesGanados; }

    public int getAces() { return aces; }

    public void setAces(int aces) { if (aces < 0) throw new IllegalArgumentException("Los aces no pueden ser negativos."); this.aces = aces; }

    public int getErroresNoForzados() { return erroresNoForzados; }

    @Override
    public int puntajeDesempate() { return gamesGanados; }

    public void setErroresNoForzados(int erroresNoForzados) { if (erroresNoForzados < 0) throw new IllegalArgumentException("Los errores no pueden ser negativos."); this.erroresNoForzados = erroresNoForzados; }
}
