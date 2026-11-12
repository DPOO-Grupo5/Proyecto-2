package club.modelo.competencias;

public class Baloncesto extends DeporteConjunto implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public Baloncesto(String nombre, int minJugadores) {
        super(nombre, minJugadores);
    }
    
    @Override
    public void validarMarcador(int local, int visitante) {
        if (local == visitante) throw new IllegalArgumentException("En baloncesto no hay empates.");
    }
}
