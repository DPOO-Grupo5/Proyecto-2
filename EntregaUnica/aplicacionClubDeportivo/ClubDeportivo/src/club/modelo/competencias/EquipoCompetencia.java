package club.modelo.competencias;

public abstract class EquipoCompetencia implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    protected String nombre;

    public EquipoCompetencia(String nombre) {
        setNombre(nombre);
    }

    public String getNombre() { return nombre; }

    public void setNombre(String nombre) { if (nombre == null || nombre.trim().isEmpty()) throw new IllegalArgumentException("El nombre del equipo es obligatorio."); this.nombre = nombre.trim(); }
}
