package club.modelo.instalaciones;

public class CanchaConjunto extends Instalacion implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public CanchaConjunto(int jugadoresMin, int jugadoresMax, String nombre, boolean techada) {
        super(jugadoresMin, jugadoresMax, nombre, techada);
    }

    @Override
    public boolean admiteJugadores(int n, String modalidad) {
        return n >= getJugadoresMin() && n <= getJugadoresMax();
    }
}