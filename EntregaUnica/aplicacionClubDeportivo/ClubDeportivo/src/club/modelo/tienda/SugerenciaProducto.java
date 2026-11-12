package club.modelo.tienda;

public class SugerenciaProducto implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private String estado;
    private Producto producto;

    public SugerenciaProducto(String estado, Producto producto) {
        this.estado = estado;
        this.producto = producto;
    }

    public SugerenciaProducto(String estado) {
        this.estado = estado;
    }

    public void aprobar() {
        this.estado = "APROBADO";
    }

    public void rechazar() {
        this.estado = "RECHAZADO";
    }

    public String getEstado() { return estado; }

    public void setEstado(String estado) { this.estado = estado; }

    public Producto getProducto() { return producto; }

    public void setProducto(Producto producto) { this.producto = producto; }
}