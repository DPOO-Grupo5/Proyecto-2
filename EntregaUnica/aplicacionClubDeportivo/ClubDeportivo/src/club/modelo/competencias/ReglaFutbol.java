package club.modelo.competencias;

public class ReglaFutbol extends ReglaPuntuacion implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private int empate = 1;

    public ReglaFutbol(int victoria, int derrota) {
        super(victoria, derrota);
    }

    public int getEmpate() { return empate; }

    public void setEmpate(int empate) { this.empate = empate; }
}
