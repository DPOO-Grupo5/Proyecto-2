package club.modelo.entrenamiento;

public class EstadisticaTenisMesa extends EstadisticaIndividual implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private int puntosTotales;

    public EstadisticaTenisMesa(int sets, int puntosTotales) {
        super(sets);
        if (puntosTotales < 0) throw new IllegalArgumentException("Los puntos no pueden ser negativos.");
        this.puntosTotales = puntosTotales;
    }

    @Override
    public int puntajeDesempate() { return puntosTotales; }

    public int getPuntosTotales() { return puntosTotales; }

    public void setPuntosTotales(int puntosTotales) { if (puntosTotales < 0) throw new IllegalArgumentException("Los puntos no pueden ser negativos."); this.puntosTotales = puntosTotales; }
}
