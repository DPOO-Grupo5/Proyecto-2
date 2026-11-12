package club.modelo.instalaciones;

public class CanchaPadel extends Instalacion implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public CanchaPadel(int jugadoresMin, int jugadoresMax, String nombre, boolean techada) {
        super(jugadoresMin, jugadoresMax, nombre, techada);
    }



    @Override
    public boolean admiteJugadores(int n, String modalidad) {
        // En pádel, la regla típica exige la modalidad de dobles y 4 jugadores
        return "dobles".equalsIgnoreCase(modalidad) && n == 4;
    }
}