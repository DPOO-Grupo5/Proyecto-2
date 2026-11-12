package club.modelo.tienda;

public class Existencia implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private int cantidad;
    private String ubicacionInventario;

    public Existencia(int cantidad, String ubicacionInventario) {
        setCantidad(cantidad);
        setUbicacionInventario(ubicacionInventario);
    }

    public boolean hayDisponible(int n) { return n >= 0 && cantidad >= n; }

    public void aumentar(int n) {
        if (n <= 0) throw new IllegalArgumentException("La cantidad a ingresar debe ser positiva.");
        cantidad += n;
    }

    public void disminuir(int n) {
        if (n <= 0 || !hayDisponible(n)) throw new IllegalArgumentException("La cantidad solicitada supera la existencia disponible.");
        cantidad -= n;
    }

    public int getCantidad() { return cantidad; }

    public void setCantidad(int cantidad) {
        if (cantidad < 0) throw new IllegalArgumentException("La existencia no puede ser negativa.");
        this.cantidad = cantidad;
    }

    public String getUbicacionInventario() { return ubicacionInventario; }

    public void setUbicacionInventario(String ubicacionInventario) {
        if (ubicacionInventario == null || ubicacionInventario.trim().isEmpty()) throw new IllegalArgumentException("La ubicación es obligatoria.");
        this.ubicacionInventario = ubicacionInventario.trim();
    }
}
