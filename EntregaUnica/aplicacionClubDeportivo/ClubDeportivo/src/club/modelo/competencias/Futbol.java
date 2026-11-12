package club.modelo.competencias;

public class Futbol extends DeporteConjunto implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public Futbol(String nombre, int minJugadores) {
        super(nombre, minJugadores);
    }
}
