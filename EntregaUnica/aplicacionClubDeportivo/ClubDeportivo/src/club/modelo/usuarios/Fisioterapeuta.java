package club.modelo.usuarios;

public class Fisioterapeuta extends Empleado implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public Fisioterapeuta(String login, String password, String nombre, String codigoDescuento) {
        super(login, password, nombre, codigoDescuento);
    }
}
