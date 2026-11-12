package club.modelo.instalaciones;

public class MesaTenisMesa extends Instalacion implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public MesaTenisMesa(int jugadoresMin, int jugadoresMax, String nombre, boolean techada) {
        super(jugadoresMin, jugadoresMax, nombre, techada);
    }

    @Override
    public boolean admiteJugadores(int n, String modalidad) {
        // En tenis de mesa se permite: sencillos (2 jugadores) o dobles (4 jugadores)
        if ("sencillos".equalsIgnoreCase(modalidad)) {
            return n == 2;
        } else if ("dobles".equalsIgnoreCase(modalidad)) {
            return n == 4;
        }
        return false;
    }
}