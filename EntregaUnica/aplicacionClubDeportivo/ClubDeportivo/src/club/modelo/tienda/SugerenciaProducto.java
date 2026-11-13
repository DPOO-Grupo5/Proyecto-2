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
    
    private String autor, descripcion, motivoRechazo;
    
    public String getAutor() { return autor; }
    public String getDescripcion() { return descripcion; }
    public String getMotivoRechazo() { return motivoRechazo; }
    public void setAutor(String a) { this.autor = a; }
    public void setDescripcion(String d) { this.descripcion = d; }
    public void rechazar(String motivo) {
        if (motivo == null || motivo.trim().isEmpty()) throw new IllegalArgumentException("El motivo del rechazo es obligatorio.");
        this.estado = "RECHAZADO";
        this.motivoRechazo = motivo.trim();
    }
    
    
}