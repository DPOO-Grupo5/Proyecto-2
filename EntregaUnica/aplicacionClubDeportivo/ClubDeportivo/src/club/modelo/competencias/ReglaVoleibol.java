package club.modelo.competencias;

public class ReglaVoleibol extends ReglaPuntuacion implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public ReglaVoleibol(int victoria, int derrota) {
        super(victoria, derrota);
    }
}
