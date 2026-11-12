package club.modelo.competencias;

public class ReglaBaloncesto extends ReglaPuntuacion implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public ReglaBaloncesto(int victoria, int derrota) {
        super(victoria, derrota);
    }
}
