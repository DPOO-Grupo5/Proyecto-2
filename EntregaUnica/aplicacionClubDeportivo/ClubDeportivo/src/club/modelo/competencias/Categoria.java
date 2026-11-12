package club.modelo.competencias;

public class Categoria implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private String nombre;
    private int edadMinima;
    private int edadMaxima;

    public Categoria(String nombre, int edadMinima, int edadMaxima) {
        setNombre(nombre);
        setEdadMinima(edadMinima);
        setEdadMaxima(edadMaxima);
        validarRango();
    }

    private void validarRango() { if (edadMinima > edadMaxima) throw new IllegalArgumentException("La edad mínima no puede superar la máxima."); }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { if (nombre == null || nombre.trim().isEmpty()) throw new IllegalArgumentException("El nombre de categoría es obligatorio."); this.nombre = nombre.trim(); }
    public int getEdadMinima() { return edadMinima; }
    public void setEdadMinima(int edadMinima) { if (edadMinima < 0 || edadMinima > edadMaxima && edadMaxima > 0) throw new IllegalArgumentException("La edad mínima no es válida."); this.edadMinima = edadMinima; }
    public int getEdadMaxima() { return edadMaxima; }
    public void setEdadMaxima(int edadMaxima) { if (edadMaxima < 0 || edadMaxima < edadMinima) throw new IllegalArgumentException("La edad máxima no es válida."); this.edadMaxima = edadMaxima; }
}
