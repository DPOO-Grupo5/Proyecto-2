package club.modelo.entrenamiento;

public abstract class EstadisticaIndividual implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    protected int sets;

    private InscripcionDisciplina inscripcion;

    public EstadisticaIndividual(int sets) {
        if (sets < 0) throw new IllegalArgumentException("Los sets no pueden ser negativos.");
        this.sets = sets;
    }

    public EstadisticaIndividual(int sets, InscripcionDisciplina inscripcion) {
        this(sets);
        this.inscripcion = inscripcion;
    }

    public int puntajeDesempate() { return 0; }

    public int getSets() { return sets; }

    public void setSets(int sets) { if (sets < 0) throw new IllegalArgumentException("Los sets no pueden ser negativos."); this.sets = sets; }

    public InscripcionDisciplina getInscripcion() { return inscripcion; }

    public void setInscripcion(InscripcionDisciplina inscripcion) { this.inscripcion = inscripcion; }
}
