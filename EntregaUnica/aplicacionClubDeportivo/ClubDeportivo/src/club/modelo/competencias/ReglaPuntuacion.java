package club.modelo.competencias;

public abstract class ReglaPuntuacion implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    protected int victoria;
    protected int derrota;

    public ReglaPuntuacion(int victoria, int derrota) {
        this.victoria = victoria;
        this.derrota = derrota;
    }

    public int getVictoria() { return victoria; }

    public void setVictoria(int victoria) { this.victoria = victoria; }

    public int getDerrota() { return derrota; }

    public int getEmpate() { return 0; }

    public void setDerrota(int derrota) { this.derrota = derrota; }
}
